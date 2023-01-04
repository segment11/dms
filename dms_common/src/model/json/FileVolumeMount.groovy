package model.json

import groovy.transform.CompileStatic
import groovy.transform.ToString

@CompileStatic
@ToString(includeNames = true)
class FileVolumeMount {
    Integer imageTplId

    Boolean isParentDirMount

    List<KVPair<String>> paramList = []

    String dist

    String content

    boolean isReloadInterval

    @Override
    boolean equals(Object obj) {
        if (!(obj instanceof FileVolumeMount)) {
            return false
        }
        def one = (FileVolumeMount) obj
        imageTplId == one.imageTplId && paramList == one.paramList && dist == one.dist &&
                content == one.content && isReloadInterval == one.isReloadInterval
    }
}
