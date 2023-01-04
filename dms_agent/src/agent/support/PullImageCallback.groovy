package agent.support

import com.github.dockerjava.api.command.PullImageResultCallback
import com.github.dockerjava.api.model.PullResponseItem
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j

@CompileStatic
@Slf4j
class PullImageCallback extends PullImageResultCallback {
    @Override
    void onNext(PullResponseItem item) {
        super.onNext(item)
        log.info item.toString()
    }
}
