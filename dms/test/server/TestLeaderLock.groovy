package server

import model.DynConfigDTO
import org.segment.d.Ds

import java.util.concurrent.CountDownLatch

def ds = Ds.dbType(Ds.DBType.mysql).cacheAs('dms_server_ds')
        .connect('localhost', 3306, 'dms', 'root', 'test1234')
DynConfigDTO.addServerLeaderLockRow()

def threadNumber = 3
def countDownLatch = new CountDownLatch(threadNumber)
threadNumber.times { t ->
    Thread.start {
        def threadId = Thread.currentThread().getId()

        10.times { i ->
            if ((t == 0 && i == 2) || (t == 1 && i == 4) || (t == 2 && i == 6)) {
                Thread.sleep(4000)
            }
            def isLeaderThisThread = DynConfigDTO.acquireLock(threadId.toString(), 3)
            println "thread id: $threadId, I am the leader: $isLeaderThisThread"
            Thread.sleep(1000)
        }

        countDownLatch.countDown()
    }
    Thread.sleep(1000)
}

countDownLatch.await()
println 'all threads done'
ds.closeConnect()