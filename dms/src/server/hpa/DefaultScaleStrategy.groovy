package server.hpa

import com.segment.common.Conf
import common.*
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import model.AppDTO
import model.json.MonitorConf

@CompileStatic
@Slf4j
class DefaultScaleStrategy implements ScaleStrategy {
    private Map<Integer, Long> lastScaleTriggerTime = [:]

    @Override
    boolean fire(Integer appId, LimitQueue<ScaleRequest> queue) {
        log.info 'scale - {}', appId
        queue.each {
            log.info it.toString()
        }

        int scaleOutVal = queue.count { it.scaleCmd == MonitorConf.SCALE_OUT }.intValue()
        int scaleInVal = queue.count { it.scaleCmd == MonitorConf.SCALE_IN }.intValue()
        log.info 'scale out/in - {}/{}', scaleOutVal, scaleInVal
        if (scaleOutVal == scaleInVal) {
            return false
        }

        def t = lastScaleTriggerTime[appId]
        def scaleIntervalMs = Conf.instance.getInt('trigger.scaleIntervalMs', 1000 * 60)
        if (t != null && (System.currentTimeMillis() - t) < scaleIntervalMs) {
            log.warn 'wait for a while as already scale before - ' + scaleIntervalMs + 'Ms'
            return false
        }

        def app = new AppDTO(id: appId).one()
        def min = app.monitorConf.scaleMin
        def max = app.monitorConf.scaleMax
        if (min == max) {
            Event.builder().type(Event.Type.app).reason('scale limited').
                    result(appId).build().log('min/max - ' + min).toDto().add()
            return false
        }

        def oldNum = app.conf.containerNumber
        def newNum = scaleOutVal > scaleInVal ? oldNum + 1 : oldNum - 1
        if (newNum < min || newNum > max) {
            Event.builder().type(Event.Type.app).reason('scale limited').
                    result(appId).build().log('min/max/newNum - ' + min + '/' + max + '/' + newNum).toDto().add()
            return false
        }

        app.conf.containerNumber = newNum
        new AppDTO(id: appId, conf: app.conf, updatedDate: new Date()).update()
        Event.builder().type(Event.Type.app).reason('scale done').
                result(appId).build().log('oldNum/newNum - ' + oldNum + '/' + newNum).toDto().add()

        lastScaleTriggerTime[appId] = System.currentTimeMillis()
        return false
    }
}
