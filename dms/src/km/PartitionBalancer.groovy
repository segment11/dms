package km

import groovy.transform.CompileStatic

@CompileStatic
class PartitionBalancer {

    static List<List<Integer>> assignReplicas(int brokerCount, int partitionCount, int replicationFactor) {
        assert replicationFactor <= brokerCount
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
        int maxBroker = currentAssignment.collect { it.max() }.max()
        int totalBrokerCount = maxBroker + 1 + newBrokerIds.length
        int partitionCount = currentAssignment.size()
        int replicationFactor = currentAssignment.size() == 0 ? 1 : currentAssignment[0].size()
        List<List<Integer>> result = assignReplicas(totalBrokerCount, partitionCount, replicationFactor)
        if (newBrokerIds.length > 0) {
            Set<Integer> newSet = newBrokerIds.toSet()
            boolean hasNew = result.any { replicas -> replicas.any { newSet.contains(it) } }
            assert hasNew
        }
        result
    }

    static List<List<Integer>> reassignForDecommission(List<List<Integer>> currentAssignment, int[] removeBrokerIds) {
        Set<Integer> removed = removeBrokerIds.toSet()
        Set<Integer> allBrokers = currentAssignment.flatten().toSet() as Set<Integer>
        List<Integer> remaining = (allBrokers - removed).sort() as List<Integer>
        assert remaining.size() > 0
        int partitionCount = currentAssignment.size()
        int replicationFactor = currentAssignment.size() == 0 ? 1 : currentAssignment[0].size()
        List<List<Integer>> result = []
        for (int p = 0; p < partitionCount; p++) {
            List<Integer> replicas = []
            for (int r = 0; r < replicationFactor; r++) {
                replicas << remaining[(p + r) % remaining.size()]
            }
            result << replicas
        }
        result
    }
}
