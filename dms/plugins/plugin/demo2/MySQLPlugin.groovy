package plugin.demo2

import com.alibaba.fastjson.JSON
import com.alibaba.fastjson.JSONObject
import common.Event
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import model.*
import model.json.*
import model.server.CreateContainerConf
import org.segment.d.D
import org.segment.d.Ds
import org.segment.d.dialect.MySQLDialect
import plugin.BasePlugin
import plugin.PluginManager
import server.scheduler.checker.*
import server.scheduler.processor.JobStepKeeper

@CompileStatic
@Slf4j
class MySQLPlugin extends BasePlugin {
    @Override
    String name() {
        'mysql'
    }

    @Override
    void init() {
        super.init()

        initImageConfig()
        initChecker()
        initCleaner()
        initExporter()
        initHealthChecker()
    }

    private void initImageConfig() {
        addEnvIfNotExists('MYSQL_ROOT_PASSWORD', 'MYSQL_ROOT_PASSWORD')
        addEnvIfNotExists('INIT_DATABASE_NAME', 'INIT_DATABASE_NAME')
        addEnvIfNotExists('INIT_DATABASE_USER', 'INIT_DATABASE_USER')

        addEnvIfNotExists('DEFAULT_PARAMS_TPL_FILE', 'DEFAULT_PARAMS_TPL_FILE',
                'generate my.cnf default params, default conf_output_mysql.json')

        // exporter env
        def exporterImageName = 'prom/mysqld-exporter'
        ['DATA_SOURCE_NAME'].each {
            def one = new ImageEnvDTO(imageName: exporterImageName, env: it).one()
            if (!one) {
                new ImageEnvDTO(imageName: exporterImageName, name: it, env: it).add()
            }
        }
        // exporter port
        [9104].each {
            def two = new ImagePortDTO(imageName: exporterImageName, port: it).one()
            if (!two) {
                new ImagePortDTO(imageName: exporterImageName, name: it.toString(), port: it).add()
            }
        }

        addPortIfNotExists('3306', 3306)

        final String tplName = 'my.cnf.tpl'

        String tplFilePath = PluginManager.pluginsResourceDirPath() + '/mysql/MyCnfTpl.groovy'
        String content = new File(tplFilePath).text

        TplParamsConf tplParams = new TplParamsConf()
        tplParams.addParam('port', '3306', 'int')
        tplParams.addParam('dataDir', '/data/mysql-data', 'string')
        tplParams.addParam('logDir', '/data/mysql-log', 'string')
        tplParams.addParam('isMasterSlave', 'false', 'string')
        tplParams.addParam('customParameters', 'character-set-server=utf8', 'string')
        tplParams.addParam('defaultParameters', 'auto generated', 'string')

        def imageName = imageName()

        def one = new ImageTplDTO(imageName: imageName, name: tplName).queryFields('id').one()
        if (!one) {
            new ImageTplDTO(
                    name: tplName,
                    imageName: imageName,
                    tplType: ImageTplDTO.TplType.mount.name(),
                    mountDist: '/etc/my.cnf',
                    content: content,
                    isParentDirMount: false,
                    params: tplParams
            ).add()
        }

        addNodeVolumeForUpdate('data-dir', '/data/mysql-data')
        addNodeVolumeForUpdate('log-dir', '/data/mysql-log')
    }

    int getPublicPort(AppConf conf) {
        def port = getParamValue(conf, 'port') as int

        int publicPort = port
        if ('host' != conf.networkMode) {
            def pm = conf.portList.find { it.privatePort == port }
            if (pm) {
                publicPort = pm.publicPort
            }
        }
        publicPort
    }

    String getParamValue(AppConf conf, String key) {
        getParamValueFromTpl(conf, '/etc/my.cnf', key)
    }

    private void initChecker() {
        CheckerHolder.instance.add new Checker() {
            @Override
            boolean check(CreateContainerConf conf, JobStepKeeper keeper) {
                def fileName = getEnvValue(conf.conf, 'DEFAULT_PARAMS_TPL_FILE') ?: 'conf_output_mysql.json'
                def file = new File(PluginManager.pluginsResourceDirPath() + '/mysql/' + fileName)
                if (!file.exists()) {
                    log.warn 'default parameters config file not exists: ' + file.absolutePath
                    return true
                }

                def content = file.text
                def arr = JSON.parseArray(content)

                List<String> customParameters = []
                List<String> defaultParameters = []

                def mountFileOne = conf.conf.fileVolumeList.find { it.dist == '/etc/my.cnf' }
                def paramDefaultParameters = mountFileOne.paramList.find { it.key == 'defaultParameters' }

                Set<String> skipKeySet = []
                def oldValue = getParamValue(conf.conf, 'customParameters')
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
                    defaultParameters << name + '=' + jo.getString('currentValue')
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
                'mysql cnf default parameters generate'
            }

            @Override
            String imageName() {
                MySQLPlugin.this.imageName()
            }
        }

        CheckerHolder.instance.add new Checker() {
            @Override
            boolean check(CreateContainerConf conf, JobStepKeeper keeper) {
                int publicPort = getPublicPort(conf.conf)

                def password = getEnvValue(conf.conf, 'MYSQL_ROOT_PASSWORD')
                def databaseName = getEnvValue(conf.conf, 'INIT_DATABASE_NAME')
                def databaseUser = getEnvValue(conf.conf, 'INIT_DATABASE_USER')

                Ds ds
                try {
                    ds = Ds.dbType(Ds.DBType.mysql).connect(conf.nodeIp, publicPort, 'mysql', 'root', password)
                } catch (Exception e) {
                    // retry once
                    Thread.sleep(10000)
                    try {
                        ds = Ds.dbType(Ds.DBType.mysql).connect(conf.nodeIp, publicPort, 'mysql', 'root', password)
                    } catch (Exception ee) {
                        log.error('reconnect mysql error', ee)
                        return false
                    }
                }
                def d = new D(ds, new MySQLDialect())

                try {
                    String createDbSql = "create database if not exists ${databaseName}"
                    Event.builder().type(Event.Type.app).reason('after init sql execute').result(conf.appId).
                            build().log(conf.nodeIp + ' - ' + createDbSql).toDto().add()
                    try {
                        d.exe(createDbSql)
                    } catch (Exception e) {
                        Event.builder().type(Event.Type.app).reason('after init sql execute error').result(conf.appId).
                                build().log(conf.nodeIp + ' - ' + createDbSql + ' - ' + e.message).toDto().add()
                        log.error('after init sql execute error - ' + createDbSql, e)
                    }
                    keeper.next(JobStepKeeper.Step.yourStep, 'after init create database', databaseName)

                    List<String> ddlList = []
                    ddlList << "create user if not exists export_user IDENTIFIED BY 'export_user_pass'"
                    ddlList << "GRANT REPLICATION CLIENT, PROCESS, SELECT ON *.* TO 'export_user'@'%'"
                    String adminUser = databaseUser
                    if (adminUser.toLowerCase() != 'root') {
                        ddlList << "create user ${adminUser} IDENTIFIED BY '${password}'".toString()
                        ddlList << "GRANT ALL PRIVILEGES ON *.* TO '${adminUser}'@'%'".toString()
                    }
                    ddlList << "flush privileges"

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
                    keeper.next(JobStepKeeper.Step.yourStep, 'after init create user', adminUser)
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
                'MySQL create database and user'
            }

            @Override
            String imageName() {
                MySQLPlugin.this.imageName()
            }
        }

        CheckerHolder.instance.add(new Checker() {
            @Override
            boolean check(CreateContainerConf conf, JobStepKeeper keeper) {
                int publicPort = getPublicPort(conf.conf)
                def password = getEnvValue(conf.conf, 'MYSQL_ROOT_PASSWORD')

                Ds ds
                try {
                    ds = Ds.dbType(Ds.DBType.mysql).connect(conf.nodeIp, publicPort, 'mysql', 'root', password)
                } catch (Exception e) {
                    // retry once
                    Thread.sleep(10000)
                    try {
                        ds = Ds.dbType(Ds.DBType.mysql).connect(conf.nodeIp, publicPort, 'mysql', 'root', password)
                    } catch (Exception ee) {
                        log.error('reconnect mysql error', ee)
                        return false
                    }
                }
                def d = new D(ds, new MySQLDialect())

                final String slaveUser = 'slave_user'
                final String slavePassword = 'slave@pass'
                try {

                    if (conf.instanceIndex == 0) {
                        // this is master
                        // create slave user and grant
                        String addSlaveUserDdl = """
create user if not exists ${slaveUser} IDENTIFIED BY '${slavePassword}';
GRANT REPLICATION SLAVE, REPLICATION CLIENT ON *.* TO '${slaveUser}'@'%' IDENTIFIED BY '${slavePassword}';
GRANT Select ON *.* TO '${slaveUser}'@'%';
GRANT File ON *.* TO '${slaveUser}'@'%';
flush privileges;
"""

                        addSlaveUserDdl.trim().split(';').each { String it ->
                            def line = it.trim()
                            if (!line) {
                                return
                            }
                            Event.builder().type(Event.Type.app).reason('master sql execute').result(conf.appId).
                                    build().log(conf.nodeIp + ' - ' + line).toDto().add()
                            try {
                                d.exe(line)
                            } catch (Exception e) {
                                Event.builder().type(Event.Type.app).reason('master sql execute error').result(conf.appId).
                                        build().log(conf.nodeIp + ' - ' + line + ' - ' + e.message).toDto().add()
                                log.error('master sql execute error - ' + line, e)
                            }
                            log.info 'done sql <-'
                        }
                        keeper.next(JobStepKeeper.Step.yourStep, 'master slave user create', slaveUser)
                    } else {
                        // this is slave
                        // get master log position first
                        String masterHost = conf.conf.targetNodeIpList[0]

                        String sql = """
CHANGE MASTER TO 
    MASTER_HOST='${masterHost}', 
    MASTER_PORT=${publicPort}, 
    MASTER_USER='${slaveUser}', 
    MASTER_PASSWORD='${slavePassword}', 
    MASTER_HEARTBEAT_PERIOD=2,
    MASTER_CONNECT_RETRY=1,
    MASTER_RETRY_COUNT=86400,
    MASTER_AUTO_POSITION=1;
set global slave_net_timeout=8;
start slave;
"""
                        sql.trim().split(';').each { String it ->
                            def line = it.trim()
                            if (!line) {
                                return
                            }
                            Event.builder().type(Event.Type.app).reason('slave sql execute').result(conf.appId).
                                    build().log(conf.nodeIp + ' - ' + line).toDto().add()
                            try {
                                d.exe(line)
                            } catch (Exception e) {
                                Event.builder().type(Event.Type.app).reason('slave sql execute error').result(conf.appId).
                                        build().log(conf.nodeIp + ' - ' + line + ' - ' + e.message).toDto().add()
                                log.error('slave sql execute error - ' + line, e)
                            }
                            log.info 'done sql <-'
                        }
                        keeper.next(JobStepKeeper.Step.yourStep, 'slave set and start', sql)

                        log.info 'slave status for instance {}', conf.instanceIndex
                        d.one('show slave status').each { k, v ->
                            log.info '{} : {}', k.toString().padLeft(25, ' '), v
                        }
                    }
                    true
                } finally {
                    if (ds) {
                        ds.closeConnect()
                    }
                }
                true
            }

            @Override
            Checker.Type type() {
                Checker.Type.after
            }

            @Override
            String name() {
                'MySQL master slave set'
            }

            @Override
            String imageName() {
                MySQLPlugin.this.imageName()
            }
        })
    }

    private void initCleaner() {
        CleanerHolder.instance.add new Cleaner() {
            @Override
            boolean clean(AppDTO app) {
                (0..<app.conf.containerNumber).each { instanceIndex ->
                    def dsName = 'db_mysql_' + app.id + '_' + instanceIndex
                    def ds = Ds.remove(dsName)
                    if (ds) {
                        try {
                            ds.closeConnect()
                        } catch (Exception e) {
                            log.warn 'close mysql connect error {} {}', dsName, e.message
                        }
                    }
                }
                true
            }

            @Override
            String name() {
                'remove mysql connect'
            }

            @Override
            List<String> imageName() {
                [MySQLPlugin.this.imageName()]
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

                def rawConf = createContainerConf.app.conf
                // one mysql instance -> one mysqld_exporter application
                conf.containerNumber = createContainerConf.conf.containerNumber
                conf.targetNodeIpList = createContainerConf.conf.targetNodeIpList
                conf.registryId = rawConf.registryId
                conf.group = 'prom'
                conf.image = 'mysqld-exporter'
                conf.tag = 'latest'
                conf.memMB = 64
                conf.cpuFixed = 0.1

                // ${nodeIp} is a placeholder, will be replaced by real ip
                def envValue = "export_user:export_user_pass@tcp(\${nodeIp}:${publicPort})/mysql".toString()
                conf.envList << new KVPair<String>('DATA_SOURCE_NAME', envValue)
                log.info envValue

                final int exporterPort = 9104
                def exporterPublicPort = exporterPort + (3306 - publicPort)
                conf.networkMode = 'bridge'
                conf.portList << new PortMapping(privatePort: exporterPort, publicPort: exporterPublicPort)

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
                'MySQL create exporter application'
            }

            @Override
            String imageName() {
                MySQLPlugin.this.imageName()
            }
        }
    }

    private void initHealthChecker() {
        HealthCheckerHolder.instance.add new HealthChecker() {

            @Override
            String name() {
                'MySQL master slave health check'
            }

            @Override
            String imageName() {
                MySQLPlugin.this.imageName()
            }

            @Override
            boolean check(AppDTO app) {
                def publicPort = getPublicPort(app.conf)
                def password = getEnvValue(app.conf, 'MYSQL_ROOT_PASSWORD')

                def isMasterSlave = 'true' == getParamValue(app.conf, 'isMasterSlave')

                def targetNodeIpList = app.conf.targetNodeIpList
                (0..<app.conf.containerNumber).each { instanceIndex ->
                    def dsName = 'db_mysql_' + app.id + '_' + instanceIndex
                    def nodeIp = targetNodeIpList[instanceIndex]

                    def ds = Ds.one(dsName) ?: Ds.dbType(Ds.DBType.mysql).connectWithPool(nodeIp, publicPort,
                            'mysql', 'root', password, 2, 5).cacheAs(dsName)
                    def d = new D(ds, new MySQLDialect())
                    d.one('select 1 as a')
                    log.info 'mysql health check ok, app id: {}, instance index: {}, node ip: {}', app.id, instanceIndex, nodeIp

                    if (0 != instanceIndex && isMasterSlave) {
                        // slave
                        def row = d.one('show slave status')
                        // camel style
                        if ('Yes' != row.get('slaveIoRunning') || 'Yes' != row.get('slaveSqlRunning')) {
                            log.warn 'slave io/sql running not Yes, instance index: {}, row: {}', instanceIndex, row
                            return false
                        }
                    }
                }

                true
            }
        }

    }

    @Override
    String group() {
        'library'
    }

    @Override
    String image() {
        'mysql'
    }

    @Override
    String version() {
        '5.7'
    }
}
