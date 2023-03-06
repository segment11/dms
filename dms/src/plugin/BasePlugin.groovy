package plugin

import common.Conf
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import model.ImageEnvDTO
import model.ImagePortDTO
import model.ImageRegistryDTO
import model.NodeVolumeDTO
import org.segment.d.D

@CompileStatic
@Slf4j
abstract class BasePlugin implements Plugin {
    private D d

    void setD(D d) {
        this.d = d
    }

    protected void addEnvIfNotExists(String name, String envName) {
        def imageName = group() + '/' + image()
        def one = new ImageEnvDTO(imageName: imageName, env: envName).one()
        if (!one) {
            new ImageEnvDTO(imageName: imageName, name: name, env: envName).add()
        }
    }

    protected void addPortIfNotExists(String name, int port) {
        def imageName = group() + '/' + image()
        def one = new ImagePortDTO(imageName: imageName, port: port).one()
        if (!one) {
            new ImagePortDTO(imageName: imageName, name: name, port: port).add()
        }
    }

    protected void addNodeVolumeForUpdate(String name, String dir) {
        def imageName = group() + '/' + image()
        def one = new NodeVolumeDTO(imageName: imageName, dir: dir).one()
        if (!one) {
            new NodeVolumeDTO(imageName: imageName, name: name, dir: dir, clusterId: 1).add()
        }
    }

    @Override
    void init() {
        log.info 'init plugin - {}', name()

        def registryUrl = registry()
        def one = new ImageRegistryDTO(url: registryUrl).one()
        if (!one) {
            new ImageRegistryDTO(name: registryUrl, url: registryUrl).add()
        }
    }

    @Override
    void destroy() {
        log.info 'destroy plugin - {}', name()
    }

    @Override
    String name() {
        this.getClass().simpleName
    }

    @Override
    String version() {
        '1.0.0'
    }

    @Override
    String registry() {
        'https://registry.hub.docker.io'
    }

    @Override
    String group() {
        'library'
    }

    @Override
    String image() {
        throw new RuntimeException('not implement')
    }

    @Override
    String tag() {
        'latest'
    }

    @Override
    Map<String, String> expressions() {
        HashMap<String, String> r = [:]

        def c = Conf.instance
        def dirPath = c.projectPath("/src/script/tpl/plugins/${name().replaceAll(' ', '_')}/expression".toString())
        def dir = new File(dirPath)
        if (dir.exists()) {
            for (file in dir.listFiles()) {
                r[file.name] = file.text
            }
        }
        r
    }

    @Override
    boolean isRunning() {
        false
    }
}
