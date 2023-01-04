package auth

import groovy.transform.CompileStatic
import model.AppDTO
import model.NamespaceDTO

@CompileStatic
class User {
    public static final Permit PermitAdmin = new Permit(PermitType.admin)
    public static final Permit PermitImageManager = new Permit(PermitType.imageManager)
    public static final User Admin = new User(permitList: [PermitAdmin])

    String name

    String introduction

    Date lastLoginTime = new Date()

    List<Permit> permitList = []

    boolean isAdmin() {
        permitList.any {
            it.type == PermitType.admin
        }
    }

    boolean isImageManager() {
        isAccess(PermitType.imageManager)
    }

    boolean isAccessCluster(int clusterId) {
        isAccess(PermitType.cluster, clusterId)
    }

    boolean isAccessNamespace(int namespaceId) {
        isAccess(PermitType.namespace, namespaceId)
    }

    boolean isAccessApp(int appId) {
        isAccess(PermitType.app, appId)
    }

    private boolean isAccess(PermitType permitType, int id = 0) {
        if (permitList.any {
            it.type == PermitType.admin || (it.type == permitType && it.id == id)
        }) {
            return true
        }
        if (permitType == PermitType.namespace) {
            def namespaceOne = new NamespaceDTO(id: id).queryFields('cluster_id').one()
            if (permitList.any {
                it.type == PermitType.cluster && it.id == namespaceOne.clusterId
            }) {
                return true
            }
        } else if (permitType == PermitType.app) {
            def appOne = new AppDTO(id: id).queryFields('cluster_id,namespace_id').one()
            if (permitList.any {
                (it.type == PermitType.cluster && it.id == appOne.clusterId) ||
                        (it.type == PermitType.namespace && it.id == appOne.namespaceId)
            }) {
                return true
            }
        }
        false
    }
}
