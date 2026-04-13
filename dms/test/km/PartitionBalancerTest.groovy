package km

import spock.lang.Specification

class PartitionBalancerTest extends Specification {

    void 'assign replicas - single replica'() {
        expect:
        PartitionBalancer.assignReplicas(3, 6, 1) == [[0], [1], [2], [0], [1], [2]]
    }

    void 'assign replicas - full replication'() {
        expect:
        PartitionBalancer.assignReplicas(3, 3, 3) == [[0, 1, 2], [1, 2, 0], [2, 0, 1]]
    }

    void 'assign replicas - replication factor exceeds broker count throws'() {
        when:
        PartitionBalancer.assignReplicas(2, 3, 3)
        then:
        thrown(AssertionError)
    }

    void 'assign replicas - single broker single partition'() {
        expect:
        PartitionBalancer.assignReplicas(1, 1, 1) == [[0]]
    }

    void 'reassign for scale - adds new brokers'() {
        given:
        def current = [[0], [1], [2], [0], [1], [2]]
        when:
        def result = PartitionBalancer.reassignForScale(current, [3] as int[])
        then:
        result.size() == 6
        result.every { it.size() == 1 }
        result.collect { it[0] }.toSet().contains(3)
    }

    void 'reassign for scale - empty new brokers returns same structure'() {
        given:
        def current = [[0, 1], [1, 2], [2, 0]]
        when:
        def result = PartitionBalancer.reassignForScale(current, [] as int[])
        then:
        result.size() == 3
        result.every { it.size() == 2 }
    }

    void 'reassign for scale - multiple new brokers'() {
        given:
        def current = [[0], [1], [0], [1]]
        when:
        def result = PartitionBalancer.reassignForScale(current, [2, 3] as int[])
        then:
        result.size() == 4
        def allBrokers = result.flatten().toSet()
        allBrokers.contains(2)
        allBrokers.contains(3)
    }

    void 'reassign for decommission - moves replicas off removed broker'() {
        given:
        def current = [[0, 1, 2], [1, 2, 0], [2, 0, 1]]
        when:
        def result = PartitionBalancer.reassignForDecommission(current, [1] as int[])
        then:
        result.size() == 3
        result.every { replicas -> replicas.every { it != 1 } }
        result.every { it.size() == 2 }
        result.every { it.toSet().size() == it.size() }
    }

    void 'reassign for decommission - remove all brokers throws'() {
        given:
        def current = [[0, 1], [1, 0]]
        when:
        PartitionBalancer.reassignForDecommission(current, [0, 1] as int[])
        then:
        thrown(AssertionError)
    }

    void 'reassign for decommission - single broker remaining'() {
        given:
        def current = [[0, 1, 2], [1, 2, 0]]
        when:
        def result = PartitionBalancer.reassignForDecommission(current, [1, 2] as int[])
        then:
        result.size() == 2
        result.every { it.every { it == 0 } }
    }

    void 'reassign for decommission - no removed brokers'() {
        given:
        def current = [[0, 1], [1, 0]]
        when:
        def result = PartitionBalancer.reassignForDecommission(current, [] as int[])
        then:
        result.size() == 2
    }

    void 'reassign for scale - non-contiguous broker IDs'() {
        given:
        def current = [[0], [2], [5], [0], [2], [5], [0], [2]]
        when:
        def result = PartitionBalancer.reassignForScale(current, [7] as int[])
        then:
        result.size() == 8
        result.every { it.size() == 1 }
        def allResultBrokers = result.collect { it[0] }.toSet()
        allResultBrokers.contains(7)
    }

    void 'reassign for decommission - non-contiguous broker IDs'() {
        given:
        def current = [[0, 2], [2, 5], [5, 0]]
        when:
        def result = PartitionBalancer.reassignForDecommission(current, [2] as int[])
        then:
        result.size() == 3
        result.every { replicas -> replicas.every { it != 2 } }
        result.every { it.toSet().size() == it.size() }
        def allBrokers = result.flatten().toSet()
        allBrokers.containsAll([0, 5])
    }

    void 'empty assignment inputs'() {
        expect:
        PartitionBalancer.reassignForScale([], [] as int[]) == []
        PartitionBalancer.reassignForDecommission([], [1] as int[]) == []
    }
}
