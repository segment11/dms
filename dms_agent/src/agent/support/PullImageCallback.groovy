package agent.support

import com.github.dockerjava.api.command.PullImageResultCallback
import com.github.dockerjava.api.model.PullResponseItem
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j

@CompileStatic
@Slf4j
class PullImageCallback extends PullImageResultCallback {
    final Set<Integer> percentPassedSet = []

    long beginT = System.currentTimeMillis()

    PullImageCallback() {
        10.times {
            percentPassedSet << (it * 10)
        }
    }

    Closure<Void> trigger

    @Override
    void onNext(PullResponseItem item) {
        super.onNext(item)

        def string = item.toString()
        log.info string

        if (string && string.contains('progress=[')) {
            // ProgressDetail(current=3255545, total=3401613, start=null)
            def pat = ~/^.+current=(\d+), total=(\d+).+$/
            def mat = pat.matcher(string)
            if (mat.matches()) {
                def current = mat.group(1) as int
                def total = mat.group(2) as int

                def percent = (Math.ceil((current / total * 100).doubleValue()).intValue() / 10).intValue() * 10
                def isRemoved = percentPassedSet.remove(percent)
                if (isRemoved && trigger) {
                    def costMs = System.currentTimeMillis() - beginT
                    trigger.call(percent, costMs)
                    beginT = System.currentTimeMillis()
                }
            }
        }
    }
}
