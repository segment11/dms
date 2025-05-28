package auth

import groovy.transform.CompileStatic
import model.UserPermitDTO

@CompileStatic
class DefaultLoginService implements LoginService {
    @Override
    User login(String user, String password) {
        def list = new UserPermitDTO(userName: user).list(100)
        if (!list) {
            return null
        }

        def u = new User(name: user)
        u.permitList.addAll(list.collect {
            new Permit(PermitType.valueOf(it.permitType), it.resourceId)
        })
        if (user == PermitType.admin.name()) {
            u.permitList << User.PermitAdmin
        }
        u
    }
}
