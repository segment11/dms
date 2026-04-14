# Kafka Manager Next Todo Daily

- Time: 2026-04-13 23:00:00 +0800
- Branch: `feat/kafka-manager` (rebased on main)
- Backend API stage: approved (review round 17), E2E test cases written
- Next works:
  - Implement G1: wire `/kafka/service/failover` endpoint with FailoverTask job chain
  - Implement G2: `/kafka/topic/alter` — increase partitions and update topic config via ZK
  - Implement G3+G7: `/kafka/topic/delete` — delete topic from ZK, add DeleteTopicTask
  - Implement G4: `/kafka/topic/reassign` — trigger REASSIGN_PARTITIONS job for single topic
  - Implement G5: `/kafka/service/update-config` — update broker dynamic config via ZK
  - Implement G6: `/kafka/snapshot/download` — serve snapshot zip file
  - Implement G8: service stop/start endpoints
  - After all gaps fixed: review round 18, then begin web UI stage
