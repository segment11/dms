package model.json

import groovy.transform.CompileStatic
import groovy.transform.ToString
import org.segment.d.json.JSONFiled

@CompileStatic
@ToString(includeNames = true)
@Deprecated
class GwAuth implements JSONFiled {
    List<KVPair> basicList = []
}
