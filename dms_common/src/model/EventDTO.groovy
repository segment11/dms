package model

import groovy.transform.CompileStatic
import groovy.transform.ToString

@CompileStatic
@ToString(includeNames = true, includeSuper = false)
class EventDTO extends BaseRecord<EventDTO> {
    Integer id

    String type

    String reason

    String result

    String message

    Date createdDate
}