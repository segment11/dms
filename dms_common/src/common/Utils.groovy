package common

import com.segment.common.Conf
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import org.apache.commons.io.IOUtils
import org.apache.commons.net.telnet.TelnetClient

import java.nio.charset.Charset
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.attribute.PosixFilePermission

@CompileStatic
@Slf4j
class Utils {
    static Date getNodeAliveCheckLastDate(int heartBeatLossTimes = 3) {
        int heartBeatIntervalMillis = Conf.instance.getInt('server.heartBeatIntervalMillis', 1000 * 10)
        def dat = new Date()
        dat.time = dat.time - heartBeatLossTimes * heartBeatIntervalMillis
        dat
    }

    static boolean isPortListenAvailable(int port, String host = '127.0.0.1') {
        def tc = new TelnetClient(connectTimeout: 500)
        try {
            tc.connect(host, port)
            return false
        } catch (Exception e) {
            return true
        } finally {
            tc.disconnect()
        }
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

    static String readFully(InputStream is) {
        def sw = new StringWriter()
        IOUtils.copy(is, sw, Charset.defaultCharset())
        sw.toString()
    }

    static List<Integer> cpusetCpusToList(String cpusetCpus) {
        List<Integer> list = []
        for (s in cpusetCpus.split(/,/)) {
            if (s.contains('-')) {
                def arr = s.split(/-/)
                for (str in arr) {
                    list << (str as int)
                }
            } else {
                list << (s as int)
            }
        }
        list
    }
}
