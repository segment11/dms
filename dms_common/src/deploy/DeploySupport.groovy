package deploy

import com.jcraft.jsch.ChannelSftp
import com.jcraft.jsch.JSch
import com.jcraft.jsch.Session
import com.segment.common.Conf
import common.Event
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import model.NodeKeyPairDTO
import org.apache.commons.io.FileUtils
import org.jetbrains.annotations.VisibleForTesting

@CompileStatic
@Slf4j
@Singleton
class DeploySupport {
    EventHandler eventHandler = new EventHandler() {
        @Override
        void handle(Event event) {
            // server side
            event.toDto().add()
        }
    }

    boolean isLogEvent = true

    boolean isAgent = false

    static final String USER_HOME_DIR = System.getProperty('user.home').replaceAll("\\\\", '/')
    static final String CURRENT_USER = USER_HOME_DIR.contains('root') ? 'root' : USER_HOME_DIR.split('/')[2]

    private static String keyName(String ip) {
        'dms_auto_' + ip.replaceAll(/\./, '_')
    }

    void initPrivateKey(NodeKeyPairDTO kp) {
        if (!kp.ip || !kp.pass) {
            throw new IllegalArgumentException('ip/ssh port/root pass required!')
        }
        kp.keyName = keyName(kp.ip)

        def locationPublicKey = USER_HOME_DIR + '/.ssh/' + kp.keyName + '.pub'
        def filePublicKey = new File(locationPublicKey)
        if (!filePublicKey.exists()) {
            FileUtils.touch(filePublicKey)
        }

        def one = new RsaKeyPairGenerator().generate()

        kp.keyType = RsaKeyPairGenerator.KEY_TYPE_RSA
        kp.keyPrivate = one.privateKeyBase64
        kp.keyPublic = one.publicKeyBase64
        kp.updatedDate = new Date()

        filePublicKey.text = one.publicKeyBase64
        log.info 'done create public key local file {}', locationPublicKey

        def privateKeyFileLocation = USER_HOME_DIR + '/.ssh/' + kp.keyName + '.' + RsaKeyPairGenerator.KEY_TYPE_RSA
        def filePrivateKey = new File(privateKeyFileLocation)
        if (!filePrivateKey.exists()) {
            FileUtils.touch(filePrivateKey)
        }
        filePrivateKey.text = one.privateKeyBase64
        log.info 'done create private key local file {}', filePrivateKey

        def remoteInfo = new RemoteInfo(host: kp.ip, port: kp.sshPort, user: kp.userName, password: kp.pass)
        def remoteFilePath = 'root' == kp.userName ? '/root/.ssh/' + kp.keyName + '.pub' :
                '/home/' + kp.userName + '/.ssh/' + kp.keyName + '.pub'

        def mkdirCommand = 'mkdir ' + remoteFilePath.split(/\//)[0..-2].join('/')
        exec(remoteInfo, OneCmd.simple(mkdirCommand))

        send(remoteInfo, locationPublicKey, remoteFilePath)

        def command = "cat ~/.ssh/${kp.keyName}.pub >> ~/.ssh/authorized_keys".toString()
        exec(remoteInfo, OneCmd.simple(command))
    }

    void send(NodeKeyPairDTO kp, String localFilePath, String remoteFilePath) {
        send(RemoteInfo.fromKeyPair(kp), localFilePath, remoteFilePath)
    }

    @VisibleForTesting
    static Session connect(RemoteInfo remoteInfo) {
        def connectTimeoutMillis = Conf.instance.getInt('ssh.sessionConnectTimeoutMillis', 2000)
        final Properties config = new Properties()
        [
                StrictHostKeyChecking   : 'no',
                PreferredAuthentications: 'publickey,gssapi-with-mic,keyboard-interactive,password'
        ].each { k, v ->
            config[k] = v
        }

        def jsch = new JSch()
        def session = jsch.getSession(remoteInfo.user, remoteInfo.host, remoteInfo.port)
        session.timeout = connectTimeoutMillis

        if (remoteInfo.isUsePass) {
            session.setPassword(remoteInfo.password)
        } else {
            def privateKeyFileLocation = USER_HOME_DIR + '/.ssh/' + keyName(remoteInfo.host) + remoteInfo.privateKeySuffix
            def filePrivateKey = new File(privateKeyFileLocation)
            if (!filePrivateKey.exists()) {
                FileUtils.touch(filePrivateKey)
                filePrivateKey.text = remoteInfo.privateKeyContent
                log.info 'done create private key local file {}', filePrivateKey
            }

            jsch.addIdentity(privateKeyFileLocation)
        }
        session.config = config
        session.connect()
        log.info 'jsch session connected {}', remoteInfo.host
        session
    }

    void send(RemoteInfo remoteInfo, String localFilePath, String remoteFilePath) {
        def f = new File(localFilePath)
        if (!f.exists() || !f.canRead()) {
            throw new IllegalStateException('local file can not read: ' + localFilePath)
        }

        Session session
        try {
            session = connect(remoteInfo)

            ChannelSftp channel
            try {
                channel = session.openChannel('sftp') as ChannelSftp
                channel.connect()
                log.info 'sftp channel connected {}', remoteInfo.host

                long beginT = System.currentTimeMillis()
                channel.put(f.absolutePath, remoteFilePath,
                        new FilePutProgressMonitor(f.length()), ChannelSftp.OVERWRITE)

                def costT = System.currentTimeMillis() - beginT
                def message = "scp cost ${costT}ms to ${remoteFilePath}".toString()
                if (eventHandler) {
                    def event = Event.builder().type(Event.Type.cluster).reason('scp').
                            result(remoteInfo.host).build().
                            log(message)
                    eventHandler.handle(event)
                } else {
                    log.info message
                }
            } finally {
                if (channel) {
                    channel.quit()
                    channel.disconnect()
                }
            }
        } finally {
            if (session) {
                session.disconnect()
            }
        }
    }

    boolean exec(NodeKeyPairDTO kp, OneCmd command, long timeoutSeconds = 10) {
        exec(RemoteInfo.fromKeyPair(kp), command, timeoutSeconds)
    }

    boolean exec(RemoteInfo remoteInfo, OneCmd command, long timeoutSeconds = 10) {
        exec(remoteInfo, [command], timeoutSeconds, false)
    }

    boolean exec(NodeKeyPairDTO kp, List<OneCmd> cmdList,
                 long timeoutSeconds = 10, boolean isShell = false) {
        exec(RemoteInfo.fromKeyPair(kp), cmdList, timeoutSeconds, isShell)
    }

    boolean exec(RemoteInfo remoteInfo, List<OneCmd> cmdList,
                 long timeoutSeconds = 10, boolean isShell = false) {
        for (one in cmdList) {
            // if user not set maxWaitTimes, use avg
            if (one.maxWaitTimes == OneCmd.DEFAULT_MAX_WAIT_TIMES) {
                one.maxWaitTimes = (timeoutSeconds * 1000 / cmdList.size() / one.waitMsOnce).intValue()
            }
        }

        Session session
        try {
            session = connect(remoteInfo)

            def exec = new CmdExecutor()
            exec.host = remoteInfo.host
            exec.session = session
            exec.cmdList = cmdList
            exec.eventHandler = eventHandler
            if (isShell) {
                return exec.execShell()
            } else {
                return exec.exec()
            }
        } finally {
            if (session) {
                session.disconnect()
            }
        }
    }

}
