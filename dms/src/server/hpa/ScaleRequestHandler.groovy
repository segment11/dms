package server.hpa

import common.LimitQueue
import groovy.transform.CompileStatic
import spi.SpiSupport

import java.util.concurrent.ConcurrentHashMap

@CompileStatic
@Singleton
class ScaleRequestHandler {
    private ConcurrentHashMap<Integer, LimitQueue<ScaleRequest>> requestByAppId = new ConcurrentHashMap<>()

    private final int queueSize = 10

    private ScaleStrategy strategy = SpiSupport.createScaleStrategy()

    void add(Integer appId, ScaleRequest request) {
        LimitQueue<ScaleRequest> t

        def queue = new LimitQueue<ScaleRequest>(queueSize)
        queue << request
        def q = requestByAppId.putIfAbsent(appId, queue)
        if (q) {
            q << request
            t = q
        } else {
            t = queue
        }

        if (strategy.fire(appId, t)) {
            synchronized (this) {
                requestByAppId.remove(appId)
            }
        }
    }

}
