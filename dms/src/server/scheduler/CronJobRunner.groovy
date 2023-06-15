package server.scheduler

import common.Event
import common.Utils
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import it.sauronsoftware.cron4j.Scheduler
import model.AppDTO
import org.segment.d.json.DefaultJsonTransformer
import org.segment.d.json.JsonTransformer

@CompileStatic
@Singleton
@Slf4j
class CronJobRunner {
    private Scheduler scheduler

    private Map<Integer, String> lastSaved = [:]

    private JsonTransformer json = new DefaultJsonTransformer()

    synchronized void stop() {
        if (scheduler && scheduler.isStarted()) {
            scheduler.stop()
            Event.builder().type(Event.Type.cluster).reason('cron job scheduler stopped').
                    result(Utils.localIp()).build().log().toDto().add()
        }
    }

    synchronized boolean refresh() {
        def appList = new AppDTO().queryFields('id,status,job_conf').
                where('status = ?', AppDTO.Status.auto.val).
                where('job_conf is not null').list()
        Map<Integer, String> item = [:]
        for (one in appList) {
            item[one.id] = json.json(one.jobConf)
        }

        if (lastSaved == item) {
            return true
        }

        Event.builder().type(Event.Type.cluster).reason('cron job scheduler refresh').
                result(Utils.localIp()).build().log('' + lastSaved + '->' + item).toDto().add()
        lastSaved = item

        stop()

        if (item) {
            scheduler = new Scheduler()
            appList.each { AppDTO entry ->
                scheduler.schedule(entry.jobConf.cronExp, new RunAppTask(entry.id))
            }
            scheduler.start()
            Event.builder().type(Event.Type.cluster).reason('cron job scheduler started').
                    result(Utils.localIp()).build().log().toDto().add()
        }
        true
    }
}
