package agent.support

import com.github.dockerjava.api.async.ResultCallback
import com.github.dockerjava.api.model.Frame
import groovy.transform.CompileStatic

@CompileStatic
class ContainerLogViewCallback extends ResultCallback.Adapter<Frame> {
    ByteArrayOutputStream os = new ByteArrayOutputStream()

    @Override
    void onNext(Frame frame) {
        os.write frame.payload
    }
}




