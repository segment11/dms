package auth

import groovy.transform.CompileStatic

@CompileStatic
interface LoginService {
    User login(String user, String password)
}