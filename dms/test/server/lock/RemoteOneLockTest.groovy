package server.lock

import org.apache.curator.framework.CuratorFrameworkFactory
import org.apache.curator.retry.ExponentialBackoffRetry
import spock.lang.Specification

import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.CountDownLatch

class RemoteOneLockTest extends Specification {
    def "Exe"() {
        given:
        def client = CuratorFrameworkFactory.newClient('192.168.111.112:2181',
                new ExponentialBackoffRetry(1000, 3))
        client.start()
        CuratorFrameworkClientHolder.instance.client = client
        and:
        int i = 10
        CopyOnWriteArrayList<Boolean> list = []
        def latch = new CountDownLatch(i)
        i.times {
            Thread.start {
                def lock = new RemoteOneLock()
                lock.acquireTrySeconds = 1
                lock.lockKey = '/test/lock'
                list << lock.exe {
                    Thread.sleep(500)
                    println 'exe done, my thread name: ' + Thread.currentThread().name
                }
                latch.countDown()
            }
        }
        latch.await()
        expect:
        list.size() == i
        list.findAll { it }.size() == 2
        list.each {
            println it
        }
        cleanup:
        client.close()
    }
}
