package model

import groovy.transform.CompileStatic
import groovy.transform.ToString
import model.json.ExtendParams

@CompileStatic
@ToString(includeNames = true, includeSuper = false)
class AppJobDTO extends BaseRecord<AppJobDTO> {
    @CompileStatic
    static enum Status {
        created, processing, failed, done
    }

    @CompileStatic
    static enum JobType {
        create, remove, scroll
    }

    Integer id

    Integer appId

    Status status

    Integer failNum

    JobType jobType

    String message

    ExtendParams params

    Date createdDate

    Date updatedDate

    Object param(String key) {
        params?.get(key)
    }

    AppJobDTO needRunInstanceIndexList(List<Integer> needRunInstanceIndexList) {
        addParam('needRunInstanceIndexList', needRunInstanceIndexList)
    }

    List<Integer> runInstanceIndexList() {
        param('needRunInstanceIndexList') as List<Integer>
    }

    AppJobDTO addParam(String key, Object value) {
        if (params == null) {
            params = new ExtendParams()
        }
        params.put(key, value)
        this
    }
}