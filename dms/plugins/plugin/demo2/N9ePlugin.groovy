package plugin.demo2

import common.Event
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import model.ImagePortDTO
import model.ImageTplDTO
import model.json.AppConf
import model.json.TplParamsConf
import model.server.CreateContainerConf
import org.segment.d.D
import org.segment.d.Ds
import org.segment.d.dialect.MySQLDialect
import plugin.BasePlugin
import plugin.PluginManager
import server.scheduler.checker.Checker
import server.scheduler.checker.CheckerHolder
import server.scheduler.processor.JobStepKeeper

@CompileStatic
@Slf4j
class N9ePlugin extends BasePlugin {
    @Override
    String name() {
        'n9e'
    }

    @Override
    void init() {
        super.init()

        initImageConfigIbex()
        initImageConfig()

//        initChecker()
    }

    private void initImageConfigIbex() {
        // /app/ibex server
        def imageName = 'ulric2019/ibex'

        [2090, 10090, 20090].each {
            def one = new ImagePortDTO(imageName: imageName, port: it).one()
            if (!one) {
                new ImagePortDTO(imageName: imageName, name: it.toString(), port: it).add()
            }
        }

        final String tplName = 'ibex.server.conf.tpl'
        final String tplName2 = 'ibex.agentd.conf.tpl'

        String tplFilePath = PluginManager.pluginsResourceDirPath() + '/n9e/IbexServerConfTpl.groovy'
        String tplFilePath2 = PluginManager.pluginsResourceDirPath() + '/n9e/IbexAgentdConfTpl.groovy'
        String content = new File(tplFilePath).text
        String content2 = new File(tplFilePath2).text

        TplParamsConf tplParams = new TplParamsConf()
        tplParams.addParam('host', '192.168.1.100', 'string')
        tplParams.addParam('port', '3306', 'int')
        tplParams.addParam('user', 'root', 'string')
        tplParams.addParam('password', 'root1234', 'string')

        def one = new ImageTplDTO(imageName: imageName, name: tplName).queryFields('id').one()
        if (!one) {
            new ImageTplDTO(
                    name: tplName,
                    imageName: imageName,
                    tplType: ImageTplDTO.TplType.mount.name(),
                    mountDist: '/app/etc/server.conf',
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
                    mountDist: '/app/etc/agentd.conf',
                    content: content2,
                    isParentDirMount: false
            ).add()
        }
    }

    private void initImageConfig() {
        // cmd /app/n9e webapi
        def imageName = imageName()

        [18000, 19000].each {
            def one = new ImagePortDTO(imageName: imageName, port: it).one()
            if (!one) {
                new ImagePortDTO(imageName: imageName, name: it.toString(), port: it).add()
            }
        }

        final String tplName = 'webapi.conf.tpl'
        final String tplName2 = 'server.conf.tpl'

        String tplFilePath = PluginManager.pluginsResourceDirPath() + '/n9e/WebapiConfTpl.groovy'
        String tplFilePath2 = PluginManager.pluginsResourceDirPath() + '/n9e/ServerConfTpl.groovy'
        String content = new File(tplFilePath).text
        String content2 = new File(tplFilePath2).text

        TplParamsConf tplParams = new TplParamsConf()
        tplParams.addParam('host', '192.168.1.100', 'string')
        tplParams.addParam('port', '3306', 'int')
        tplParams.addParam('user', 'root', 'string')
        tplParams.addParam('password', 'root1234', 'string')
        tplParams.addParam('promAddressList', 'http://localhost:9090', 'string')

        TplParamsConf tplParams2 = new TplParamsConf()
        tplParams2.addParam('host', '192.168.1.100', 'string')
        tplParams2.addParam('port', '3306', 'int')
        tplParams2.addParam('user', 'root', 'string')
        tplParams2.addParam('password', 'root1234', 'string')
        tplParams2.addParam('promAddressWrite', 'http://localhost:9090', 'string')

        def one = new ImageTplDTO(imageName: imageName, name: tplName).queryFields('id').one()
        if (!one) {
            new ImageTplDTO(
                    name: tplName,
                    imageName: imageName,
                    tplType: ImageTplDTO.TplType.mount.name(),
                    mountDist: '/app/etc/webapi.conf',
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
                    mountDist: '/app/etc/server.conf',
                    content: content2,
                    isParentDirMount: false,
                    params: tplParams2
            ).add()
        }

//        addNodeVolumeForUpdate('n9e etc dir', '/data/n9e/etc', '/app/etc_ext')
    }

    private static String getParamOneValue(AppConf conf, String key) {
        def mountFileOne = conf.fileVolumeList.find { it.dist == '/app/etc/config.toml' }
        def paramOne = mountFileOne.paramList.find { it.key == key }
        paramOne.value
    }

    private void initChecker() {
        CheckerHolder.instance.add new Checker() {
            @Override
            boolean check(CreateContainerConf conf, JobStepKeeper keeper) {
                def host = getParamOneValue(conf.conf, 'host')
                int port = getParamOneValue(conf.conf, 'port') as int
                def user = getParamOneValue(conf.conf, 'user')
                def password = getParamOneValue(conf.conf, 'password')

                Ds ds
                try {
                    ds = Ds.dbType(Ds.DBType.mysql).connect(host, port, 'mysql', user, password)
                } catch (Exception e) {
                    log.error('reconnect mysql error', e)
                    return false
                }
                def d = new D(ds, new MySQLDialect())

                List<String> ddlList = []

                String sqlFilePath = PluginManager.pluginsResourceDirPath() + '/n9e/initsql/a-n9e.sql'
                String sqlFilePath2 = PluginManager.pluginsResourceDirPath() + '/n9e/initsql/b-n9e.sql'

                for (str in new File(sqlFilePath).text.split(';')) {
                    ddlList << str
                }
                for (str in new File(sqlFilePath2).text.split(';')) {
                    ddlList << str
                }

                try {
                    ddlList.each {
                        def line = it.trim()
                        if (!line) {
                            return
                        }
                        Event.builder().type(Event.Type.app).reason('before init sql execute').result(conf.appId).
                                build().log(conf.nodeIp + ' - ' + line).toDto().add()
                        try {
                            d.exe(line)
                        } catch (Exception e) {
                            Event.builder().type(Event.Type.app).reason('before init sql execute error').result(conf.appId).
                                    build().log(conf.nodeIp + ' - ' + line + ' - ' + e.message).toDto().add()
                            log.error('after init sql execute error - ' + line, e)
                        }
                        log.info 'done sql <-'
                    }
                    keeper.next(JobStepKeeper.Step.yourStep, 'before init create database', 'n9e')
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
                Checker.Type.before
            }

            @Override
            String name() {
                'N9e create database'
            }

            @Override
            String imageName() {
                N9ePlugin.this.imageName()
            }
        }
    }

    @Override
    String group() {
        'key232323'
    }

    @Override
    String image() {
        'n9e'
    }

    @Override
    String version() {
        'v5'
    }
}
