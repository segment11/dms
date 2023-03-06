package plugin

import groovy.transform.CompileStatic

@CompileStatic
interface Plugin {

    void init()

    void destroy()

    String name()

    String version()

    String registry()

    String group()

    String image()

    String tag()

    Map<String, String> expressions()

    Date loadTime()

}