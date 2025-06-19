package plugin.demo2

import com.alibaba.fastjson.JSON
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import model.AppDTO
import model.ImageTplDTO
import model.json.*
import model.server.CreateContainerConf
import plugin.BasePlugin
import plugin.PluginManager
import server.scheduler.checker.Checker
import server.scheduler.checker.CheckerHolder
import server.scheduler.processor.JobStepKeeper

@CompileStatic
@Slf4j
class LokiPlugin extends BasePlugin {
    @Override
    String name() {
        'loki'
    }

    @Override
    void init() {
        super.init()

        initImageConfig()
        initChecker()
    }

    final String tplNameRedisExporter = 'loki.redis.exporter.yml.tpl'

    private void initImageConfig() {
        addPortIfNotExists('3100', 3100)
        addPortIfNotExists('9096', 9096)

        String tplFilePathRedisExporter = PluginManager.pluginsResourceDirPath() + '/loki/LokiRedisExporterYmlTpl.groovy'
        String contentRedisExporter = new File(tplFilePathRedisExporter).text

        TplParamsConf tplParams = new TplParamsConf()
        tplParams.addParam('dataDir', '/loki', 'string')
        tplParams.addParam('queryCacheMB', '256', 'int')
        tplParams.addParam('alertManagerNodeIp', '127.0.0.1', 'string')

        def imageName = imageName()

        def one = new ImageTplDTO(imageName: imageName, name: tplNameRedisExporter).queryFields('id').one()
        if (!one) {
            new ImageTplDTO(
                    name: tplNameRedisExporter,
                    imageName: imageName,
                    tplType: ImageTplDTO.TplType.mount,
                    mountDist: '/etc/config/loki-config.yml',
                    content: contentRedisExporter,
                    isParentDirMount: false,
                    params: tplParams
            ).add()
        }

        addNodeVolumeForUpdate('data-dir', '/loki', 'for chunks and rules, default /loki')
    }

    private void initChecker() {
        CheckerHolder.instance.add new Checker() {
            @Override
            boolean check(CreateContainerConf conf, JobStepKeeper keeper) {
                List<String> cmdArgs = []
                cmdArgs << '--config.file=/etc/config/loki-config.yml'

                conf.conf.cmd = JSON.toJSONString(cmdArgs)
                true
            }

            @Override
            Checker.Type type() {
                Checker.Type.before
            }

            @Override
            String name() {
                'loki cmd generate'
            }

            @Override
            String imageName() {
                LokiPlugin.this.imageName()
            }
        }
    }

    @Override
    String group() {
        'grafana'
    }

    @Override
    String image() {
        'loki'
    }

    String nodeDir = '/loki'

    @Override
    AppDTO demoApp(AppDTO app) {
        initAppBase(app)

        def conf = app.conf
        conf.tag = '3.4.1'

        if (conf.memMB == 0) {
            // set default
            conf.memMB = 512
            conf.memReservationMB = conf.memMB
            conf.cpuShares = 512
        }

        conf.dirVolumeList << new DirVolumeMount(
                dir: nodeDir, dist: '/loki', mode: 'rw',
                nodeVolumeId: getNodeVolumeIdByDir(nodeDir))

        conf.fileVolumeList << new FileVolumeMount(
                isReloadInterval: false,
                paramList: [
                        new KVPair<String>('dataDir', '/loki'),
                        new KVPair<String>('queryCacheMB', '256'),
                        new KVPair<String>('alertManagerNodeIp', '127.0.0.1'),
                ],
                dist: '/etc/config/loki-config.yml',
                imageTplId: getImageTplIdByName(tplNameRedisExporter))

        if (!conf.portList) {
            conf.portList << new PortMapping(privatePort: 3100, publicPort: 3100)
            conf.portList << new PortMapping(privatePort: 9096, publicPort: 9096)
        }

        app
    }
}
