package script

import agent.support.ContainerLogViewCallback
import com.github.dockerjava.api.DockerClient
import org.apache.commons.io.input.ReversedLinesFileReader

import java.util.concurrent.TimeUnit

import static common.ContainerHelper.getAppIdFromProcess
import static common.ContainerHelper.isProcess

DockerClient docker = super.binding.getProperty('docker') as DockerClient
Map params = super.binding.getProperty('params') as Map
String id = params.id
if (!id) {
    return [error: 'id required']
}

int tailLines = params.tailLines ? params.tailLines as int : 100

if (isProcess(id)) {
    int appId = getAppIdFromProcess(id)

    def logFile = new File('/opt/dms/app_' + appId + '/main.log')
    if (!logFile.exists()) {
        return ''
    } else {
        def reader = new ReversedLinesFileReader(logFile)
        def sb = new StringBuffer()
        (0..<tailLines).each {
            try {
                sb.append reader.readLine()
                sb.append "\r\n"
            } catch (Exception ee) {
                // ignore
                return sb.toString()
            }
        }
        return sb.toString()
    }
}

int oneDayAgo = ((System.currentTimeMillis() / 1000) as int) - 24 * 3600
int since = params.since ? params.since as int : oneDayAgo

def callBack = new ContainerLogViewCallback()
docker.logContainerCmd(id).
        withTimestamps(true).
        withStdOut(true).
        withStdErr(true).
        withTail(tailLines).
        withSince(since).
        exec(callBack)

callBack.awaitCompletion(10, TimeUnit.SECONDS)
callBack.os.toString()
