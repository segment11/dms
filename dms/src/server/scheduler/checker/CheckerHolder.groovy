package server.scheduler.checker

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j

@CompileStatic
@Singleton
@Slf4j
class CheckerHolder {
    List<Checker> checkerList = []

    synchronized void add(Checker checker) {
        checkerList.removeIf { it.name() == checker.name() }
        checkerList.add(checker)
        log.info 'done add checker: {}, type: {}', checker.name(), checker.type().name()
    }
}
