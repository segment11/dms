package model

import groovy.transform.CompileStatic
import groovy.transform.ToString

@CompileStatic
@ToString(includeNames = true, includeSuper = false)
class ImageRegistryDTO extends BaseRecord<ImageRegistryDTO> {

    Integer id

    String name

    String url

    String loginUser

    String loginPassword

    Date updatedDate

    String trimScheme() {
        if (!url) {
            return null
        }
        url.replace('http://', '').replace('https://', '')
    }

    boolean anon() {
        'anon' == loginUser
    }

    boolean local() {
        'local' == name
    }
}