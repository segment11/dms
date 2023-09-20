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

    String toFormatString() {
        type.name() + '=' + id
    }

    static Permit fromFormatString(String formatString) {
        def arr = formatString.split('=')
        PermitType type = PermitType.valueOf(arr[0])
        int id = arr[1] as int
        new Permit(type, id)
    }
}