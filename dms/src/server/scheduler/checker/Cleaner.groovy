package server.scheduler.checker

import groovy.transform.CompileStatic
import model.AppDTO

@CompileStatic
interface Cleaner {
    boolean clean(AppDTO app)

    String name()

    List<String> imageName()
}