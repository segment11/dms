package model.job

import groovy.transform.CompileStatic
import groovy.transform.ToString
import model.BaseRecord
import model.vendor.TargetBucket

@CompileStatic
@ToString(includeNames = true, includeSuper = false)
class RmBackupTemplateDTO extends BaseRecord<RmBackupTemplateDTO> {
    @CompileStatic
    enum TargetType {
        nfs, s3
    }

    @CompileStatic
    enum Provider {
        aws, aliyun, tencent, huawei
    }

    Integer id

    String name

    TargetType targetType

    Provider provider

    TargetBucket targetBucket

    Date updatedDate
}
