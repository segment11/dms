package model.vendor

import groovy.transform.CompileStatic
import groovy.transform.ToString
import org.segment.d.json.JSONFiled

@CompileStatic
@ToString(includeNames = true)
class TargetBucket implements JSONFiled {
    String bucketName
    String endpoint
    String accessKey
    String secretKey
}
