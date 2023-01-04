package server.hpa

import common.LimitQueue
import groovy.transform.CompileStatic

@CompileStatic
interface ScaleStrategy {
    boolean fire(Integer appId, LimitQueue<ScaleRequest> queue)
}
