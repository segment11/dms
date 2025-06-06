package model.json

import groovy.transform.CompileStatic
import groovy.transform.ToString

@CompileStatic
@ToString(includeNames = true)
class FileVolumeMount {
    Integer imageTplId

    Boolean isParentDirMount

    List<KVPair<String>> paramList = []

    Object paramValue(String key) {
        paramList.find { it.key == key }?.value
    }

    String dist

    String content

    boolean isReloadInterval

    FileVolumeMount copy() {
        def r = new FileVolumeMount()
        r.imageTplId = imageTplId
        r.isParentDirMount = isParentDirMount
        for (param in paramList) {
            r.paramList.add(new KVPair<String>(param.key, param.value))
        }
        r.dist = dist
        r.content = content
        r.isReloadInterval = isReloadInterval
        return r
    }

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
