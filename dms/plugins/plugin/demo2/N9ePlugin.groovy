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

        [17000].each {
            def one = new ImagePortDTO(imageName: imageName, port: it).one()
            if (!one) {
                new ImagePortDTO(imageName: imageName, name: it.toString(), port: it).add()
            }
        }

        final String tplName = 'config.toml.tpl'

        String tplFilePath = PluginManager.pluginsResourceDirPath() + '/n9e/ConfigTomlTpl.groovy'
        String content = new File(tplFilePath).text

        TplParamsConf tplParams = new TplParamsConf()
        tplParams.addParam('host', '192.168.1.100', 'string')
        tplParams.addParam('port', '3306', 'int')
        tplParams.addParam('user', 'root', 'string')
        tplParams.addParam('password', 'root1234', 'string')
        tplParams.addParam('signingKey', '5b94a0fd640fe2765af826acfe42d151', 'string')

        def one = new ImageTplDTO(imageName: imageName, name: tplName).queryFields('id').one()
        if (!one) {
            new ImageTplDTO(
                    name: tplName,
                    imageName: imageName,
                    tplType: ImageTplDTO.TplType.mount.name(),
                    mountDist: '/app/etc/config.toml',
                    content: content,
                    isParentDirMount: false,
                    params: tplParams
            ).add()
        }
    }

    String getParamValue(AppConf conf, String key) {
        getParamValueFromTpl(conf, '/app/etc/config.toml', key)
    }

    private void initChecker() {
        CheckerHolder.instance.add new Checker() {
            @Override
            boolean check(CreateContainerConf conf, JobStepKeeper keeper) {
                def host = getParamValue(conf.conf, 'host')
                int port = getParamValue(conf.conf, 'port') as int
                def user = getParamValue(conf.conf, 'user')
                def password = getParamValue(conf.conf, 'password')

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
        'v6'
    }
}
