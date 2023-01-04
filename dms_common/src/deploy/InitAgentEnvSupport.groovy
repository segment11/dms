package deploy

import common.AgentConf
import common.Conf
import common.LimitQueue
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import model.NodeKeyPairDTO
import org.apache.commons.io.FileUtils

@CompileStatic
@Slf4j
class InitAgentEnvSupport {

    // debian 11 default admin user
    final static String BASE_DIR = '/home/admin'

    final static String DOCKER_RUNTIME_FILE = BASE_DIR + '/docker.tar'

    final static String JDK_FILE = BASE_DIR + '/jdk8.tar.gz'

    final static String AGENT_FILE = BASE_DIR + '/agentV1.tar.gz'

    Integer clusterId

    String proxyNodeIp

    final static String INIT_ROOT_PASS = 'Paic1234'

    private LimitQueue<String> steps = new LimitQueue<>(1000)

    private addStep(NodeKeyPairDTO kp, String step, String content, OneCmd cmd = null) {
        def now = new Date().format('yyyy-MM-dd HH:mm:ss')
        steps << "${kp.ip} - ${step} - ${now} - ${content} - ${cmd ? cmd.toString() : ''}".toString()
    }

    List<String> getSteps(NodeKeyPairDTO kp) {
        steps.findAll { it.startsWith(kp.ip + ' - ') }
    }

    InitAgentEnvSupport(Integer clusterId, String proxyNodeIp = null) {
        this.clusterId = clusterId
        this.proxyNodeIp = proxyNodeIp
    }

    boolean resetRootPassword(NodeKeyPairDTO kp) {
        List<OneCmd> commandList = [
                new OneCmd(cmd: 'pwd', checker: OneCmd.keyword(kp.user + '@')),
                new OneCmd(cmd: 'sudo passwd root', checker: OneCmd.keyword('New password:')),
                new OneCmd(cmd: INIT_ROOT_PASS, checker: OneCmd.keyword('Retype'), showCmdLog: false),
                new OneCmd(cmd: INIT_ROOT_PASS, checker: OneCmd.any(), showCmdLog: false)
        ]

        def deploy = DeploySupport.instance
        deploy.exec(kp, commandList, 10, true)
        commandList.every { it.ok() }
    }

    boolean mount(NodeKeyPairDTO kp, String devFilePath, String dir) {
        List<OneCmd> commandList = [
                new OneCmd(cmd: 'pwd', checker: OneCmd.keyword(kp.user + '@')),
                new OneCmd(cmd: 'su', checker: OneCmd.keyword('Password:')),
                new OneCmd(cmd: kp.rootPass, showCmdLog: false,
                        checker: OneCmd.keyword('root@').failKeyword('failure')),
                new OneCmd(cmd: "/usr/sbin/mkfs.ext3 ${devFilePath}".toString(),
                        maxWaitTimes: 300,
                        checker: OneCmd.keyword('superblocks', 'Creating journal', 'Proceed anyway?')),
                new OneCmd(cmd: 'N', checker: OneCmd.any()),
                new OneCmd(cmd: "mkdir -p ${dir}".toString(),
                        checker: OneCmd.any()),
                new OneCmd(cmd: "mount ${devFilePath} ${dir}".toString(),
                        checker: OneCmd.any()),
        ]

        def deploy = DeploySupport.instance
        deploy.exec(kp, commandList, 1200, true)
        commandList.every { it.ok() }
    }

    boolean mkdir(NodeKeyPairDTO kp, String dir) {
        List<OneCmd> commandList = [
                new OneCmd(cmd: 'pwd', checker: OneCmd.keyword(kp.user + '@')),
                new OneCmd(cmd: 'su', checker: OneCmd.keyword('Password:')),
                new OneCmd(cmd: kp.rootPass, showCmdLog: false,
                        checker: OneCmd.keyword('root@').failKeyword('failure')),
                new OneCmd(cmd: "mkdir -p ${dir}".toString(),
                        checker: OneCmd.any())
        ]

        def deploy = DeploySupport.instance
        deploy.exec(kp, commandList, 10, true)
        commandList.every { it.ok() }
    }

    boolean copyFileIfNotExists(NodeKeyPairDTO kp, String localFilePath,
                                boolean isTarX = true, boolean isCreateDir = false) {
        String destFilePath = localFilePath

        def deploy = DeploySupport.instance
        def one = OneCmd.simple('ls ' + destFilePath)
        deploy.exec(kp, one)
        if (!one.ok()) {
            // do not use root to scp
            if (isCreateDir) {
                String mkdirCommand = 'mkdir -p ' + destFilePath.split(/\//)[0..-2].join('/')
                def mkdirCmd = OneCmd.simple(mkdirCommand)
                deploy.exec(kp, mkdirCmd)
                addStep(kp, 'mkdir', 'for file: ' + destFilePath + ' - ' + mkdirCmd.result)
            }

            deploy.send(kp, localFilePath, destFilePath)
            addStep(kp, 'copy file', 'dest: ' + destFilePath)
        } else {
            log.info 'skip scp {}', destFilePath
            addStep(kp, 'skip copy file', 'dest: ' + destFilePath)
        }

        if (!isTarX) {
            return true
        }

        String suf
        String tarOpts
        if (destFilePath.endsWith('.tar')) {
            suf = '.tar'
            tarOpts = '-xvf'
        } else {
            suf = '.tar.gz'
            tarOpts = '-zxvf'
        }

        def destDir = destFilePath.replace(suf, '')

        // tar
        def two = OneCmd.simple('ls ' + destDir)
        deploy.exec(kp, two)
        if (!two.ok()) {
            List<OneCmd> commandList = [OneCmd.simple('mkdir -p ' + destDir),
                                        OneCmd.simple("tar ${tarOpts} ${destFilePath} -C ${destDir}".toString())]
            deploy.exec(kp, commandList)
            addStep(kp, 'tar x file', 'file: ' + destFilePath)
            return commandList.every { it.ok() }
        } else {
            log.info 'skip tar {}', destFilePath
            addStep(kp, 'skip tar x file', 'file: ' + destFilePath)
            return true
        }
    }

    boolean initDockerDaemon(NodeKeyPairDTO kp) {
        if (!kp.rootPass) {
            throw new DeployException('root password need init - ' + kp.ip)
        }

        def deploy = DeploySupport.instance

        List<OneCmd> cmdList = [
                new OneCmd(cmd: 'pwd', checker: OneCmd.keyword(kp.user + '@')),
                new OneCmd(cmd: 'su', checker: OneCmd.keyword('Password:')),
                new OneCmd(cmd: kp.rootPass, showCmdLog: false,
                        checker: OneCmd.keyword('root@').failKeyword('failure')),
                new OneCmd(cmd: 'docker ps',
                        checker: OneCmd.keyword('CONTAINER ID').failKeyword('Cannot connect', 'command not'))
        ]
        deploy.exec(kp, cmdList, 20, true)
        if (cmdList.every { it.ok() }) {
            log.info 'skip init docker engine'
            addStep(kp, 'skip init docker engine', 'already done')
            return true
        }
        /*
Get:1 http://mirrors.ustc.edu.cn/debian bullseye/main amd64 pigz amd64 2.6-1 [64.0 kB]
Get:2 http://mirrors.ustc.edu.cn/debian bullseye/main amd64 libip6tc2 amd64 1.8.7-1 [35.0 kB]
Get:3 /home/admin/docker/docker-ce_20.10.21_3-0_debian-bullseye_amd64.deb docker-ce amd64 5:20.10.21~3-0~debian-bullseye [20.4 MB]
Get:4 http://mirrors.ustc.edu.cn/debian bullseye/main amd64 libnfnetlink0 amd64 1.0.1-3+b1 [13.9 kB]
Get:5 http://mirrors.ustc.edu.cn/debian bullseye/main amd64 libnetfilter-conntrack3 amd64 1.0.8-3 [40.6 kB]
Get:6 http://mirrors.ustc.edu.cn/debian bullseye/main amd64 iptables amd64 1.8.7-1 [382 kB]
Get:7 http://mirrors.ustc.edu.cn/debian bullseye/main amd64 liberror-perl all 0.17029-1 [31.0 kB]
Get:8 http://mirrors.ustc.edu.cn/debian bullseye/main amd64 git-man all 1:2.30.2-1 [1827 kB]
Get:9 http://mirrors.ustc.edu.cn/debian bullseye/main amd64 git amd64 1:2.30.2-1 [5527 kB]
Get:10 http://mirrors.ustc.edu.cn/debian bullseye/main amd64 libltdl7 amd64 2.4.6-15 [391 kB]
Get:11 http://mirrors.ustc.edu.cn/debian bullseye/main amd64 patch amd64 2.7.6-7 [128 kB]
         */

        String destDockerDir = DOCKER_RUNTIME_FILE.replace('.tar', '')
        def engineInstallCmd = "apt install -y ${destDockerDir}/docker-ce_20.10.21_3-0_debian-bullseye_amd64.deb".toString()

        List<OneCmd> finalCmdList = [
                new OneCmd(cmd: 'pwd', checker: OneCmd.keyword(kp.user + '@')),
                new OneCmd(cmd: 'su', checker: OneCmd.keyword('Password:')),
                new OneCmd(cmd: kp.rootPass, showCmdLog: false,
                        checker: OneCmd.keyword('root@').failKeyword('failure')),
                new OneCmd(cmd: engineInstallCmd, maxWaitTimes: 600,
                        checker: OneCmd.keyword('0 newly installed', 'Processing triggers', ':/home/admin')),
                new OneCmd(cmd: 'systemctl enable docker.service', checker: OneCmd.any())
        ]
        deploy.exec(kp, finalCmdList, 1200, true)
        addStep(kp, 'init docker engine', '', finalCmdList[-1])
        finalCmdList.every { it.ok() }
    }

    boolean initDockerClient(NodeKeyPairDTO kp) {
        if (!kp.rootPass) {
            throw new DeployException('root password need init - ' + kp.ip)
        }

        def deploy = DeploySupport.instance
        def one = OneCmd.simple('docker -v')
        deploy.exec(kp, one)
        if (one.result?.contains('Docker version')) {
            log.info 'skip init docker client'
            addStep(kp, 'skip init docker client', 'already done')
            return true
        }

        String destDockerDir = DOCKER_RUNTIME_FILE.replace('.tar', '')
        def containerdInstallCmd = "apt install -y ${destDockerDir}/containerd.io_1.6.10-1_amd64.deb".toString()
        def clientInstallCmd = "apt install -y ${destDockerDir}/docker-ce-cli_20.10.21_3-0_debian-bullseye_amd64.deb".toString()

        List<OneCmd> cmdList = [
                new OneCmd(cmd: 'pwd', checker: OneCmd.keyword(kp.user + '@')),
                new OneCmd(cmd: 'su', checker: OneCmd.keyword('Password:')),
                new OneCmd(cmd: kp.rootPass, showCmdLog: false,
                        checker: OneCmd.keyword('root@').failKeyword('failure')),
                // 200ms once 300 times -> 60s -> 1m
                new OneCmd(cmd: containerdInstallCmd, maxWaitTimes: 600,
                        checker: OneCmd.keyword('0 newly installed', 'Processing triggers', ':/home/admin')),
                new OneCmd(cmd: clientInstallCmd, maxWaitTimes: 600,
                        checker: OneCmd.keyword('0 newly installed', 'Processing triggers', ':/home/admin'))
        ]
        def isExecOk = deploy.exec(kp, cmdList, 1200, true)
        if (!isExecOk) {
            return false
        }
        addStep(kp, 'init docker client', '', cmdList[-1])
        cmdList.every { it.ok() }
    }

    boolean copyAndLoadDockerImage(NodeKeyPairDTO kp, String imageTarGzName) {
        if (!kp.rootPass) {
            throw new DeployException('root password need init - ' + kp.ip)
        }

        String localFilePath = InitAgentEnvSupport.BASE_DIR + '/images/' + imageTarGzName

        boolean isCopyDone = copyFileIfNotExists(kp, localFilePath, false, true)
        if (!isCopyDone) {
            return isCopyDone
        }
        boolean isLoadOk = loadDockerImage(kp, localFilePath)
        isLoadOk
    }

    boolean loadDockerImage(NodeKeyPairDTO kp, String localFilePath) {
        if (!kp.rootPass) {
            throw new DeployException('root password need init - ' + kp.ip)
        }

        def deploy = DeploySupport.instance

        String loadCmd = "gunzip -c ${localFilePath}|docker load".toString()

        List<OneCmd> cmdList = [
                new OneCmd(cmd: 'pwd', checker: OneCmd.keyword(kp.user + '@')),
                new OneCmd(cmd: 'su', checker: OneCmd.keyword('Password:')),
                new OneCmd(cmd: kp.rootPass, showCmdLog: false,
                        checker: OneCmd.keyword('root@').failKeyword('failure')),
                new OneCmd(cmd: loadCmd, maxWaitTimes: 300,
                        checker: OneCmd.keyword('Loaded image'))
        ]
        def isExecOk = deploy.exec(kp, cmdList, 600, true)
        if (!isExecOk) {
            return false
        }
        addStep(kp, 'load image', '', cmdList[-1])
        cmdList.every { it.ok() }
    }

    void initAgentConf(NodeKeyPairDTO kp, AgentConf agentConf) {
        String destAgentDir = AGENT_FILE.replace('.tar.gz', '')

        final String tmpLocalDir = '/tmp'
        final String fileName = 'conf.properties'

        def localFilePath = tmpLocalDir + '/' + fileName
        def destFilePath = destAgentDir + '/' + fileName
        def f = new File(localFilePath)
        if (!f.exists()) {
            FileUtils.touch(f)
        }
        f.text = agentConf.generate()

        DeploySupport.instance.send(kp, localFilePath, destFilePath)
        addStep(kp, 'copy agent config file', 'dest: ' + destFilePath)
    }

    boolean stopAgent(NodeKeyPairDTO kp) {
        if (!kp.rootPass) {
            throw new DeployException('root password need init - ' + kp.ip)
        }

        String stopCommand = "pgrep -f dms_agent|xargs kill -s 15"
        List<OneCmd> cmdList = [
                new OneCmd(cmd: 'pwd', checker: OneCmd.keyword(kp.user + '@')),
                new OneCmd(cmd: 'su', checker: OneCmd.keyword('Password:')),
                new OneCmd(cmd: kp.rootPass, showCmdLog: false,
                        checker: OneCmd.keyword('root@').failKeyword('failure')),
                new OneCmd(cmd: stopCommand, checker: OneCmd.any())
        ]
        DeploySupport.instance.exec(kp, cmdList, 30, true)
        addStep(kp, 'stop agent', '', cmdList[-1])
        cmdList.every { it.ok() }
    }

    boolean startAgentCmd(NodeKeyPairDTO kp) {
        if (!kp.rootPass) {
            throw new DeployException('root password need init - ' + kp.ip)
        }

        String destAgentDir = AGENT_FILE.replace('.tar.gz', '')

        String javaCmd = Conf.instance.getString('agent.javaCmd',
                '../jdk8/zulu8.66.0.15-ca-jdk8.0.352-linux_x64/bin/java -Xms128m -Xmx256m')
        String startCommand = "nohup ${javaCmd} ".toString() +
                "-Djava.library.path=. -cp . -jar dms_agent-1.0.jar > dmc.log 2>&1 &"
        List<OneCmd> cmdList = [
                new OneCmd(cmd: 'pwd', checker: OneCmd.keyword(kp.user + '@')),
                new OneCmd(cmd: 'su', checker: OneCmd.keyword('Password:')),
                new OneCmd(cmd: kp.rootPass, showCmdLog: false,
                        checker: OneCmd.keyword('root@').failKeyword('failure')),
                new OneCmd(cmd: 'cd ' + destAgentDir, checker: OneCmd.keyword('agentV1')),
                new OneCmd(cmd: startCommand, checker: OneCmd.any())
        ]
        DeploySupport.instance.exec(kp, cmdList, 30, true)
        addStep(kp, 'start agent', '', cmdList[-1])
        cmdList.every { it.ok() }
    }

    boolean initOtherNode(NodeKeyPairDTO kp) {
        if (!copyFileIfNotExists(kp, DOCKER_RUNTIME_FILE)) {
            return false
        }
        if (!initDockerClient(kp)) {
            return false
        }
        if (!initDockerDaemon(kp)) {
            return false
        }

        if (!copyFileIfNotExists(kp, JDK_FILE)) {
            return false
        }

        if (!copyFileIfNotExists(kp, AGENT_FILE)) {
            return false
        }

        return true
    }
}
