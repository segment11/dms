package model.json

import groovy.transform.CompileStatic
import groovy.transform.ToString
import model.cluster.MultiSlotRange
import org.segment.d.json.JSONFiled

@CompileStatic
@ToString(includeNames = true)
class ClusterSlotsDetail implements JSONFiled {
    // one shard, one application
    @CompileStatic
    static class Shard {
        Integer shardIndex
        Integer appId
        MultiSlotRange multiSlotRange = new MultiSlotRange()
    }

    List<Shard> shards = []
}
