package model.cluster

import groovy.transform.AutoClone
import groovy.transform.CompileStatic
import groovy.transform.TupleConstructor

@CompileStatic
@TupleConstructor
@AutoClone
class SlotRange implements Comparable<SlotRange> {
    Integer begin
    Integer end

    int totalNumber() {
        end - begin + 1
    }

    @Override
    String toString() {
        "${begin}-${end}".toString()
    }

    @Override
    int compareTo(SlotRange o) {
        begin <=> o.begin
    }
}
