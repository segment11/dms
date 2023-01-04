package server.scheduler.processor

import groovy.transform.CompileStatic
import groovy.transform.ToString
import groovy.util.logging.Slf4j
import model.AppJobLogDTO

@CompileStatic
@Slf4j
class JobStepKeeper {
    @CompileStatic
    static enum Step {
        chooseNode, pullImage, preCheck, createContainer, initContainer, startContainer,
        copyDirs, updateTpl, startCmd, wrapContainerInfo,
        updateDns, afterCheck, addToGateway, stopAndRemoveContainer, removeFromGateway, done, yourStep
    }

    @CompileStatic
    @ToString(includeNames = true)
    static class JobStep {
        Step step
        String title
        String message
        Date createdDate = new Date()
        Date endDate = new Date()
        String nodeIp
        int instanceIndex
        boolean isOk
        long costMs
    }

    int jobId
    String nodeIp
    int instanceIndex

    private long beginT = System.currentTimeMillis()

    JobStepKeeper next(Step step, String title, String message = '', boolean isOk = true) {
        def costMs = System.currentTimeMillis() - beginT
        def jobStep = new JobStep(step: step, nodeIp: nodeIp, instanceIndex: instanceIndex,
                title: title, message: message, costMs: costMs, isOk: isOk)
        def str = jobStep.toString()
        log.info str
        new AppJobLogDTO(jobId: jobId, instanceIndex: instanceIndex,
                title: title, message: str, isOk: isOk, createdDate: new Date()).add()
        beginT = System.currentTimeMillis()
        this
    }
}
