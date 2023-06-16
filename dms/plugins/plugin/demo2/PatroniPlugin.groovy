package plugin.demo2

import com.alibaba.fastjson.JSON
import com.alibaba.fastjson.JSONObject
import com.github.kevinsawicki.http.HttpRequest
import common.Event
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import model.*
import model.json.*
import model.server.CreateContainerConf
import org.segment.d.D
import org.segment.d.Ds
import org.segment.d.dialect.PGDialect
import plugin.BasePlugin
import plugin.PluginManager
import server.scheduler.checker.*
import server.scheduler.processor.JobStepKeeper

@CompileStatic
@Slf4j
class PatroniPlugin extends BasePlugin {
    @Override
    String name() {
        'patroni'
    }

    @Override
    void init() {
        super.init()

        initImageConfig()
        initImageConfigPgAdmin()
        initImageConfigHaproxy()
        initChecker()
        initCleaner()
        initExporter()
        initHaproxy()
        initHealthChecker()
    }

    private void initImageConfig() {
        addEnvIfNotExists('DEFAULT_PARAMS_TPL_FILE', 'DEFAULT_PARAMS_TPL_FILE',
                'generate patroni.yml bootstrap postgresql default params, default conf_output_postgresql.json')
        addEnvIfNotExists('PGBACKREST_LOG_PATH', 'PGBACKREST_LOG_PATH')

        // exporter env
        def exporterImageName = 'prometheuscommunity/postgres-exporter'
        ['DATA_SOURCE_NAME'].each {
            def one = new ImageEnvDTO(imageName: exporterImageName, env: it).one()
            if (!one) {
                new ImageEnvDTO(imageName: exporterImageName, name: it, env: it).add()
            }
        }
        // exporter port
        [9187].each {
            def two = new ImagePortDTO(imageName: exporterImageName, port: it).one()
            if (!two) {
                new ImagePortDTO(imageName: exporterImageName, name: it.toString(), port: it).add()
            }
        }

        addPortIfNotExists('5432', 5432)
        // patroni port
        addPortIfNotExists('4432', 4432)

        final String tplName = 'patroni.yml.tpl'
        final String tplName2 = 'patroni.yml.single.node.tpl'
        final String tplName3 = 'pgbackrest.conf.tpl'

        String tplFilePath = PluginManager.pluginsResourceDirPath() + '/patroni/PatroniYmlTpl.groovy'
        String tplFilePath2 = PluginManager.pluginsResourceDirPath() + '/patroni/PatroniYmlSingleNodeTpl.groovy'
        String tplFilePath3 = PluginManager.pluginsResourceDirPath() + '/patroni/PgbackrestConfTpl.groovy'
        String content = new File(tplFilePath).text
        String content2 = new File(tplFilePath2).text
        String content3 = new File(tplFilePath3).text

        TplParamsConf tplParams = new TplParamsConf()
        tplParams.addParam('etcdAppName', 'etcd', 'string')
        // patroni port
        tplParams.addParam('port', '4432', 'int')
        tplParams.addParam('pgPort', '5432', 'int')
        tplParams.addParam('pgEncoding', 'utf8', 'string')
        tplParams.addParam('pgPassword', 'postgres1234', 'string')
        tplParams.addParam('dataDir', '/data/pg', 'string')
        tplParams.addParam('customParameters', 'hot_standby=on', 'string')
        tplParams.addParam('defaultParameters', 'auto generated', 'string')

        TplParamsConf tplParams3 = new TplParamsConf()
        tplParams3.addParam('dataDir', '/data/pg', 'string')

        def imageName = imageName()

        def one = new ImageTplDTO(imageName: imageName, name: tplName).queryFields('id').one()
        def one2 = new ImageTplDTO(imageName: imageName, name: tplName2).queryFields('id').one()
        def one3 = new ImageTplDTO(imageName: imageName, name: tplName3).queryFields('id').one()
        if (!one) {
            new ImageTplDTO(
                    name: tplName,
                    imageName: imageName,
                    tplType: ImageTplDTO.TplType.mount.name(),
                    mountDist: '/etc/patroni.yml',
                    content: content,
                    isParentDirMount: false,
                    params: tplParams
            ).add()
        }
        if (!one2) {
            new ImageTplDTO(
                    name: tplName2,
                    imageName: imageName,
                    tplType: ImageTplDTO.TplType.mount.name(),
                    mountDist: '/etc/patroni.yml',
                    content: content2,
                    isParentDirMount: false,
                    params: tplParams
            ).add()
        }
        if (!one3) {
            new ImageTplDTO(
                    name: tplName3,
                    imageName: imageName,
                    tplType: ImageTplDTO.TplType.mount.name(),
                    mountDist: '/etc/pgbackrest/pgbackrest.conf',
                    content: content3,
                    isParentDirMount: false,
                    params: tplParams3
            ).add()
        }

        addNodeVolumeForUpdate('data-dir', '/data/pg')
        addNodeVolumeForUpdate('run-dir', '/run/postgresql')
        addNodeVolumeForUpdate('pgbackrest-log-dir', '/var/log/pgbackrest')
        addNodeVolumeForUpdate('pgbackrest-lib-dir', '/var/lib/pgbackrest')
    }

    private void initImageConfigPgAdmin() {
        // PGADMIN_DEFAULT_PASSWORD
        def imageName = 'dpage/pgadmin4'

        ['PGADMIN_DEFAULT_EMAIL', 'PGADMIN_DEFAULT_PASSWORD', 'PGADMIN_LISTEN_PORT', 'PGADMIN_ENABLE_TLS'].each {
            def one = new ImageEnvDTO(imageName: imageName, name: it).one()
            if (!one) {
                new ImageEnvDTO(imageName: imageName, name: it, env: it).add()
            }
        }

        [80, 443].each {
            def one = new ImagePortDTO(imageName: imageName, port: it).one()
            if (!one) {
                new ImagePortDTO(imageName: imageName, name: it.toString(), port: it).add()
            }
        }

        def dir = '/var/lib/pgadmin'
        def one = new NodeVolumeDTO(imageName: imageName, dir: dir).one()
        if (!one) {
            new NodeVolumeDTO(imageName: imageName, name: dir, dir: dir, clusterId: 1).add()
        }
    }

    private void initImageConfigHaproxy() {
        def imageName = 'library/haproxy'

        [5000, 7000].each {
            def one = new ImagePortDTO(imageName: imageName, port: it).one()
            if (!one) {
                new ImagePortDTO(imageName: imageName, name: it.toString(), port: it).add()
            }
        }

        final String tplName = 'haproxy.cfg.tpl'

        String tplFilePath = PluginManager.pluginsResourceDirPath() + '/patroni/HaproxyCfgTpl.groovy'
        String content = new File(tplFilePath).text

        TplParamsConf tplParams = new TplParamsConf()
        tplParams.addParam('patroniAppNames', 'patroni', 'string')
        tplParams.addParam('patroniAppIsSingleNode', 'false', 'string')
        tplParams.addParam('port', '5000', 'int')
        tplParams.addParam('statsPort', '7000', 'int')
        tplParams.addParam('maxConn', '100', 'int')

        def one = new ImageTplDTO(imageName: imageName, name: tplName).queryFields('id').one()
        if (!one) {
            new ImageTplDTO(
                    name: tplName,
                    imageName: imageName,
                    tplType: ImageTplDTO.TplType.mount.name(),
                    mountDist: '/usr/local/etc/haproxy/haproxy.cfg',
                    content: content,
                    isParentDirMount: false,
                    params: tplParams
            ).add()
        }
    }

    static int getPublicPort(AppConf conf, String key = 'pgPort') {
        def port = getParamOneValue(conf, key) as int

        int publicPort = port
        if ('host' != conf.networkMode) {
            def pm = conf.portList.find { it.privatePort == port }
            if (pm) {
                publicPort = pm.publicPort
            }
        }
        publicPort
    }

    static String getParamOneValue(AppConf conf, String key) {
        def mountFileOne = conf.fileVolumeList.find { it.dist == '/etc/patroni.yml' }
        def paramOne = mountFileOne.paramList.find { it.key == key }
        paramOne.value
    }

    static String getEnvOneValue(AppConf conf, String key) {
        def envOne = conf.envList.find { it.key == key }
        envOne?.value.toString()
    }

    private void initChecker() {
        CheckerHolder.instance.add new Checker() {
            @Override
            boolean check(CreateContainerConf conf, JobStepKeeper keeper) {
                // set right user
                conf.conf.user = 'repl'
                // set run dir or mkdir
                def runDirDirMountOne = conf.conf.dirVolumeList.find { it.dist == '/run/postgresql' }
                if (!runDirDirMountOne) {
                    conf.conf.dirVolumeList.add(new DirVolumeMount(dir: '/run/postgresql', dist: '/run/postgresql', mode: 'rw'))
                }
                // set pgbackrest log dir env
                def logPathEnvOne = conf.conf.envList.find { it.key == 'PGBACKREST_LOG_PATH' }
                if (!logPathEnvOne) {
                    conf.conf.envList.add(new KVPair<String>(key: 'PGBACKREST_LOG_PATH', value: '/var/log/pgbackrest'))
                }

//                def dir = conf.conf.dirVolumeList.collect { it.dir }.join(',')
//                log.warn 'ready mkdir dirs: {}', dir
//                AgentCaller.instance.agentScriptExe(conf.app.clusterId, conf.nodeIp, 'mk dir', [dir: dir])

                def fileName = getEnvOneValue(conf.conf, 'DEFAULT_PARAMS_TPL_FILE') ?: 'conf_output_postgresql.json'
                def file = new File(PluginManager.pluginsResourceDirPath() + '/patroni/' + fileName)
                if (!file.exists()) {
                    log.warn 'default parameters config file not exists: ' + file.absolutePath
                    return true
                }

                def content = file.text
                def arr = JSON.parseArray(content)

                List<String> customParameters = []
                List<String> defaultParameters = []

                def mountFileOne = conf.conf.fileVolumeList.find { it.dist == '/etc/patroni.yml' }
                def paramDefaultParameters = mountFileOne.paramList.find { it.key == 'defaultParameters' }

                Set<String> skipKeySet = []
                def oldValue = getParamOneValue(conf.conf, 'customParameters')
                if (oldValue) {
                    def oldValueArr = oldValue.split(',')
                    for (oldValueOne in oldValueArr) {
                        def name = oldValueOne.split('=')[0].trim()
                        skipKeySet << name
                    }
                    customParameters.addAll(oldValueArr)
                }
                for (one in arr) {
                    def jo = one as JSONObject
                    def name = jo.getString('name')
                    if (name in skipKeySet) {
                        continue
                    }
                    defaultParameters << name + '=' + jo.getString('defaultValue')
                }

                paramDefaultParameters.value = defaultParameters.join(',')

                true
            }

            @Override
            Checker.Type type() {
                Checker.Type.before
            }

            @Override
            String name() {
                'patroni bootstrap postgresql default parameters generate'
            }

            @Override
            String imageName() {
                PatroniPlugin.this.imageName()
            }

            @Override
            String script(CreateContainerConf conf) {
                null
            }
        }

        CheckerHolder.instance.add new Checker() {

            @Override
            boolean check(CreateContainerConf conf, JobStepKeeper keeper) {
                return true
            }

            @Override
            Checker.Type type() {
                Checker.Type.init
            }

            @Override
            String name() {
                'pgbackrest init'
            }

            @Override
            String imageName() {
                PatroniPlugin.this.imageName()
            }

            @Override
            String script(CreateContainerConf conf) {
                """
'''
chown repl /data
mkdir -p /var/lib/pgbackrest
mkdir -p /var/log/pgbackrest
chmod 750 /var/lib/pgbackrest
chown postgres:postgres /var/lib/pgbackrest
'''
"""
            }
        }

        CheckerHolder.instance.add new Checker() {
            @Override
            boolean check(CreateContainerConf conf, JobStepKeeper keeper) {
                int publicPort = getPublicPort(conf.conf)
                def password = getParamOneValue(conf.conf, 'pgPassword')

                Ds ds
                try {
                    ds = Ds.dbType(Ds.DBType.postgresql).connect(conf.nodeIp, publicPort, 'postgres', 'postgres', password)
                } catch (Exception e) {
                    // retry once
                    Thread.sleep(10000)
                    try {
                        ds = Ds.dbType(Ds.DBType.postgresql).connect(conf.nodeIp, publicPort, 'postgres', 'postgres', password)
                    } catch (Exception ee) {
                        log.error('reconnect pg error', ee)
                        return false
                    }
                }
                def d = new D(ds, new PGDialect())

                try {
                    List<String> ddlList = []
                    ddlList << "create user export_user password 'export_user_pass'"
                    ddlList << "GRANT pg_monitor TO export_user"
                    // create extension
                    ddlList << "create extension if not exists citus"
                    ddlList << "create extension if not exists timescaledb"

                    ddlList.each {
                        def line = it.trim()
                        if (!line) {
                            return
                        }
                        Event.builder().type(Event.Type.app).reason('after init sql execute').result(conf.appId).
                                build().log(conf.nodeIp + ' - ' + line).toDto().add()
                        try {
                            d.exe(line)
                        } catch (Exception e) {
                            Event.builder().type(Event.Type.app).reason('after init sql execute error').result(conf.appId).
                                    build().log(conf.nodeIp + ' - ' + line + ' - ' + e.message).toDto().add()
                            log.error('after init sql execute error - ' + line, e)
                        }
                        log.info 'done sql <-'
                    }
                    keeper.next(JobStepKeeper.Step.yourStep, 'after init create exporter user', 'export_user')
                    true
                } finally {
                    if (ds) {
                        ds.closeConnect()
                    }
                    true
                }
            }

            @Override
            Checker.Type type() {
                Checker.Type.after
            }

            @Override
            String name() {
                'patroni create exporter user'
            }

            @Override
            String imageName() {
                PatroniPlugin.this.imageName()
            }

            @Override
            String script(CreateContainerConf conf) {
                null
            }
        }
    }

    private void initCleaner() {
        CleanerHolder.instance.add new Cleaner() {
            @Override
            boolean clean(AppDTO app) {
                (0..<app.conf.containerNumber).each { instanceIndex ->
                    def dsName = 'db_pg_' + app.id + '_' + instanceIndex
                    def ds = Ds.remove(dsName)
                    if (ds) {
                        try {
                            ds.closeConnect()
                        } catch (Exception e) {
                            log.warn 'close pg connect error {} {}', dsName, e.message
                        }
                    }
                }
                true
            }

            @Override
            String name() {
                'remove pg connect'
            }

            @Override
            List<String> imageName() {
                [PatroniPlugin.this.imageName()]
            }
        }
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

                def app = new AppDTO()
                app.name = createContainerConf.appId + '_' + createContainerConf.app.name + '_exporter'

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

                int registryId = BasePlugin.addRegistryIfNotExist('quay.io', 'https://quay.io')
                // one pg instance -> one postgres-exporter application
                conf.containerNumber = createContainerConf.conf.containerNumber
                conf.targetNodeIpList = createContainerConf.conf.targetNodeIpList
                conf.registryId = registryId
                conf.group = 'prometheuscommunity'
                conf.image = 'postgres-exporter'
                conf.tag = 'latest'
                conf.memMB = 64
                conf.cpuShare = 128

                // check if single node
                def ymlOne = createContainerConf.conf.fileVolumeList.find { it.dist == '/etc/patroni.yml' }
                def tplOne = new ImageTplDTO(id: ymlOne.imageTplId).one()
                def isSingleNode = tplOne.name.contains('single.node')

                String envValue
                if (isSingleNode) {
                    // ${nodeIp} is a placeholder, will be replaced by real ip
                    envValue = "postgresql://export_user:export_user_pass@\${nodeIp}:\${${publicPort} + 100 * instanceIndex}/postgres?sslmode=disable".toString()
                } else {
                    envValue = "postgresql://export_user:export_user_pass@\${nodeIp}:${publicPort}/postgres?sslmode=disable".toString()
                }

                conf.envList << new KVPair<String>('DATA_SOURCE_NAME', envValue)
                log.info envValue

                final int exporterPort = 9187
                def exporterPublicPort = exporterPort + (5432 - publicPort)
                conf.networkMode = 'bridge'
                if (isSingleNode) {
                    conf.portList << new PortMapping(privatePort: exporterPort, publicPort: -1)
                } else {
                    conf.portList << new PortMapping(privatePort: exporterPort, publicPort: exporterPublicPort)
                }

                // monitor
                def monitorConf = new MonitorConf()
                app.monitorConf = monitorConf
                monitorConf.port = exporterPort
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
                'patroni create exporter application'
            }

            @Override
            String imageName() {
                PatroniPlugin.this.imageName()
            }

            @Override
            String script(CreateContainerConf conf) {
                null
            }
        }
    }

    private void initHaproxy() {
        CheckerHolder.instance.add new Checker() {
            @Override
            boolean check(CreateContainerConf createContainerConf, JobStepKeeper keeper) {
                // only last container create haproxy application
                if (createContainerConf.instanceIndex != createContainerConf.conf.containerNumber - 1) {
                    return
                }

                // 1 haproxy instance, scale if u need
                final int defaultContainerNumber = 1
                def patroniAppName = createContainerConf.app.name
                def ymlOne = createContainerConf.conf.fileVolumeList.find { it.dist == '/etc/patroni.yml' }
                def pgPort = ymlOne.paramList.find { it.key == 'pgPort' }.value as int
                def portDiff = 5432 - pgPort

                def tplOne = new ImageTplDTO(id: ymlOne.imageTplId).one()
                def isSingleNode = tplOne.name.contains('single.node')

                def app = new AppDTO()
                app.name = createContainerConf.appId + '_haproxy'

                // check if application name duplicated
                def existsOne = new AppDTO(name: app.name).one()
                if (existsOne) {
                    log.warn('this haproxy application name already exists {}', app.name)
                    return true
                }

                app.clusterId = createContainerConf.app.clusterId
                app.namespaceId = createContainerConf.app.namespaceId
                // not auto first
                app.status = AppDTO.Status.manual.val

                def conf = new AppConf()
                app.conf = conf

                int registryId = BasePlugin.addRegistryIfNotExist('docker.io', 'https://docker.io')

                // one patroni instance -> one haproxy application
                conf.containerNumber = defaultContainerNumber
                conf.targetNodeIpList = createContainerConf.conf.targetNodeIpList[0..1]
                conf.registryId = registryId
                conf.group = 'library'
                conf.image = 'haproxy'
                conf.tag = 'lts-alpine'
                conf.memMB = 1024
                conf.cpuShare = 1024

                conf.networkMode = 'host'
                conf.portList << new PortMapping(privatePort: 5000, publicPort: 5000 + portDiff)
                conf.portList << new PortMapping(privatePort: 7000, publicPort: 7000 + portDiff)

                def tplHaproxyOne = new ImageTplDTO(imageName: 'library/haproxy', name: 'haproxy.cfg.tpl').one()
                def mountOne = new FileVolumeMount(imageTplId: tplHaproxyOne.id, content: tplHaproxyOne.content, dist: tplHaproxyOne.mountDist)
                mountOne.paramList << new KVPair<String>('patroniAppNames', patroniAppName)
                mountOne.paramList << new KVPair<String>('patroniAppIsSingleNode', isSingleNode.toString())
                mountOne.paramList << new KVPair<String>('maxConn', '100')
                mountOne.paramList << new KVPair<String>('port', (5000 + portDiff).toString())
                mountOne.paramList << new KVPair<String>('statsPort', (7000 + portDiff).toString())
                conf.fileVolumeList << mountOne

                // add application to dms
                int appId = app.add() as int
                app.id = appId
                log.info 'done create related haproxy application {}', appId

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
                log.info 'done create related haproxy application start job {}', jobId
                true
            }

            @Override
            Checker.Type type() {
                Checker.Type.after
            }

            @Override
            String name() {
                'patroni create haproxy application'
            }

            @Override
            String imageName() {
                PatroniPlugin.this.imageName()
            }

            @Override
            String script(CreateContainerConf conf) {
                null
            }
        }
    }

    private void initHealthChecker() {
        HealthCheckerHolder.instance.add new HealthChecker() {

            @Override
            String name() {
                'patroni state health check'
            }

            @Override
            String imageName() {
                PatroniPlugin.this.imageName()
            }

            @Override
            boolean check(AppDTO app) {
                def publicPort = getPublicPort(app.conf, 'port')

                def targetNodeIpList = app.conf.targetNodeIpList
                for (nodeIp in targetNodeIpList) {
                    def body = HttpRequest.get("http://${nodeIp}:${publicPort}/health".toString()).
                            connectTimeout(500).readTimeout(1000).body()
                    if (!body.contains('"state": "running"')) {
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
        'pg_patroni'
    }
}
