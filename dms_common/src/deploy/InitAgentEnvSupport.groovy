package deploy

import com.segment.common.Conf
import common.AgentConf
import common.LimitQueue
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import model.NodeKeyPairDTO
import org.apache.commons.io.FileUtils

import java.text.SimpleDateFormat

@CompileStatic
@Slf4j
class InitAgentEnvSupport {
    private RemoteInfo info

    InitAgentEnvSupport(NodeKeyPairDTO kp) {
        this(RemoteInfo.fromKeyPair(kp))
    }

    InitAgentEnvSupport(RemoteInfo info) {
        this.info = info
        this.userHomeDir = info.user == 'root' ? '/root' : '/home/' + info.user
        this.agentTarFile = userHomeDir + '/agentV2.tar.gz'
    }

    String userHomeDir

    String agentTarFile

    String initRootPass = 'PaicDMS'

    private LimitQueue<String> steps = new LimitQueue<>(1000)

    private addStep(String step, String content, OneCmd cmd = null) {
        def now = new SimpleDateFormat('yyyy-MM-dd HH:mm:ss').format(new Date())
        steps << "${info.host} - ${step} - ${now} - ${content} - ${cmd ? cmd.toString() : ''}".toString()
    }

    List<String> getSteps() {
        steps.findAll { it.startsWith(info.host + ' - ') }
    }

    boolean resetRootPassword() {
        def passwordTips = Conf.instance.getString('sudo.shell.new.password.input.tips', 'New Password:')
        def passwordRetryTips = Conf.instance.getString('sudo.shell.retry.password.input.tips', 'Retype')

        List<OneCmd> commandList = [
                new OneCmd(cmd: 'pwd', checker: OneCmd.keyword(info.user + '@')),
                new OneCmd(cmd: 'sudo passwd root', checker: OneCmd.keyword('[sudo]', passwordTips)),
                new OneCmd(cmd: initRootPass, checker: OneCmd.keyword(passwordRetryTips), showCmdLog: false, dependOnEndMatchKeyword: { String endMatchKeyword ->
                    endMatchKeyword == '[sudo]' ? info.password : initRootPass
                }),
                new OneCmd(cmd: initRootPass, checker: OneCmd.keyword(info.user + '@', '*'), showCmdLog: false),
                new OneCmd(cmd: initRootPass, checker: OneCmd.any(), showCmdLog: false, dependOnEndMatchKeyword: { String endMatchKeyword ->
                    endMatchKeyword == (info.user + '@') ? null : initRootPass
                })
        ]

        def deploy = DeploySupport.instance
        deploy.exec(info, commandList, 10, true)
        commandList.every { it.ok() }
    }

    List<OneCmd> cmdAsRoot(OneCmd... extList) {
        def list = [new OneCmd(cmd: 'pwd', checker: OneCmd.keyword(info.user + '@'))]

        if ('root' != info.user) {
            def passwordTips = Conf.instance.getString('sudo.shell.password.input.tips', 'Password:')
            list << new OneCmd(cmd: 'su', checker: OneCmd.keyword(passwordTips))
            list << new OneCmd(cmd: info.rootPass, showCmdLog: false,
                    checker: OneCmd.keyword('root@').failKeyword('failure'))
        }
        for (one in extList) {
            list << one
        }
        list
    }

    boolean mount(String devFilePath, String dir) {
        List<OneCmd> commandList = cmdAsRoot new OneCmd(cmd: "/usr/sbin/mkfs.ext4 ${devFilePath}".toString(),
                maxWaitTimes: 300,
                checker: OneCmd.keyword('superblocks', 'Creating journal', 'Proceed anyway?')),
                new OneCmd(cmd: 'N', checker: OneCmd.any()),
                new OneCmd(cmd: "mkdir -p ${dir}".toString(),
                        checker: OneCmd.any()),
                new OneCmd(cmd: "mount ${devFilePath} ${dir}".toString(),
                        checker: OneCmd.any())

        def deploy = DeploySupport.instance
        deploy.exec(info, commandList, 1200, true)
        commandList.every { it.ok() }
    }

    boolean mkdir(String dir) {
        List<OneCmd> commandList = cmdAsRoot new OneCmd(cmd: "mkdir -p ${dir}".toString(), checker: OneCmd.any())

        def deploy = DeploySupport.instance
        deploy.exec(info, commandList, 10, true)
        commandList.every { it.ok() }
    }

    boolean copyFileIfNotExists(String localFilePath,
                                boolean isTarX = true, boolean isCreateDir = false, String destFilePathGiven = null) {
        String destFilePath = destFilePathGiven ?: localFilePath

        def deploy = DeploySupport.instance
        def one = OneCmd.simple('ls ' + destFilePath)
        deploy.exec(info, one)
        if (!one.ok()) {
            // do not use root to scp
            if (isCreateDir) {
                String mkdirCommand = 'mkdir -p ' + destFilePath.split(/\//)[0..-2].join('/')
                def mkdirCmd = OneCmd.simple(mkdirCommand)
                deploy.exec(info, mkdirCmd)
                addStep('mkdir', 'for file: ' + destFilePath + ' - ' + mkdirCmd.result)
            }

            deploy.send(info, localFilePath, destFilePath)
            addStep('copy file', 'dest: ' + destFilePath)
        } else {
            log.info 'skip scp {}', destFilePath
            addStep('skip copy file', 'dest: ' + destFilePath)
        }

        if (!isTarX) {
            return true
        }

        unTar(destFilePath)
    }

    boolean unTar(String destFilePath) {
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

        def deploy = DeploySupport.instance
        deploy.exec(info, two)
        if (!two.ok()) {
            List<OneCmd> commandList = [OneCmd.simple('mkdir -p ' + destDir),
                                        OneCmd.simple("tar ${tarOpts} ${destFilePath} -C ${destDir}".toString())]
            deploy.exec(info, commandList)
            addStep('tar x file', 'file: ' + destFilePath)
            return commandList.every { it.ok() }
        } else {
            log.info 'skip tar {}', destFilePath
            addStep('skip tar x file', 'file: ' + destFilePath)
            return true
        }
    }

    boolean pullDockerImageList(List<String> imageList) {
        if (!info.rootPass) {
            throw new DeployException('root password need init - ' + info.host)
        }

        def deploy = DeploySupport.instance
        List<OneCmd> commandList = cmdAsRoot()

        for (image in imageList) {
            // 200ms once 600 times -> 120s -> 2m
            commandList << new OneCmd(cmd: 'docker pull ' + image, maxWaitTimes: 600,
                    checker: OneCmd.keyword('Downloaded newer image', 'Image is up to date').
                            failKeyword('not found'))
        }

        def isExecOk = deploy.exec(info, commandList, 120 * imageList.size(), true)
        if (!isExecOk) {
            return false
        }
        commandList.every { it.ok() }
    }

    boolean copyAndLoadDockerImage(String imageTarGzName) {
        if (!info.rootPass) {
            throw new DeployException('root password need init - ' + info.host)
        }

        String localFilePath = userHomeDir + '/images/' + imageTarGzName

        boolean isCopyDone = copyFileIfNotExists(localFilePath, false, true)
        if (!isCopyDone) {
            return isCopyDone
        }
        boolean isLoadOk = loadDockerImage(localFilePath)
        isLoadOk
    }

    boolean loadDockerImage(String localFilePath) {
        if (!info.rootPass) {
            throw new DeployException('root password need init - ' + info.host)
        }

        def deploy = DeploySupport.instance

        String loadCmd = "gunzip -c ${localFilePath}|docker load".toString()

        List<OneCmd> commandList = cmdAsRoot new OneCmd(cmd: loadCmd, maxWaitTimes: 300, checker: OneCmd.keyword('Loaded image'))

        def isExecOk = deploy.exec(info, commandList, 600, true)
        if (!isExecOk) {
            return false
        }
        addStep('load image', '', commandList[-1])
        commandList.every { it.ok() }
    }

    void initAgentConf(AgentConf agentConf) {
        String destAgentDir = agentTarFile.replace('.tar.gz', '')

        final String tmpLocalDir = '/tmp'
        final String fileName = 'conf.properties'

        def localFilePath = tmpLocalDir + '/' + fileName
        def destFilePath = destAgentDir + '/' + fileName
        def f = new File(localFilePath)
        if (!f.exists()) {
            FileUtils.touch(f)
        }
        f.text = agentConf.generate()

        DeploySupport.instance.send(info, localFilePath, destFilePath)
        addStep('copy agent config file', 'dest: ' + destFilePath)
    }

    boolean stopAgent() {
        if (!info.rootPass) {
            throw new DeployException('root password need init - ' + info.host)
        }

        String stopCommand = "pgrep -f dms_agent|xargs kill -s 15"
        List<OneCmd> commandList = cmdAsRoot new OneCmd(cmd: stopCommand, checker: OneCmd.any())

        DeploySupport.instance.exec(info, commandList, 30, true)
        addStep('stop agent', '', commandList[-1])
        commandList.every { it.ok() }
    }

    boolean startAgentCmd() {
        if (!info.rootPass) {
            throw new DeployException('root password need init - ' + info.host)
        }

        String destAgentDir = agentTarFile.replace('.tar.gz', '')

        String javaCmd = Conf.instance.getString('agent.java.cmd',
                'java -Xms128m -Xmx256m')
        String startCommand = "nohup ${javaCmd} ".toString() +
                "-Djava.library.path=. -cp . -jar dms_agent-1.2.jar > /dev/null 2>&1 &"
        List<OneCmd> commandList = cmdAsRoot new OneCmd(cmd: 'cd ' + destAgentDir, checker: OneCmd.keyword('agentV2')),
                new OneCmd(cmd: startCommand, checker: OneCmd.any())

        DeploySupport.instance.exec(info, commandList, 30, true)
        addStep('start agent', '', commandList[-1])
        commandList.every { it.ok() }
    }

    boolean initNodeAgent() {
        def agentTarFilePath = Conf.instance.projectPath('/dms_agent/agentV2.tar.gz')
        if (!copyFileIfNotExists(agentTarFilePath, true, false, agentTarFile)) {
            return false
        }

        return true
    }
}