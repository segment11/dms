package server.scheduler.checker

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j

@CompileStatic
@Singleton
@Slf4j
class CleanerHolder {
    List<Cleaner> cleanerList = []

    synchronized void add(Cleaner cleaner) {
        def old = cleanerList.find { it.name() == cleaner.name() }
        if (old) {
            cleanerList.remove(old)
        }
        cleanerList.add(cleaner)
        log.info 'done add cleaner - {}', cleaner.name()
    }
}
