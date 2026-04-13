package km

import groovy.transform.CompileStatic

@CompileStatic
class PartitionBalancer {

    static List<List<Integer>> assignReplicas(int brokerCount, int partitionCount, int replicationFactor) {
        assert replicationFactor <= brokerCount
        assert brokerCount > 0
        assert partitionCount >= 0
        assert replicationFactor >= 0

        List<List<Integer>> result = []
        for (int p = 0; p < partitionCount; p++) {
            List<Integer> replicas = []
            for (int r = 0; r < replicationFactor; r++) {
                replicas << (p + r) % brokerCount
            }
            result << replicas
        }
        result
    }

    static List<List<Integer>> reassignForScale(List<List<Integer>> currentAssignment, int[] newBrokerIds) {
        if (!currentAssignment && !newBrokerIds) return []

        Set<Integer> existingBrokers = (currentAssignment ? currentAssignment.flatten().toSet() : []) as Set<Integer>
        Set<Integer> newSet = newBrokerIds.toSet()
        List<Integer> allBrokers = (existingBrokers + newSet).sort() as List<Integer>

        if (!allBrokers) return []

        int partitionCount = currentAssignment ? currentAssignment.size() : 0
        int replicationFactor = currentAssignment && currentAssignment[0] ? currentAssignment[0].size() : 1
        int brokerCount = allBrokers.size()

        List<List<Integer>> result = []
        for (int p = 0; p < partitionCount; p++) {
            List<Integer> replicas = []
            int offset = 0
            while (replicas.size() < replicationFactor && offset < brokerCount * replicationFactor) {
                int candidate = allBrokers[(p + offset) % brokerCount]
                if (!(candidate in replicas)) {
                    replicas << candidate
                }
                offset++
            }
            result << replicas
        }
        result
    }

    static List<List<Integer>> reassignForDecommission(List<List<Integer>> currentAssignment, int[] removeBrokerIds) {
        if (!currentAssignment) return []

        Set<Integer> removed = removeBrokerIds.toSet()
        Set<Integer> allBrokers = currentAssignment.flatten().toSet() as Set<Integer>
        List<Integer> remaining = (allBrokers - removed).sort() as List<Integer>
        assert remaining.size() > 0: 'no remaining brokers after decommission'

        int partitionCount = currentAssignment.size()
        int replicationFactor = currentAssignment[0] ? currentAssignment[0].size() : 1

        if (remaining.size() < replicationFactor) {
            replicationFactor = remaining.size()
        }

        List<List<Integer>> result = []
        for (int p = 0; p < partitionCount; p++) {
            List<Integer> replicas = []
            int offset = 0
            while (replicas.size() < replicationFactor && offset < remaining.size() * replicationFactor) {
                int candidate = remaining[(p + offset) % remaining.size()]
                if (!(candidate in replicas)) {
                    replicas << candidate
                }
                offset++
            }
            result << replicas
        }
        result
    }
}
