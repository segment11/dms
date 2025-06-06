package rm

import groovy.transform.CompileStatic
import org.segment.d.Pager

@CompileStatic
class SlotBalancer {
    static final int total = 16384

    static Pager splitAvg(int shardSize, int pageNum = 1) {
        def totalCount = total
        int totalPage = shardSize

        int pageSize = Math.ceil((totalCount / totalPage) as double).intValue()
        def pager = new Pager(pageNum, pageSize)
        pager.totalCount = total
        pager
    }

    // once add one shard only
    static int needMigrateSlotSize(int fromShardSize) {
        final int addShardNumber = 1
        final int toShardSize = fromShardSize + addShardNumber

        int avgSlotNumberThisVersion = Math.ceil((total / fromShardSize) as double).intValue()
        int avgSlotNumberNextVersion = Math.ceil((total / toShardSize) as double).intValue()
        avgSlotNumberThisVersion - avgSlotNumberNextVersion
    }

    // shard size -> after remove one shard
    static List<TreeSet<Integer>> splitSlotSetForMigrateWhenReduceShard(ArrayList<Integer> slotList, int shardSize) {
        List<TreeSet<Integer>> r = []

        def totalCount = slotList.size()
        int totalPage = shardSize

        // 0 for some target shard if slot set is too small
        if (totalCount < totalPage) {
            for (pageNum in 1..totalPage) {
                TreeSet<Integer> one = []
                if (pageNum <= totalCount) {
                    one << slotList[pageNum - 1]
                }
                r << one
            }
            return r
        }

        int pageSize = Math.ceil((totalCount / totalPage) as double).intValue()
        for (pageNum in 1..totalPage) {
            def pager = new Pager(pageNum, pageSize)
            pager.totalCount = totalCount

            TreeSet<Integer> one = []
            for (i in pager.start..<pager.end) {
                one << slotList[i]
            }
            r << one
        }

        r
    }
}
