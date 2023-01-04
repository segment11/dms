package model.json

import groovy.transform.CompileStatic
import groovy.transform.ToString

@CompileStatic
@ToString(includeNames = true)
class DirVolumeMount {

    Integer nodeVolumeId

    String dir

    String dist

    String mode

    @Override
    boolean equals(Object obj) {
        if (!(obj instanceof DirVolumeMount)) {
            return false
        }
        def one = (DirVolumeMount) obj
        nodeVolumeId == one.nodeVolumeId && dir == one.dir && dist == one.dist
    }
}
