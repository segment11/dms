package model


import groovy.transform.CompileStatic
import groovy.transform.ToString
import model.json.ExtendParams

@CompileStatic
@ToString(includeNames = true, includeSuper = false)
class AppJobDTO extends BaseRecord<AppJobDTO> {
    @CompileStatic
    static enum Status {
        created(0), processing(1), failed(-1), done(10)

        int val

        Status(int val) {
            this.val = val
        }
    }

    @CompileStatic
    static enum JobType {
        create(1), remove(2), scroll(3)

        int val

        JobType(int val) {
            this.val = val
        }
    }

    Integer id

    Integer appId

    Integer status

    Integer failNum

    Integer jobType

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