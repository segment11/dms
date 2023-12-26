package plugin.callback

import transfer.ContainerInfo

interface LiveCheckResultHandler {
    void liveCheckResultHandle(ContainerInfo x)
}
