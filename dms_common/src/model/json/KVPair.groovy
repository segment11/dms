package model.json

import groovy.transform.CompileStatic
import groovy.transform.ToString
import groovy.transform.TupleConstructor

@CompileStatic
@ToString(includeNames = true)
@TupleConstructor
class KVPair<E> {
    String key
    E value

    @Override
    boolean equals(Object obj) {
        if (!(obj instanceof KVPair)) {
            return false
        }
        def one = (KVPair) obj
        key == one.key && value == one.value
    }
}
