package server.scheduler.checker

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j

@CompileStatic
@Singleton
@Slf4j
class CleanerHolder {
    List<Cleaner> cleanerList = []

    synchronized void add(Cleaner cleaner) {
        cleanerList.removeIf { it.name() == cleaner.name() }
        cleanerList.add(cleaner)
        log.info 'done add cleaner - {}', cleaner.name()
    }
}
