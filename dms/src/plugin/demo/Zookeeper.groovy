package plugin.demo

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import model.json.KVPair
import model.server.CreateContainerConf
import plugin.BasePlugin
import server.scheduler.checker.Checker
import server.scheduler.checker.CheckerHolder
import server.scheduler.processor.JobStepKeeper

@CompileStatic
@Slf4j
class Zookeeper extends BasePlugin {
    @Override
    String name() {
        'zookeeper'
    }

    @Override
    void init() {
        super.init()

        initImageConfig()
        initChecker()
    }

    private void initImageConfig() {
        '''
ZOO_TICK_TIME
ZOO_INIT_LIMIT
ZOO_SYNC_LIMIT
ZOO_MAX_CLIENT_CNXNS
ZOO_STANDALONE_ENABLED
ZOO_ADMINSERVER_ENABLED
ZOO_AUTOPURGE_PURGEINTERVAL
ZOO_AUTOPURGE_SNAPRETAINCOUNT
ZOO_4LW_COMMANDS_WHITELIST
ZOO_CFG_EXTRA
JVMFLAGS
ZOO_MY_ID
ZOO_SERVERS
'''.readLines().collect { it.trim() }.findAll { it }.each {
            addEnvIfNotExists(it, it)
        }
    }

    private void initChecker() {
        CheckerHolder.instance.add new Checker() {
            @Override
            boolean check(CreateContainerConf conf, JobStepKeeper keeper) {
                conf.conf.envList << new KVPair<String>('ZOO_MY_ID', conf.instanceIndex.toString())

                List<String> list = []
                conf.nodeIpList.eachWithIndex { String nodeIp, int i ->
                    list << "server.${i}=${nodeIp}:2888:3888;2181".toString()
                }

                conf.conf.envList << new KVPair<String>('ZOO_SERVERS', list.join(' '))
            }

            @Override
            Checker.Type type() {
                Checker.Type.before
            }

            @Override
            String name() {
                'set env'
            }

            @Override
            String imageName() {
                'library/zookeeper'
            }

            @Override
            String script(CreateContainerConf conf) {
                null
            }
        }
    }

    @Override
    String image() {
        'zookeeper'
    }

    @Override
    boolean isRunning() {
        true
    }
}
