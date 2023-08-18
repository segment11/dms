package model.traefik

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j

@CompileStatic
@Slf4j
class JustTest {
    static String hi(String name) {
        log.info 'just test for ctrl script'
    }
}
