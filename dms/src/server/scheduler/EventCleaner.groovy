package server.scheduler

import com.segment.common.Conf
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import model.AppJobDTO
import model.AppJobLogDTO
import model.EventDTO

@CompileStatic
@Singleton
@Slf4j
class EventCleaner {
    void clearOld(long intervalCount, long interval) {
        def now = new Date()
        if (!(now.hours == 23 && now.minutes == 59 && now.seconds >= (60 - interval))) {
            if (!Conf.isWindows()) {
                return
            }
            if (intervalCount % 10 != 0) {
                return
            }
        }
        int dayAfter = Conf.instance.getInt('guardian.clearOldEventLogDayAfter', 7)
        def oldDay = new Date(now.time - dayAfter * 24 * 3600 * 1000)

        try {
            def one = new EventDTO()
            def num = one.useD().exeUpdate('delete from ' + one.tbl() + ' where created_date < ?', [oldDay])
            log.info 'done delete old event log - {}', num

            def one2 = new AppJobLogDTO()
            def num2 = one.useD().exeUpdate('delete from ' + one2.tbl() + ' where created_date < ?', [oldDay])
            log.info 'done delete old job log - {}', num2

            def one3 = new AppJobDTO()
            def num3 = one.useD().exeUpdate('delete from ' + one3.tbl() + ' where created_date < ?', [oldDay])
            log.info 'done delete old job - {}', num3
        } catch (Exception e) {
            log.error 'clear old event log error', e
        }
    }
}
