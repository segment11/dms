package model

import groovy.transform.CompileStatic
import groovy.transform.ToString
import model.json.TplParamsConf

@CompileStatic
@ToString(includeNames = true, includeSuper = false)
class ImageTplDTO extends BaseRecord<ImageTplDTO> {
    @CompileStatic
    static enum TplType {
        init, mount
    }

    Integer id

    String imageName

    String name

    String des

    TplType tplType

    String mountDist

    Boolean isParentDirMount

    String content

    TplParamsConf params

    Date updatedDate
}