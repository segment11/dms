package plugin.model

import groovy.transform.CompileStatic
import groovy.transform.TupleConstructor

@CompileStatic
@TupleConstructor
class Menu {
    String title
    String icon
    String module
    String page
    List<Menu> children
}
