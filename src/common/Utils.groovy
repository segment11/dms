package common

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import org.apache.commons.net.telnet.TelnetClient

import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.attribute.PosixFilePermission

@CompileStatic
@Slf4j
class Utils {
    static void stopWhenConsoleQuit(Closure<Void> closure, InputStream is = null) {
        boolean isStopped = false
        Runtime.addShutdownHook {
            if (!isStopped) {
                closure.call()
            }
        }

        if (Conf.isWindows()) {
            Thread.start {
                def br = new BufferedReader(new InputStreamReader(is ?: System.in))
                while (true) {
                    if (br.readLine() == 'quit') {
                        println 'quit from console...'
                        closure.call()
                        isStopped = true
                        break
                    }
                }
            }
        }
    }

    static String localIp() {
        InetAddress.localHost.hostAddress
    }

    static String uuid(String pre = '', int len = 5) {
        def rand = new Random()
        List az = 0..9
        int size = az.size()
        def sb = new StringBuilder()
        for (int i = 0; i < len; i++) {
            sb << az[rand.nextInt(size)]
        }
        sb.toString()
    }

    static Date getNodeAliveCheckLastDate(int heartBeatLossTimes = 3) {
        int heartBeatIntervalMillis = Conf.instance.getInt('heartBeatIntervalMillis', 1000 * 10)
        def dat = new Date()
        dat.time = dat.time - heartBeatLossTimes * heartBeatIntervalMillis
        dat
    }

    static boolean isPortListenAvailable(int port) {
        def tc = new TelnetClient(connectTimeout: 500)
        try {
            tc.connect('localhost', port)
            return false
        } catch (Exception e) {
            return true
        } finally {
            tc.disconnect()
        }
    }

    static int compareIp(String ip1, String ip2) {
        def arr1 = ip1.split(/\./)
        def arr2 = ip2.split(/\./)
        for (i in (0..<4)) {
            int diff = (arr1[i] as int) - (arr2[i] as int)
            if (diff != 0) {
                return diff < 0 ? -1 : 1
            }
        }
        0
    }

    private static int BEGIN_PORT = 31000

    synchronized static int getOnePortListenAvailable() {
        for (i in (0..<100)) {
            def j = BEGIN_PORT + i
            if (j >= 51000) {
                j = 31000
            }

            if (isPortListenAvailable(j)) {
                BEGIN_PORT = j + 1
                return j
            }
        }
        -1
    }

    static void setFilePermission(File f, PosixFilePermission... permissions) {
        if (Conf.isWindows()) {
            return
        }
        Set<PosixFilePermission> set = []
        for (permission in permissions) {
            set << permission
        }
        try {
            Files.setPosixFilePermissions(Paths.get(f.absolutePath), set)
        } catch (Exception e) {
            log.error('set read permit to file error - ' + f.absolutePath, e)
        }
    }

    static void setFileRead(File f) {
        setFilePermission(f, PosixFilePermission.OWNER_READ, PosixFilePermission.GROUP_READ,
                PosixFilePermission.OTHERS_READ)
    }

    static void setFile600(File f) {
        setFilePermission(f, PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE)
    }
}
