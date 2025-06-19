package server.scheduler.checker

import groovy.transform.CompileStatic
import model.server.CreateContainerConf
import server.scheduler.processor.JobStepKeeper

@CompileStatic
interface Checker {
    boolean check(CreateContainerConf conf, JobStepKeeper keeper)

    Type type()

    String name()

    String imageName()

    default String script(CreateContainerConf conf) {
        null
    }

    default boolean canUseTo(String group, String image) {
        false
    }

    @CompileStatic
    static enum Type {
        before, beforeStart, after, init
    }
}