package model.cluster

import groovy.transform.AutoClone
import groovy.transform.CompileStatic

@CompileStatic
@AutoClone
class MultiSlotRange implements Comparable<MultiSlotRange> {
    List<SlotRange> list = []

    int totalNumber() {
        if (!list) {
            return 0
        }

        int sum = 0
        for (one in list) {
            sum += one.totalNumber()
        }
        sum
    }

    void addSingle(Integer begin, Integer end) {
        list.add(new SlotRange(begin, end))
        list.sort()
    }

    void addMerge(Integer begin, Integer end) {
        TreeSet<Integer> set = []
        for (i in begin..end) {
            set << i
        }
        def copy = addSet(set)
        list = copy.list
    }

    void removeSingle(Integer begin, Integer end) {
        list.removeIf {
            it.begin == begin && it.end == end
        }
    }

    void removeMerge(Integer begin, Integer end) {
        TreeSet<Integer> set = []
        for (i in begin..end) {
            set << i
        }
        def copy = removeSet(set)
        list = copy.list
    }

    MultiSlotRange removeSet(TreeSet<Integer> remove, TreeSet<Integer> add = null) {
        if (!remove && !add) {
            return this
        }

        def set = toTreeSet()
        if (remove) {
            set.removeAll(remove)
        }
        if (add) {
            set.addAll(add)
        }
        fromSet(set)
    }

    MultiSlotRange addSet(TreeSet<Integer> add) {
        removeSet(null, add)
    }

    TreeSet<Integer> removeSomeFromEnd(int removeNumber) {
        def arr = toArray()
        if (removeNumber > arr.length) {
            throw new IllegalArgumentException('remove more than total, remove number: ' +
                    removeNumber + ', total: ' + arr.length)
        }

        TreeSet<Integer> removed = []
        def beginIndex = arr.length - removeNumber
        def endIndex = arr.length - 1
        for (i in beginIndex..endIndex) {
            removed << arr[i]
        }
        removed
    }

    // nearly O(list.size =~ 1)
    boolean contains(Integer slot) {
        for (one in list) {
            if (slot >= one.begin && slot <= one.end) {
                return true
            }
        }
        false
    }

    TreeSet<Integer> toTreeSet() {
        TreeSet<Integer> r = []
        if (!list) {
            return r
        }

        for (one in list) {
            for (int i = one.begin; i <= one.end; i++) {
                r << i
            }
        }
        r
    }

    HashSet<Integer> toHashSet() {
        def cap = list ? list[-1].end - list[0].begin : 16
        HashSet<Integer> r = new HashSet<>(cap)
        if (!list) {
            return r
        }

        for (one in list) {
            for (int i = one.begin; i <= one.end; i++) {
                r << i
            }
        }
        r
    }

    int[] toArray() {
        def list = toList()
        toIntArray(list)
    }

    static int[] toIntArray(Collection<Integer> list) {
        def arr2 = list.toArray()
        int[] arr = new int[arr2.length]
        for (int i = 0; i < arr2.length; i++) {
            arr[i] = arr2[i] as int
        }
        arr
    }

    ArrayList<Integer> toList() {
        ArrayList<Integer> r = []
        if (!list) {
            return r
        }

        for (one in list) {
            for (int i = one.begin; i <= one.end; i++) {
                r << i
            }
        }
        r
    }

    @Override
    int compareTo(MultiSlotRange o) {
        if (!list) {
            return -1
        }

        if (!o.list) {
            return 1
        }

        list[0].begin <=> o.list[0].begin
    }

    @Override
    String toString() {
        if (!list) {
            return ''
        }
        list.collect { "${it.begin}-${it.end}" }.join(',')
    }

    static MultiSlotRange fromSelfString(String toString) {
        def r = new MultiSlotRange()
        if (!toString) {
            return r
        }

        for (it in toString.split(',')) {
            def arr = it.split(/-/)
            r.addSingle(arr[0] as int, arr[1] as int)
        }
        r
    }

    static MultiSlotRange fromSet(TreeSet<Integer> all) {
        def r = new MultiSlotRange()

        if (!all) {
            return r
        }

        if (all.size() == (all[-1] - all[0] + 1)) {
            r.addSingle(all[0], all[-1])
            return r
        }

        Integer begin
        Integer end
        Integer last
        for (j in all) {
            if (begin == null) {
                begin = j
            }
            if (end == null) {
                end = j
            }
            if (last == null) {
                last = j
            }

            if (j != begin) {
                if (j != last + 1) {
                    r.addSingle(begin, end)
                    begin = j
                    end = j
                    last = j
                    continue
                }
            }

            last = j
            end = j
        }

        if (begin != null && end != null) {
            r.addSingle(begin, end)
        }
        r
    }

    List<String> clusterNodesArgs(String nodeId, String ip, Integer port) {
        List<String> argsList = []
        if (!list) {
            argsList << "${nodeId} ${ip} ${port} master -".toString()
        } else {
            def allSlotRange = list.collect { it.begin == it.end ? "${it.begin}" : "${it.begin}-${it.end}" }.join(' ')
            argsList << "${nodeId} ${ip} ${port} master - ${allSlotRange}".toString()
        }
        argsList
    }
}
