package plugin

import groovy.transform.CompileStatic
import model.AppDTO
import plugin.model.Menu

@CompileStatic
interface Plugin {

    void init()

    void destroy()

    String name()

    String version()

    String registry()

    String group()

    String image()

    default boolean canUseTo(String group, String image) {
        false
    }

    String tag()

    Map<String, String> expressions()

    Date loadTime()

    AppDTO demoApp(AppDTO app)

    List<Menu> menus()

}