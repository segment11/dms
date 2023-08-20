package plugin.demo2

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import model.AppDTO
import model.json.DirVolumeMount
import model.json.KVPair
import model.json.PortMapping
import plugin.BasePlugin
import plugin.callback.Observer
import server.AgentCaller
import server.InMemoryAllContainerManager
import server.dns.DnsOperator
import server.scheduler.checker.HealthChecker
import server.scheduler.checker.HealthCheckerHolder
import server.scheduler.processor.ContainerRunResult
import server.scheduler.processor.JobStepKeeper
import transfer.ContainerInfo

@CompileStatic
@Slf4j
class ConsulPlugin extends BasePlugin implements Observer {
    @Override
    String name() {
        'consul'
    }

    @Override
    void init() {
        super.init()

        initImageConfig()
        initChecker()

        // cmd
        // ["sh","-c","consul agent $ServerModeCmdExt"]
    }

    private void initImageConfig() {
        addEnvIfNotExists('DATA_CENTER', 'DATA_CENTER')
        addEnvIfNotExists('DOMAIN', 'DOMAIN', 'default: consul')

        '8500,8600'.split(',').each {
            addPortIfNotExists(it.toString(), it as int)
        }

        addNodeVolumeForUpdate('data-dir', '/consul/data')
        addNodeVolumeForUpdate('config-dir', '/consul/config')
    }

    private void initChecker() {
        HealthCheckerHolder.instance.add new HealthChecker() {

            @Override
            String name() {
                'consul member check'
            }

            @Override
            String imageName() {
                ConsulPlugin.this.imageName()
            }

            @Override
            boolean check(AppDTO app) {
                if (app.conf.cmd.contains('-server=true')) {
                    // sh -c "consul members"
                    final String cmd = 'consul members'

                    def containerList = InMemoryAllContainerManager.instance.getContainerList(app.clusterId, app.id)
                    if (!containerList) {
                        log.warn 'failed get container list, clusterId: {}, appId: {}', app.clusterId, app.id
                        return false
                    }

                    // only check first container
                    def x = containerList[0]
                    def r = AgentCaller.instance.agentScriptExe(app.clusterId, x.nodeIp,
                            'container init', [id: x.id, initCmd: cmd])

                    def message = r.getString('message')
                    if (!message) {
                        log.warn 'failed get container cmd result message, result: {}', r.toString()
                        return false
                    }

                    def okLines = message.readLines().findAll { it.contains('server') && it.contains('alive') }
                    if (okLines.size() != app.conf.containerNumber) {
                        return false
                    }
                }

                true
            }
        }
    }

    @Override
    String image() {
        'consul'
    }

    @Override
    Map<String, String> expressions() {
        HashMap<String, String> r = [:]
        r['ServerModeCmdExt'] = """
def instanceIndex = super.binding.getProperty('instanceIndex') as int
def nodeIp = super.binding.getProperty('nodeIp')
def nodeIpList = super.binding.getProperty('nodeIpList') as List<String>

List<String> cmdArgs = []
cmdArgs << "-server=true"
cmdArgs << "-advertise=\${nodeIp}".toString()
cmdArgs << "-bind=0.0.0.0"
cmdArgs << "-client=0.0.0.0"
cmdArgs << "-ui"
cmdArgs << "-datacenter=\${envs.DATA_CENTER}"
cmdArgs << "-domain=\${envs.DOMAIN}"

cmdArgs << "-data-dir /consul/data -config-dir /consul/config"
cmdArgs << "-bootstrap-expect=\${nodeIpList.size()}".toString()
if (instanceIndex > 0) {
    cmdArgs << "-join \${nodeIpList[0]}".toString()
}

cmdArgs.join(' ')
"""
        r
    }

    @Override
    AppDTO demoApp(AppDTO app) {
        app.name = image()

        def conf = app.conf
        conf.group = group()
        conf.image = image()

        conf.cmd = '[ "sh", "-c", "consul agent $ServerModeCmdExt" ]'

        conf.memMB = 256
        conf.cpuFixed = 0.2

        // because -bootstrap-expect must be equal to nodeIpList.size()
        def nodeIpList = conf.targetNodeIpList
        if (conf.isLimitNode && nodeIpList.size() > 1) {
            conf.targetNodeIpList = [nodeIpList[0]]
        }

        conf.dirVolumeList << new DirVolumeMount(
                dir: '/consul/data', dist: '/consul/data', mode: 'rw',
                nodeVolumeId: getNodeVolumeIdByDir('/consul/data'))
        conf.dirVolumeList << new DirVolumeMount(
                dir: '/consul/config', dist: '/consul/config', mode: 'rw',
                nodeVolumeId: getNodeVolumeIdByDir('/consul/config'))

        conf.portList << new PortMapping(privatePort: 8500, publicPort: 8500)
        conf.portList << new PortMapping(privatePort: 8600, publicPort: 8600)

        conf.envList << new KVPair('DATA_CENTER', 'dc1')
        conf.envList << new KVPair('DOMAIN', 'consul')

        app
    }

    @Override
    void afterContainerRun(AppDTO app, int instanceIndex, ContainerRunResult result) {
        DnsOperator.instance.register(app, result.nodeIp, instanceIndex)
    }

    @Override
    void beforeContainerStop(AppDTO app, ContainerInfo x, JobStepKeeper keeper) {
    }

    @Override
    void afterContainerStopped(AppDTO app, ContainerInfo x, boolean flag) {
        DnsOperator.instance.deregister(app, x)
    }

    @Override
    void refresh(AppDTO app, List<ContainerInfo> runningContainerList) {
        DnsOperator.instance.refreshContainerDns(app, runningContainerList)
    }
}
