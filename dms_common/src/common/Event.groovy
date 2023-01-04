package common

import groovy.transform.CompileStatic
import groovy.transform.ToString
import groovy.transform.builder.Builder
import groovy.util.logging.Slf4j
import model.EventDTO

@CompileStatic
@Builder
@Slf4j
@ToString(includeNames = true)
class Event {
//    Logger logAudit = LoggerFactory.getLogger('audit')

    @CompileStatic
    static enum Type {
        cluster, node, app, user
    }

    Integer id

    Type type

    String reason

    Object result

    String message

    Date createdDate

    Event log(String message = '') {
        this.message = message
        log.info("{}/{}/{} - {}", type, reason, result, message)
        this
    }

    Event audit(String message = '') {
        this.message = message
        log.info("{}/{}/{} - {}", type, reason, result, message)
        this
    }

    EventDTO toDto() {
        new EventDTO(id: id, type: type?.name(), reason: reason, result: result?.toString(),
                message: message, createdDate: new Date())
    }
}