package server.scheduler.checker

import groovy.transform.CompileStatic
import model.AppDTO

@CompileStatic
interface HealthChecker {
    String name()

    String imageName()

    boolean check(AppDTO app)
}
