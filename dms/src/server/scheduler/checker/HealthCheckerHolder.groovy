package server.scheduler.checker

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j

@CompileStatic
@Singleton
@Slf4j
class HealthCheckerHolder {
    List<HealthChecker> checkerList = []

    synchronized void add(HealthChecker checker) {
        checkerList.removeIf { it.name() == checker.name() }
        checkerList.add(checker)
        log.info 'done add health checker - {}', checker.name()
    }
}
