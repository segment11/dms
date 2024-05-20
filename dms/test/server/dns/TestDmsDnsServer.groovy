package server.dns

import com.segment.common.Utils

def server = DmsDnsServer.instance

def listen = {
    def localIp = Utils.localIp('192.')

    Thread.start {
        // dig @192.168.1.* -p 6363 example.com
        // dig @192.168.1.* -p 6363 bing.com
        server.listen(localIp, 6363, 10, '114.114.114.114', 53) { domain ->
            byte[] localhostBytes = [127, 0, 0, 1]
            if (domain.contains('example.com')) {
                return localhostBytes
            }
            null
        }
    }
}

Thread.start {
    Thread.sleep(10 * 1000)
    server.stop()
    assert !server.isListening()
    Thread.sleep(10 * 1000)
    listen()
    Thread.sleep(10 * 1000)
    server.stop()
    assert !server.isListening()
}

listen().join()

