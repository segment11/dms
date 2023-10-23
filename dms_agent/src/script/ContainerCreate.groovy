package script

import agent.Agent
import com.alibaba.fastjson.JSON
import com.github.dockerjava.api.DockerClient
import com.github.dockerjava.api.model.*
import com.segment.common.Conf
import com.segment.common.Utils
import model.json.FileVolumeMount
import model.json.KVPair
import model.json.PortMapping
import model.server.CreateContainerConf
import org.apache.commons.io.FileUtils
import org.segment.web.common.CachedGroovyClassLoader
import support.ToJson
import transfer.ContainerConfigInfo
import transfer.ContainerInfo

import java.security.InvalidParameterException

import static common.ContainerHelper.*
import static java.nio.file.attribute.PosixFilePermission.*

DockerClient docker = super.binding.getProperty('docker') as DockerClient
Map params = super.binding.getProperty('params') as Map
String jsonStr = params.jsonStr.toString()

def createConf = ToJson.read(jsonStr, CreateContainerConf)
def conf = createConf.conf

String containerName = generateContainerName(createConf.appId, createConf.instanceIndex)[1..-1]
def alreadyExistsList = docker.listContainersCmd().
        withShowAll(true).
        withNameFilter([containerName]).exec()
if (alreadyExistsList) {
    throw new InvalidParameterException('container name already exists: ' + containerName)
}

def hostConfig = new HostConfig()
if (conf.isPrivileged) {
    hostConfig.withPrivileged(true)
}

hostConfig.withPidMode(conf.pidMode ?: 'host')

final Map<String, Object> evalParams = [
        appId        : createConf.appId as Object,
        instanceIndex: createConf.instanceIndex,
        nodeIp       : createConf.nodeIp]

def envList = conf.envList.findAll { it.key }
for (env in envList) {
    if (env.value && env.value.toString().contains('${')) {
        env.value = CachedGroovyClassLoader.instance.eval('"' + env.value + '"', evalParams).toString()
    }
}

createConf.globalEnvConf.envList.each {
    envList << it
}
envList << new KVPair(key: KEY_APP_ID, value: createConf.appId)
envList << new KVPair(key: KEY_CLUSTER_ID, value: createConf.clusterId)
envList << new KVPair(key: KEY_NODE_IP, value: createConf.nodeIp)
envList << new KVPair(key: KEY_NODE_IP_LIST, value: createConf.nodeIpList.join(','))
envList << new KVPair(key: KEY_INSTANCE_INDEX, value: createConf.instanceIndex)

double vCpuNumber = 0
if (conf.cpuShares) {
    hostConfig.withCpuShares(conf.cpuShares)
    vCpuNumber = (conf.cpuShares / 1024).round(2).doubleValue()
} else if (conf.cpuFixed) {
    long cpuPeriod = 100 * 1000
    long cpuQuota = (conf.cpuFixed * cpuPeriod) as long
    hostConfig.withCpuPeriod(cpuPeriod).withCpuQuota(cpuQuota)
    vCpuNumber = conf.cpuFixed
}
envList << new KVPair(key: 'X_vCpuNumber', value: vCpuNumber)

if (conf.cpusetCpus) {
    hostConfig.withCpusetCpus(conf.cpusetCpus)
    envList << new KVPair(key: 'X_cpusetCpus', value: conf.cpusetCpus)
}

final long MBSize = (1024 * 1024) as long
if (conf.memMB > 0) {
    long mem = conf.memMB * MBSize
    hostConfig.withMemory(mem)
    envList << new KVPair(key: 'X_memory', value: mem)
}
if (conf.memReservationMB > 0) {
    long memReservation = conf.memReservationMB * MBSize
    hostConfig.withMemoryReservation(memReservation)
    envList << new KVPair(key: 'X_memory_reservation', value: memReservation)
}

// network *** ***
def networkMode = conf.networkMode ?: 'host'
hostConfig.withNetworkMode(networkMode)
boolean isNetworkHost = networkMode == 'host'

List<PortBinding> portBindings = []
if (!isNetworkHost) {
    conf.portList.eachWithIndex { PortMapping pm, int i ->
        def publicPort = pm.publicPort == -1 ? common.Utils.getOnePortListenAvailable() : pm.publicPort
        def privatePort = pm.privatePort
        // for application use
        envList << new KVPair(key: 'X_port_' + privatePort, value: '' + publicPort)
        def binding = new PortBinding(Ports.Binding.bindPort(publicPort),
                pm.listenType.name() == 'udp' ? ExposedPort.udp(privatePort) : ExposedPort.tcp(privatePort))
        portBindings << binding
    }
    hostConfig.withPortBindings(portBindings)

    Agent.instance.addJobStep(createConf.jobId, createConf.instanceIndex,
            'create container port bind', [portBindings: portBindings])
}

// dns *** ***
List<String> dnsServerList = []
if (conf.isNetworkDnsUsingCluster && createConf.globalEnvConf.dnsServer) {
    createConf.globalEnvConf.dnsServer.split(',').each {
        dnsServerList << it.toString()
    }
}
def f = new File('/etc/resolv.conf')
if (f.exists() && f.canRead()) {
    f.readLines().findAll { it.contains('nameserver') }.each {
        dnsServerList << it.trim().split(' ')[1].trim()
    }
}

if (dnsServerList) {
    hostConfig.withDns(dnsServerList)
    Agent.instance.addJobStep(createConf.jobId, createConf.instanceIndex,
            'create container dns server bind', [dnsServerBind: dnsServerList])
}

// volume *** ***
List<Bind> binds = []

String tplConfFileDir = Conf.instance.getString('agentTplConfFileDir', '/opt/dms/config')
conf.fileVolumeList.eachWithIndex { FileVolumeMount one, int i ->
    def content = Agent.instance.post('/dms/api/container/create/tpl',
            [clusterId       : createConf.clusterId,
             appId           : createConf.appId,
             appIdList       : createConf.appIdList,
             nodeIp          : createConf.nodeIp,
             nodeIpList      : createConf.nodeIpList,
             targetNodeIpList: createConf.conf.targetNodeIpList,
             instanceIndex   : createConf.instanceIndex,
             containerNumber : conf.containerNumber,
             envList         : envList,
             imageTplId      : one.imageTplId], String)

    if (one.isParentDirMount) {
        String fileLocal = one.dist
        // dyn
        String hostFileFinal
        if (fileLocal.contains('${')) {
            hostFileFinal = CachedGroovyClassLoader.instance.eval('"' + fileLocal + '"', evalParams).toString()
        } else {
            hostFileFinal = fileLocal
        }

        def localFile = new File(hostFileFinal)
        if (!localFile.exists()) {
            FileUtils.forceMkdirParent(localFile)
            localFile.text = content
        }
    } else {
        String hostFilePath = tplConfFileDir + ('/' + createConf.appId + '/' + Utils.uuid() + '.file')
        def localFile = new File(hostFilePath)
        FileUtils.forceMkdirParent(localFile)
        localFile.text = content
        common.Utils.setFileRead(localFile)

        binds << new Bind(hostFilePath, new Volume(one.dist), AccessMode.ro)
    }
    Agent.instance.addJobStep(createConf.jobId, createConf.instanceIndex,
            'create container mount file', [mountFileDist: one.dist, mountFileGenerateContent: content])
}

conf.dirVolumeList.each {
    def mod = 'rw' == it.mode ? AccessMode.rw : AccessMode.ro
    boolean needChangeMode = true

    String dirMount = it.dir
    // dyn
    String dirMountFinal
    if (dirMount.contains('${')) {
        dirMountFinal = CachedGroovyClassLoader.instance.eval('"' + dirMount + '"', evalParams).toString()
    } else {
        dirMountFinal = dirMount
    }

    def dir = new File(dirMountFinal)
    if (!dir.exists()) {
        FileUtils.forceMkdir(dir)
    } else {
        if (dir.isFile()) {
            needChangeMode = false
        }
    }

    if (needChangeMode) {
        common.Utils.setFilePermission(dir, OWNER_READ, OWNER_EXECUTE, OWNER_WRITE,
                GROUP_READ, GROUP_EXECUTE, GROUP_WRITE, OTHERS_READ, OTHERS_EXECUTE, OTHERS_WRITE)
    }

    binds << new Bind("${dirMountFinal}".toString(), new Volume(it.dist), mod)

    Agent.instance.addJobStep(createConf.jobId, createConf.instanceIndex,
            'create container mount directory', [mountDir: it.dist, hostDir: dirMountFinal])
}

hostConfig.withBinds(binds)

// uLimit *** ***
List<Ulimit> uLimitList = conf.uLimitList.collect {
    new Ulimit(it.name, it.soft, it.hard)
}
if (uLimitList) {
    hostConfig.withUlimits(uLimitList)
}

Agent.instance.addJobStep(createConf.jobId, createConf.instanceIndex,
        'create container env setting', [envSetting: envList])

// cmd *** ***
List<String> cmd = []
if (conf.cmd) {
    // json array
    if (conf.cmd.startsWith('[')) {
        JSON.parseArray(conf.cmd).each {
            cmd << it.toString()
        }
    } else {
//        cmd << "sh"
//        cmd << "-c"
        cmd << conf.cmd
    }
}

def createContainerCmd = docker.createContainerCmd(createConf.imageWithTag).
        withUser(conf.user ?: 'root').
        withEnv(envList.collect {
            "${it.key}=${it.value.toString()}".toString()
        }).
        withName(containerName).
        withHostConfig(hostConfig).
        withCmd(cmd)

if (!isNetworkHost) {
    createContainerCmd.withHostName(generateContainerHostname(createConf.appId, createConf.instanceIndex))
}

def response = createContainerCmd.exec()
Agent.instance.sendContainer()

ContainerConfigInfo containerConfigInfo = new ContainerConfigInfo()
containerConfigInfo.containerId = response.id
containerConfigInfo.networkMode = hostConfig.networkMode
containerConfigInfo.ports = []
conf.portList.each { mapping ->
    containerConfigInfo.ports << new ContainerInfo.PortMapping(
            type: mapping.listenType.name(),
            privatePort: mapping.privatePort,
            publicPort: mapping.publicPort)
}

containerConfigInfo
