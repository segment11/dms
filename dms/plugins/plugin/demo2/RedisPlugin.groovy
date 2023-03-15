package plugin.demo2

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import model.*
import model.json.*
import model.server.CreateContainerConf
import plugin.BasePlugin
import plugin.PluginManager
import server.scheduler.checker.Checker
import server.scheduler.checker.CheckerHolder
import server.scheduler.processor.JobStepKeeper

@CompileStatic
@Slf4j
class RedisPlugin extends BasePlugin {
    @Override
    String name() {
        'redis'
    }

    @Override
    void init() {
        super.init()

        initImageConfig()
        initExporter()
    }

    private void initImageConfig() {
        // exporter env
        ['REDIS_ADDR', 'REDIS_PASSWORD'].each {
            def one = new ImageEnvDTO(imageName: 'oliver006/redis_exporter', env: it).one()
            if (!one) {
                new ImageEnvDTO(imageName: 'oliver006/redis_exporter', name: it, env: it).add()
            }
        }

        addPortIfNotExists('6379', 6379)

        final String tplName = 'redis.conf.tpl'

        String tplFilePath = PluginManager.pluginsResourceDirPath() + '/redis/RedisConfTpl.groovy'
        String content = new File(tplFilePath).text

        TplParamsConf tplParams = new TplParamsConf()
        tplParams.addParam('port', '6379', 'int')
        tplParams.addParam('dataDir', '/data/redis', 'string')
        tplParams.addParam('password', '123456', 'string')
        tplParams.addParam('customParameters', 'cluster-enabled yes', 'string')

        def imageName = imageName()

        def one = new ImageTplDTO(imageName: imageName, name: tplName).queryFields('id').one()
        if (!one) {
            new ImageTplDTO(
                    name: tplName,
                    imageName: imageName,
                    tplType: ImageTplDTO.TplType.mount.name(),
                    mountDist: '/etc/redis/redis.conf',
                    content: content,
                    isParentDirMount: false,
                    params: tplParams
            ).add()
        }

        addNodeVolumeForUpdate('data-dir', '/data/redis')
    }

    private static int getPublicPort(AppConf conf) {
        def port = getParamOneValue(conf, 'port') as int

        int publicPort = port
        if ('host' != conf.networkMode) {
            def pm = conf.portList.find { it.privatePort == port }
            if (pm) {
                publicPort = pm.publicPort
            }
        }
        publicPort
    }

    private static String getParamOneValue(AppConf conf, String key) {
        def mountFileOne = conf.fileVolumeList.find { it.dist == '/etc/redis/redis.conf' }
        def paramOne = mountFileOne.paramList.find { it.key == key }
        paramOne.value
    }

    private void initExporter() {
        CheckerHolder.instance.add new Checker() {
            @Override
            boolean check(CreateContainerConf createContainerConf, JobStepKeeper keeper) {
                // only last container create exporter application
                if (createContainerConf.instanceIndex != createContainerConf.conf.containerNumber - 1) {
                    return
                }
                def publicPort = getPublicPort(createContainerConf.conf)
                def password = getParamOneValue(createContainerConf.conf, 'password')

                def app = new AppDTO()
                app.name = createContainerConf.appId + '_exporter'

                // check if database name duplicated
                def existsOne = new AppDTO(name: app.name).one()
                if (existsOne) {
                    log.warn('this exporter application name already exists {}', app.name)
                    if (existsOne.conf.containerNumber != createContainerConf.conf.containerNumber) {
                        existsOne.conf.containerNumber = createContainerConf.conf.containerNumber
                        existsOne.conf.targetNodeIpList = createContainerConf.conf.targetNodeIpList

                        new AppDTO(id: existsOne.id, conf: existsOne.conf).update()
                        log.info 'update exporter application container number - {}', existsOne.id
                    }
                    return true
                }

                app.clusterId = createContainerConf.app.clusterId
                app.namespaceId = createContainerConf.app.namespaceId
                // not auto first
                app.status = AppDTO.Status.manual.val

                def conf = new AppConf()
                app.conf = conf

                def rawConf = createContainerConf.app.conf
                // one redis instance -> one redis_exporter application
                conf.containerNumber = createContainerConf.conf.containerNumber
                conf.targetNodeIpList = createContainerConf.conf.targetNodeIpList
                conf.registryId = rawConf.registryId
                conf.group = 'oliver006'
                conf.image = 'redis_exporter'
                conf.tag = 'latest'
                conf.memMB = 64
                conf.cpuShare = 128
                conf.user = '59000:59000'

                conf.envList << new KVPair<String>('REDIS_ADDR', 'redis://${nodeIp}:' + publicPort)
                conf.envList << new KVPair<String>('REDIS_PASSWORD', password)

                final int exporterDefaultPort = 9121

                def imageName = conf.group + '/' + conf.image
                def two = new ImagePortDTO(imageName: imageName, port: exporterDefaultPort).one()
                if (!two) {
                    new ImagePortDTO(imageName: imageName, name: 'redis exporter listen port', port: exporterDefaultPort).add()
                }

                // not bridge
                conf.networkMode = 'host'
                conf.portList << new PortMapping(privatePort: exporterDefaultPort, publicPort: exporterDefaultPort)

                // monitor
                def monitorConf = new MonitorConf()
                app.monitorConf = monitorConf
                monitorConf.port = exporterDefaultPort
                monitorConf.isHttpRequest = true
                monitorConf.httpRequestUri = '/metrics'

                // add application to dms
                int appId = app.add() as int
                app.id = appId
                log.info 'done create related exporter application {}', appId

                // create dms job
                List<Integer> needRunInstanceIndexList = []
                (0..<conf.containerNumber).each {
                    needRunInstanceIndexList << it
                }
                def job = new AppJobDTO(
                        appId: appId,
                        failNum: 0,
                        status: AppJobDTO.Status.created.val,
                        jobType: AppJobDTO.JobType.create.val,
                        createdDate: new Date(),
                        updatedDate: new Date()).
                        needRunInstanceIndexList(needRunInstanceIndexList)
                int jobId = job.add()
                job.id = jobId

                // set auto so dms can handle this job
                new AppDTO(id: appId, status: AppDTO.Status.auto.val).update()
                log.info 'done create related exporter application start job {}', jobId
                true
            }

            @Override
            Checker.Type type() {
                Checker.Type.after
            }

            @Override
            String name() {
                'Redis create exporter application'
            }

            @Override
            String imageName() {
                RedisPlugin.this.imageName()
            }

            @Override
            String script(CreateContainerConf conf) {
                null
            }
        }
    }

    @Override
    String group() {
        'library'
    }

    @Override
    String image() {
        'redis'
    }
}
