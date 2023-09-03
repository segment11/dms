package plugin.demo2

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import model.AppDTO
import model.ImageTplDTO
import model.json.*
import model.server.CreateContainerConf
import plugin.BasePlugin
import plugin.PluginManager
import server.AgentCaller
import server.InMemoryAllContainerManager
import server.scheduler.checker.Checker
import server.scheduler.checker.CheckerHolder
import server.scheduler.checker.HealthChecker
import server.scheduler.checker.HealthCheckerHolder
import server.scheduler.processor.JobStepKeeper

@CompileStatic
@Slf4j
class EtcdPlugin extends BasePlugin {
    @Override
    String name() {
        'etcd'
    }

    @Override
    void init() {
        super.init()

        initImageConfig()
        initChecker()
    }

    private void initImageConfig() {
        '2379,2380'.split(',').each {
            addPortIfNotExists(it.toString(), it as int)
        }

        final String tplName = 'etcd.yml.tpl'
        final String tplName2 = 'etcd.yml.single.node.tpl'

        String tplFilePath = PluginManager.pluginsResourceDirPath() + '/etcd/EtcdYmlTpl.groovy'
        String tplFilePath2 = PluginManager.pluginsResourceDirPath() + '/etcd/EtcdYmlSingleNodeTpl.groovy'
        String content = new File(tplFilePath).text
        String content2 = new File(tplFilePath2).text

        TplParamsConf tplParams = new TplParamsConf()
        tplParams.addParam('enableV2', 'true', 'string')
        tplParams.addParam('dataDir', '/data/etcd', 'string')

        def imageName = imageName()

        def one = new ImageTplDTO(imageName: imageName, name: tplName).queryFields('id').one()
        if (!one) {
            new ImageTplDTO(
                    name: tplName,
                    imageName: imageName,
                    tplType: ImageTplDTO.TplType.mount.name(),
                    mountDist: '/etcd/etcd.yml',
                    content: content,
                    isParentDirMount: false,
                    params: tplParams
            ).add()
        }

        def one2 = new ImageTplDTO(imageName: imageName, name: tplName2).queryFields('id').one()
        if (!one2) {
            new ImageTplDTO(
                    name: tplName2,
                    imageName: imageName,
                    tplType: ImageTplDTO.TplType.mount.name(),
                    mountDist: '/etcd/etcd.yml',
                    content: content2,
                    isParentDirMount: false,
                    params: tplParams
            ).add()
        }

        addNodeVolumeForUpdate('/data/etcd', '/data/etcd')
    }

    private void initChecker() {
        CheckerHolder.instance.add new Checker() {

            @Override
            boolean check(CreateContainerConf conf, JobStepKeeper keeper) {
                def containerNumber = conf.conf.containerNumber
                def endpoints = (0..<containerNumber).collect {
                    conf.conf.isLimitNode ? "http://${conf.conf.targetNodeIpList[0]}:${2379 + 100 * it}"
                            : "http://${conf.conf.targetNodeIpList[it]}:2379"
                }

                conf.conf.envList << new KVPair('ENDPOINTS', endpoints.join(','))
                true
            }

            @Override
            Checker.Type type() {
                Checker.Type.before
            }

            @Override
            String name() {
                'etcd set env'
            }

            @Override
            String imageName() {
                EtcdPlugin.this.imageName()
            }
        }

        HealthCheckerHolder.instance.add new HealthChecker() {
            @Override
            String name() {
                'etcd endpoints status'
            }

            @Override
            String imageName() {
                EtcdPlugin.this.imageName()
            }

            @Override
            boolean check(AppDTO app) {
                final String cmd = '/etcd/etcdctl --write-out=table --endpoints=$ENDPOINTS endpoint status'

                def containerList = InMemoryAllContainerManager.instance.getContainerList(app.clusterId, app.id)
                if (!containerList) {
                    log.warn 'failed get container list, clusterId: {}, appId: {}', app.clusterId, app.id
                    return false
                }

                def containerNumber = app.conf.containerNumber

                for (x in containerList) {
                    def r = AgentCaller.instance.agentScriptExe(app.clusterId, x.nodeIp,
                            'container init', [id: x.id, initCmd: cmd])

                    def message = r.getString('message')
                    if (!message) {
                        log.warn 'failed get container cmd result message, result: {}', r.toString()
                        return false
                    }

                    def lines = message.readLines()
                    if (lines.size() < (containerNumber + 1)) {
                        log.warn 'etcd endpoints status lines not match, result: {}', message
                        return false
                    }

                    def arrList = lines[(-containerNumber - 1)..-2].collect { line ->
                        def arr = line.split(/\|/)
                        arr
                    }

                    Set<String> raftTermSet = []
                    for (arr in arrList) {
                        if (arr.size() < 10) {
                            log.warn 'etcd endpoints status line not match, arr: {}', arr
                            return false
                        }

                        raftTermSet << arr[7]
                    }

                    if (raftTermSet.size() > 1) {
                        log.warn 'etcd raft term not match, raft term: {}', raftTermSet
                        return false
                    }

                    // only one leader
                    def leaderCount = arrList.count { arr ->
                        'true' == arr[5].trim()
                    }
                    if (leaderCount != 1) {
                        log.warn 'etcd leader number not match, leader count: {}', leaderCount
                        return false
                    }
                }

                true
            }
        }
    }

    @Override
    String group() {
        'key232323'
    }

    @Override
    String image() {
        'etcd'
    }

    @Override
    AppDTO demoApp(AppDTO app) {
        app.name = image()

        def conf = app.conf
        conf.group = group()
        conf.image = image()
        conf.tag = '3.4.24'

        conf.containerNumber = 3

        conf.dirVolumeList << new DirVolumeMount(
                dir: '/data/etcd', dist: '/data/etcd', mode: 'rw',
                nodeVolumeId: getNodeVolumeIdByDir('/data/etcd'))

        List<KVPair<String>> paramList = []
        paramList << new KVPair<String>(key: 'enableV2', value: 'true')
        paramList << new KVPair<String>(key: 'dataDir', value: '/data/etcd')

        if (conf.isLimitNode) {
            conf.fileVolumeList << new FileVolumeMount(
                    paramList: paramList,
                    dist: '/etcd/etcd.yml',
                    imageTplId: getImageTplIdByName('etcd.yml.single.node.tpl'))
        } else {
            conf.fileVolumeList << new FileVolumeMount(
                    paramList: paramList,
                    dist: '/etcd/etcd.yml',
                    imageTplId: getImageTplIdByName('etcd.yml.tpl'))
        }
        conf.portList << new PortMapping(privatePort: 2379, publicPort: 2379)
        conf.portList << new PortMapping(privatePort: 2380, publicPort: 2380)

        app
    }
}
