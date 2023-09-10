package auth

import groovy.transform.CompileStatic
import model.AppDTO
import model.NamespaceDTO
import server.InMemoryCacheSupport

@CompileStatic
class User {
    public static final Permit PermitAdmin = new Permit(PermitType.admin)
    public static final User Admin = new User(permitList: [PermitAdmin])

    String name

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

    Set<Integer> getAccessAppIdSet(int clusterId) {
        def userAccessAppIdSet = new HashSet<Integer>()
        def userAccessClusterIdList = permitList.findAll { it.type == PermitType.cluster }.collect { it.id }
        if (clusterId != 0 && clusterId !in userAccessClusterIdList) {
            return userAccessAppIdSet
        }

        def cacheSupport = InMemoryCacheSupport.instance
        permitList.each {
            if (it.type == PermitType.cluster) {
                def appList = cacheSupport.appList.findAll { it.clusterId == it.id }
                userAccessAppIdSet.addAll(appList.collect { it.id })
            } else if (it.type == PermitType.namespace) {
                def appList = cacheSupport.appList.findAll { it.namespaceId == it.id }
                userAccessAppIdSet.addAll(appList.collect { it.id })
            } else if (it.type == PermitType.app) {
                userAccessAppIdSet.add(it.id)
            }
        }
        userAccessAppIdSet
    }
}
