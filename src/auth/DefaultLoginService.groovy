package auth

import groovy.transform.CompileStatic
import model.UserPermitDTO

@CompileStatic
class DefaultLoginService implements LoginService {
    @Override
    User login(String user, String password) {
        def u = new User(name: user)
        def list = new UserPermitDTO(user: user).loadList(10) as List<UserPermitDTO>
        u.permitList.addAll(list.collect {
            new Permit(PermitType.valueOf(it.permitType), it.resourceId)
        })
        if (user == PermitType.admin.name()) {
            u.permitList << User.PermitAdmin
        }
        u
    }
}
