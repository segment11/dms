package model

import groovy.transform.CompileStatic
import groovy.transform.ToString

@CompileStatic
@ToString(includeNames = true, includeSuper = false)
class ImagePortDTO extends BaseRecord<ImagePortDTO> {

    Integer id

    String imageName

    String name

    String des

    Integer port
}