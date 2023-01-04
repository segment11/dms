package model.json

import groovy.transform.CompileStatic
import groovy.transform.ToString

@CompileStatic
@ToString(includeNames = true)
class ULimit {
    String name
    Long soft
    Long hard

    @Override
    boolean equals(Object obj) {
        if (!(obj instanceof ULimit)) {
            return false
        }
        def one = (ULimit) obj
        name == one.name && soft == one.soft && hard == one.hard
    }
}
