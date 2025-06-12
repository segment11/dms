package model.json

import groovy.transform.CompileStatic
import groovy.transform.ToString
import org.segment.d.json.JSONFiled

@CompileStatic
@ToString(includeNames = true, includePackage = false)
class PrimaryReplicasDetail implements JSONFiled {

    List<Node> nodes = []

    @CompileStatic
    @ToString(includeNames = true, includePackage = false)
    static class Node {
        boolean isPrimary
        String ip
        int port
        int replicaIndex

        String uuid() {
            ip + ':' + port
        }
    }
}
