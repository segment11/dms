package agent

import groovy.transform.CompileStatic
import groovy.transform.TupleConstructor

@CompileStatic
@TupleConstructor
class OneContainerId {
    Integer appId
    Integer instanceIndex
}
