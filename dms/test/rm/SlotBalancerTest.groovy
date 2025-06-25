package rm

import spock.lang.Specification

class SlotBalancerTest extends Specification {
    final int t = 16384

    void splitAvgTest() {
        given:
        def pager = SlotBalancer.splitAvg(1, 1)
        def pager2 = SlotBalancer.splitAvg(2, 2)
        def pager3 = SlotBalancer.splitAvg(3, 2)
        expect:
        pager.start == 0
        pager.end == t

        pager2.start == t / 2
        pager2.end == t

        pager3.start == ((t / 3) as int) + 1
        pager3.end == pager3.start * 2
    }

    void needMigrateSlotSizeTest() {
        given:
        def i1 = SlotBalancer.needMigrateSlotSize(1)
        def i2 = SlotBalancer.needMigrateSlotSize(2)
        expect:
        i1 == (t / 2) as int
        i2 == ((t / 2) as int) - (((t / 3) as int) + 1)
    }

    void splitSlotSetForMigrateWhenReduceShardTest() {
        given:
        ArrayList<Integer> slotListSmall = [0, 1, 2, 3, 4]
        ArrayList<Integer> slotList = []
        t.times {
            slotList << it
        }
        def r0 = SlotBalancer.splitSlotSetForMigrateWhenReduceShard(slotListSmall, 10)

        def r1 = SlotBalancer.splitSlotSetForMigrateWhenReduceShard(slotList, 1)
        def r2 = SlotBalancer.splitSlotSetForMigrateWhenReduceShard(slotList, 2)
        def r3 = SlotBalancer.splitSlotSetForMigrateWhenReduceShard(slotList, 3)

        expect:
        r0.size() == 10
        r0[0].size() == 1
        r0[0][0] == 0

        r0[1].size() == 1
        r0[1][0] == 1

        r0[2].size() == 1
        r0[2][0] == 2

        r0[3].size() == 1
        r0[3][0] == 3

        r0[4].size() == 1
        r0[4][0] == 4

        r0[5].size() == 0

        r1.size() == 1
        r1[0].containsAll slotList

        r2.size() == 2
        def half = (t / 2) as int
        r2[0].containsAll slotList[0..<half]
        r2[1].containsAll slotList[half..-1]

        def oneThird = ((t / 3) as int) + 1
        r3.size() == 3
        r3[0].containsAll slotList[0..<oneThird]
        r3[1].containsAll slotList[oneThird..<(oneThird * 2)]
        r3[2].containsAll slotList[(oneThird * 2)..<-1]
    }
}

