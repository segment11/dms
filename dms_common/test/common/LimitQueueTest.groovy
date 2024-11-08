package common

import spock.lang.Specification

class LimitQueueTest extends Specification {
    def 'test all'() {
        given:
        LimitQueue<Integer> limitQueue = new LimitQueue<>(3)
        when:
        limitQueue.add(1)
        limitQueue.add(2)
        limitQueue.add(3)
        limitQueue.add(4)
        then:
        limitQueue.limit == 3
        limitQueue.size() == 3
        limitQueue.get(0) == 2
        limitQueue.get(1) == 3
        limitQueue.get(2) == 4

        when:
        limitQueue.clear()
        limitQueue.offer(1)
        limitQueue.offer(2)
        limitQueue.offer(3)
        limitQueue.offer(4)
        then:
        limitQueue.size() == 3
    }
}
