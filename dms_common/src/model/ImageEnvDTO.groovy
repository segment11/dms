package model

import groovy.transform.CompileStatic
import groovy.transform.ToString

@CompileStatic
@ToString(includeNames = true, includeSuper = false)
class ImageEnvDTO extends BaseRecord<ImageEnvDTO> {

    Integer id

    String imageName

    String name

    String des

    String env
}