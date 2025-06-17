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
        scp, nfs, s3
    }

    @CompileStatic
    enum Provider {
        idc, aws, aliyun, tencent, huawei
    }

    Integer id

    String name

    TargetType targetType

    Provider provider

    // for s3
    TargetBucket targetBucket

    // for scp
    String[] targetNodeIps

    // when scp or nfs, backup data dir, when s3, object key prefix in bucket
    String backupDataDir

    Date updatedDate
}
