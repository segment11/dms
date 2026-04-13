# Kafka Manager Review Round 9

## Scope

- Review target: post-data-model implementation commits `ec04266`, `f9bafec`, `628b43e`, `f449365`
- Stage: post-core-engine implementation review
- Previous round: `doc/feat/kafka_manager/review_round8.md`

## Findings

1. High: `PartitionBalancer.reassignForScale()` ignores the actual `newBrokerIds` input and rebuilds the broker set as a dense range.

This only works if broker IDs are always contiguous and new IDs are always appended in order. If broker IDs contain gaps, or the caller passes explicit IDs, the method can assign partitions to brokers that do not exist and skip brokers that do.

References:
- `dms/src/km/PartitionBalancer.groovy:21`

2. High: `PartitionBalancer.reassignForDecommission()` can generate duplicate replicas on the same broker when the remaining broker count is smaller than the topic replication factor.

The current implementation and test both accept results like `[0, 0, 0]`, but Kafka replica assignments cannot place multiple replicas of one partition on the same broker. This path should fail fast, or require replication-factor reduction before decommission, instead of producing an invalid assignment.

References:
- `dms/src/km/PartitionBalancer.groovy:35`
- `dms/test/km/PartitionBalancerTest.groovy:82`

3. Medium: `PartitionBalancer.reassignForScale()` and `PartitionBalancer.reassignForDecommission()` both assume `currentAssignment` is non-empty.

For a cluster with zero topics, both methods throw while computing `max()` or `flatten()`. Scaling or decommissioning an empty cluster should be a valid no-op and return an empty assignment.

References:
- `dms/src/km/PartitionBalancer.groovy:22`
- `dms/src/km/PartitionBalancer.groovy:37`

## Summary

The data-model commits are in place, and the core-engine scaffolding is mostly aligned with the design, but the current `PartitionBalancer` behavior is not safe enough to treat Stage 2 as accepted.

Do not move to the next stage until the reassignment logic is corrected and covered by focused tests for:

- non-contiguous broker IDs during scale-up
- decommission when remaining brokers are fewer than replication factor
- empty topic assignment inputs
