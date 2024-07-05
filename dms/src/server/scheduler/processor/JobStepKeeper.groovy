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
        copyDirs, updateTpl, startCmd, afterCmd, wrapContainerInfo,
        updateDns, afterCheck, stopAndRemoveContainer, done, yourStep
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

        String toHtmlString() {
            // pretty show
            """
<table class="table table-bordered table-striped">
    <tr>
        <td>step</td>
        <td>${step}</td>
    </tr>
    <tr>
        <td>title</td>
        <td>${title}</td>
    </tr>    
    <tr>
        <td>message</td>
        <td>${message}</td>
    </tr>
    <tr>
        <td>created date</td>
        <td>${createdDate}</td>
    </tr>
    <tr>
        <td>end date</td>
        <td>${endDate}</td>
    </tr>
    <tr>
        <td>node ip</td>
        <td>${nodeIp}</td>
    </tr>
    <tr>
        <td>instance index</td>
        <td>${instanceIndex}</td>
    </tr>
    <tr>
        <td>is ok</td>
        <td>${isOk}</td>
    </tr>
    <tr>
        <td>cost ms</td>
        <td>${costMs}</td>
    </tr>       
</table>
""".toString()
        }
    }

    int jobId
    String nodeIp
    int instanceIndex

    private long beginT = System.currentTimeMillis()

    JobStepKeeper next(Step step, String title, String message = '', boolean isOk = true) {
        def costMs = System.currentTimeMillis() - beginT
        def jobStep = new JobStep(step: step, nodeIp: nodeIp, instanceIndex: instanceIndex,
                title: title, message: message, costMs: costMs, isOk: isOk)
        def html = jobStep.toHtmlString()
        log.info html
        new AppJobLogDTO(jobId: jobId, instanceIndex: instanceIndex,
                title: title, message: html, isOk: isOk, costMs: costMs, createdDate: new Date()).add()
        beginT = System.currentTimeMillis()
        this
    }
}
