package plugin.demo2

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import model.AppDTO
import plugin.BasePlugin
import server.AgentCaller
import server.InMemoryAllContainerManager
import server.scheduler.checker.HealthChecker
import server.scheduler.checker.HealthCheckerHolder

@CompileStatic
@Slf4j
class ConsulPlugin extends BasePlugin {
    @Override
    String name() {
        'consul'
    }

    @Override
    void init() {
        super.init()

        initImageConfig()
        initChecker()
    }

    private void initImageConfig() {
        addEnvIfNotExists('CONSUL_BIND_INTERFACE', 'CONSUL_BIND_INTERFACE')
        addEnvIfNotExists('CONSUL_ALLOW_PRIVILEGED_PORTS=', 'CONSUL_ALLOW_PRIVILEGED_PORTS=')

        '8300,8301,8302,8500,8600'.split(',').each {
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
cmdArgs << "-bind=\${nodeIp}".toString()
cmdArgs << "-bootstrap-expect=\${nodeIpList.size()}".toString()

if (instanceIndex > 0) {
    cmdArgs << "-join \${nodeIpList[0]}".toString()
}

cmdArgs.join(' ')
"""
        r['ClientModeCmdExt'] = """
nodeIpList.collect { '--join ' + it }.join(' ')
"""
        r
    }
}
