package server.scheduler.processor

import com.alibaba.fastjson.JSONObject
import com.segment.common.Conf
import common.ContainerHelper
import deploy.DeploySupport
import deploy.InitAgentEnvSupport
import deploy.OneCmd
import ex.JobProcessException
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import model.DeployFileDTO
import model.NodeKeyPairDTO
import model.server.CreateContainerConf
import org.segment.d.json.DefaultJsonTransformer
import org.segment.d.json.JsonTransformer
import server.AgentCaller
import server.InMemoryCacheSupport
import transfer.ContainerInfo

@CompileStatic
@Singleton
@Slf4j
class HostProcessSupport {
    static final int skipDeployFileId = 0

    private static JsonTransformer json = new DefaultJsonTransformer()

    int startOneProcess(CreateContainerConf c, JobStepKeeper keeper) {
        def deployFileIdList = c.conf.deployFileIdList

        if (deployFileIdList.size() == 1 && deployFileIdList[0] == skipDeployFileId) {
            log.info 'skip deploy'
        } else {
            if (!deployFileIdList) {
                throw new JobProcessException('run as a process need copy executable file!')
            }

            def deployFileList = new DeployFileDTO().whereIn('id', deployFileIdList).list()
            if (!deployFileList) {
                throw new JobProcessException('deploy file not exists!')
            }

            def kp = new NodeKeyPairDTO(clusterId: c.clusterId, ip: c.nodeIp).one()
            if (!kp) {
                throw new JobProcessException('deploy node not init!')
            }

            def clusterOne = InMemoryCacheSupport.instance.oneCluster(c.clusterId)
            String proxyNodeIp = clusterOne.globalEnvConf.proxyNodeIp
            def needProxy = proxyNodeIp && proxyNodeIp != kp.ip

            for (deployFile in deployFileList) {
                if (needProxy) {
                    def r = AgentCaller.instance.doSshCopy(kp, deployFile.localPath, deployFile.destPath)
                    log.info r ? r.toString() : '...'
                } else {
                    new InitAgentEnvSupport(kp).copyFileIfNotExists(deployFile.localPath)
                }

                def initCmd = deployFile.initCmd
                if (initCmd) {
                    String changedCmd
                    if (initCmd.contains('$destPath')) {
                        changedCmd = initCmd.replace('$destPath', deployFile.destPath)
                    } else {
                        changedCmd = initCmd
                    }
                    def oneCmd = OneCmd.simple(changedCmd)

                    if (needProxy) {
                        def r = AgentCaller.instance.doSshExec(kp, oneCmd.cmd)
                        log.info r ? r.toString() : '...'
                    } else {
                        DeploySupport.instance.exec(kp, oneCmd)
                        if (!oneCmd.ok()) {
                            throw new JobProcessException('deploy file exec fail - ' + oneCmd.toString())
                        }
                    }
                }
            }
        }

        // dyn template
        def fileVolumeList = c.conf.fileVolumeList
        if (fileVolumeList) {
            JSONObject updateTplR = AgentCaller.instance.agentScriptExe(c.clusterId, c.nodeIp, 'update tpl',
                    [jsonStr: json.json(c)])
            Boolean isErrorUpdateTpl = updateTplR.getBoolean('isError')
            if (isErrorUpdateTpl && isErrorUpdateTpl.booleanValue()) {
                throw new JobProcessException('update tpl fail - ' + fileVolumeList + ' - ' + updateTplR.getString('message'))
            }
            keeper.next(JobStepKeeper.Step.updateTpl, 'update tpl', fileVolumeList.toString())
        }

        // ***
        String fixPwd = c.conf.envList.find { it.key == 'PWD' }?.value
        int pid = startCmdWithSsh(fixPwd, c.conf.cmd, c.clusterId, c.appId, c.nodeIp, keeper)
        if (c.conf.cpusetCpus) {
            // may set already
            setProcessCpuset(pid, c.conf.cpusetCpus, c.clusterId, c.nodeIp, keeper)
        }

        // ***
        ContainerInfo containerInfo = new ContainerInfo()
        containerInfo.nodeIp = c.nodeIp
        containerInfo.clusterId = c.clusterId
        containerInfo.namespaceId = c.app.namespaceId
        containerInfo.appName = c.app.name
        containerInfo.appDes = c.app.des
        containerInfo.id = ContainerHelper.generateProcessAsContainerId(c.appId, c.instanceIndex, pid)
        containerInfo.names = [ContainerHelper.generateContainerName(c.appId, c.instanceIndex)]
        containerInfo.image = 'refer deploy file:' + c.conf.deployFileIdList
        containerInfo.imageId = 'refer deploy file:' + c.conf.deployFileIdList
        containerInfo.command = c.conf.cmd
        containerInfo.created = System.currentTimeMillis()

        AgentCaller.instance.agentScriptExe(c.clusterId, c.nodeIp, 'set wrap container info',
                [containerInfo: containerInfo])
        keeper.next(JobStepKeeper.Step.wrapContainerInfo, 'set wrap container info')

        pid
    }

    int startCmdWithSsh(String fixPwd, String cmd, int clusterId, int appId, String nodeIp, JobStepKeeper keeper = null) {
        // ***
        def kp = new NodeKeyPairDTO(clusterId: clusterId, ip: nodeIp).one()
        if (!kp) {
            throw new JobProcessException('node ssh init not yet, ip: ' + nodeIp)
        }
        if (!kp.rootPass) {
            throw new JobProcessException('node ssh root pass not set yet, ip: ' + nodeIp)
        }

        // guardian do job too fast
        // agent not send yet
        JSONObject getPidOldR = AgentCaller.instance.agentScriptExe(clusterId, nodeIp, 'get pid',
                [cmd: cmd])
        def pidAlreadyRun = getPidOldR.getInteger('pid')
        if (pidAlreadyRun != null) {
            keeper.next(JobStepKeeper.Step.startCmd, 'skip start cmd', cmd)
            return pidAlreadyRun
        }

        def clusterOne = InMemoryCacheSupport.instance.oneCluster(clusterId)
        String proxyNodeIp = clusterOne.globalEnvConf.proxyNodeIp
        def needProxy = proxyNodeIp && proxyNodeIp != kp.ip

        def passwordTips = Conf.instance.getString('sudo.shell.password.input.tips', 'Password:')

        String pwd = fixPwd ?: '/opt/dms/app_' + appId
        String startCommand = "nohup ${cmd} > main.log 2>&1 &"
        log.info 'start command: {}, node id: {}', startCommand, nodeIp

        List<OneCmd> cmdList = [
                new OneCmd(cmd: 'pwd', checker: OneCmd.keyword(kp.user + '@')),
                new OneCmd(cmd: 'su', checker: OneCmd.keyword(passwordTips)),
                new OneCmd(cmd: kp.rootPass, showCmdLog: false,
                        checker: OneCmd.keyword('root@').failKeyword('failure')),
                new OneCmd(cmd: 'mkdir -p ' + pwd, checker: OneCmd.any()),
                new OneCmd(cmd: 'cd ' + pwd, checker: OneCmd.keyword(pwd)),
                new OneCmd(cmd: startCommand, checker: OneCmd.any())
        ]

        if (needProxy) {
            AgentCaller.instance.doSshShell(kp, cmdList, 30000)
        } else {
            DeploySupport.instance.exec(kp, cmdList, 30, true)
            if (!cmdList.every { it.ok() }) {
                throw new JobProcessException('ssh cmd exec error, ip: ' + nodeIp + ' last cmd: ' +
                        cmdList.find { !it.ok() }.toString())
            }
        }

        // wait process start
        int maxWaitSeconds = Conf.instance.getInt('process.getPid.maxWaitSeconds', 5)
        int count = 0
        String errorMessage
        while (count <= maxWaitSeconds) {
            count++

            Thread.sleep(1000)

            JSONObject getPidR = AgentCaller.instance.agentScriptExe(clusterId, nodeIp, 'get pid',
                    [cmd: cmd])
            Boolean isErrorGetPid = getPidR.getBoolean('isError')
            if (isErrorGetPid && isErrorGetPid.booleanValue()) {
                errorMessage = getPidR.getString('message')
                continue
            }
            if (keeper) {
                keeper.next(JobStepKeeper.Step.startCmd, 'start cmd', cmd)
            }
            int pid = getPidR.getInteger('pid')
            return pid
        }

        throw new JobProcessException('start cmd get pid fail - ' + cmd + ' - ' + errorMessage)
    }

    void setProcessCpuset(int pid, String cpusetCpus, int clusterId, String nodeIp, JobStepKeeper keeper = null) {
        def kp = new NodeKeyPairDTO(clusterId: clusterId, ip: nodeIp).one()

        def clusterOne = InMemoryCacheSupport.instance.oneCluster(clusterId)
        String proxyNodeIp = clusterOne.globalEnvConf.proxyNodeIp
        def needProxy = proxyNodeIp && proxyNodeIp != kp.ip

        def passwordTips = Conf.instance.getString('sudo.shell.password.input.tips', 'Password:')

        String setCommand = "taskset -cp ${cpusetCpus} ${pid}"
        log.info 'set command: {}, node id: {}', setCommand, nodeIp

        List<OneCmd> cmdList = [
                new OneCmd(cmd: 'pwd', checker: OneCmd.keyword(kp.user + '@')),
                new OneCmd(cmd: 'su', checker: OneCmd.keyword(passwordTips)),
                new OneCmd(cmd: kp.rootPass, showCmdLog: false,
                        checker: OneCmd.keyword('root@').failKeyword('failure')),
                new OneCmd(cmd: setCommand, checker: OneCmd.any())
        ]

        if (needProxy) {
            AgentCaller.instance.doSshShell(kp, cmdList, 10000)
        } else {
            DeploySupport.instance.exec(kp, cmdList, 10, true)
            if (!cmdList.every { it.ok() }) {
                throw new JobProcessException('ssh cmd exec error, ip: ' + nodeIp + ' last cmd: ' +
                        cmdList.find { !it.ok() }.toString())
            }
        }

        if (keeper) {
            keeper.next(JobStepKeeper.Step.afterCmd, 'after cmd', setCommand)
        }
    }

}
