package common

import groovy.transform.CompileStatic

@CompileStatic
class LimitQueue<E> extends LinkedList<E> {
    private int limit

    LimitQueue(int limit) {
        this.limit = limit
    }

    int getLimit() {
        return limit
    }

    @Override
    boolean offer(E e) {
        if (size() >= limit) {
            poll()
        }
        super.offer(e)
    }

    @Override
    boolean add(E e) {
        if (size() >= limit) {
            poll()
        }
        super.add(e)
    }
}
