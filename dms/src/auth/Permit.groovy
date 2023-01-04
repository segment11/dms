package auth

import groovy.transform.CompileStatic

@CompileStatic
class Permit {
    PermitType type

    int id

    Permit(PermitType type, int id = 0) {
        this.type = type
        this.id = id
    }
}