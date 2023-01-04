package auth

import groovy.transform.CompileStatic

@CompileStatic
enum PermitType {
    admin, cluster, namespace, app, imageManager
}