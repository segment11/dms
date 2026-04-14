package ctrl.kafka

import km.CuratorPoolHolder
import model.KmServiceDTO
import org.segment.d.json.DefaultJsonTransformer
import org.segment.web.handler.ChainHandler
import org.slf4j.LoggerFactory

def h = ChainHandler.instance

def log = LoggerFactory.getLogger(this.getClass())

h.group('/kafka/consumer') {
    h.get('/list') { req, resp ->
        def serviceIdStr = req.param('serviceId')
        assert serviceIdStr
        def serviceId = serviceIdStr as int

        def service = new KmServiceDTO(id: serviceId).one()
        if (!service) {
            resp.halt(404, 'service not found')
        }

        if (service.status != KmServiceDTO.Status.running) {
            resp.halt(409, 'service must be running')
        }

        def connectionString = service.zkConnectString + service.zkChroot
        def client = CuratorPoolHolder.instance.create(connectionString)
        try {
            def consumersPath = '/consumers'
            if (client.checkExists().forPath(consumersPath) == null) {
                return [list: []]
            }

            List<String> groups = client.getChildren().forPath(consumersPath)
            [serviceId: serviceId, list: groups]
        } catch (Exception e) {
            log.error('list consumer groups error', e)
            resp.halt(500, 'list consumer groups error: ' + e.message)
        }
    }

    h.get('/one') { req, resp ->
        def serviceIdStr = req.param('serviceId')
        def groupId = req.param('groupId')
        assert serviceIdStr && groupId
        def serviceId = serviceIdStr as int

        def service = new KmServiceDTO(id: serviceId).one()
        if (!service) {
            resp.halt(404, 'service not found')
        }

        if (service.status != KmServiceDTO.Status.running) {
            resp.halt(409, 'service must be running')
        }

        def connectionString = service.zkConnectString + service.zkChroot
        def client = CuratorPoolHolder.instance.create(connectionString)
        try {
            def json = new DefaultJsonTransformer()

            def groupPath = '/consumers/' + groupId
            if (client.checkExists().forPath(groupPath) == null) {
                resp.halt(404, 'consumer group not found')
            }

            List<String> memberIds = []
            def idsPath = groupPath + '/ids'
            if (client.checkExists().forPath(idsPath) != null) {
                memberIds = client.getChildren().forPath(idsPath)
            }

            List<String> topicNames = []
            def offsetsPath = groupPath + '/offsets'
            if (client.checkExists().forPath(offsetsPath) != null) {
                topicNames = client.getChildren().forPath(offsetsPath)
            }

            List partitionOffsets = []
            for (String topicName : topicNames) {
                def topicOffsetPath = offsetsPath + '/' + topicName
                if (client.checkExists().forPath(topicOffsetPath) == null) {
                    continue
                }
                List<String> partitionIds = client.getChildren().forPath(topicOffsetPath)
                for (String partitionId : partitionIds) {
                    def partitionOffsetPath = topicOffsetPath + '/' + partitionId
                    long offset = -1L
                    if (client.checkExists().forPath(partitionOffsetPath) != null) {
                        def data = new String(client.getData().forPath(partitionOffsetPath), 'UTF-8')
                        offset = data as long
                    }
                    partitionOffsets << [
                            topic     : topicName,
                            partition : partitionId as int,
                            offset    : offset,
                    ]
                }
            }

            List owners = []
            def ownersPath = groupPath + '/owners'
            if (client.checkExists().forPath(ownersPath) != null) {
                List<String> ownerTopics = client.getChildren().forPath(ownersPath)
                for (String topicName : ownerTopics) {
                    def topicOwnerPath = ownersPath + '/' + topicName
                    if (client.checkExists().forPath(topicOwnerPath) == null) {
                        continue
                    }
                    List<String> partitionOwners = client.getChildren().forPath(topicOwnerPath)
                    for (String partitionId : partitionOwners) {
                        def partitionOwnerPath = topicOwnerPath + '/' + partitionId
                        String owner = ''
                        if (client.checkExists().forPath(partitionOwnerPath) != null) {
                            owner = new String(client.getData().forPath(partitionOwnerPath), 'UTF-8')
                        }
                        owners << [
                                topic    : topicName,
                                partition: partitionId as int,
                                owner    : owner,
                        ]
                    }
                }
            }

            [serviceId: serviceId, groupId: groupId, members: memberIds, offsets: partitionOffsets, owners: owners]
        } catch (Exception e) {
            log.error('describe consumer group error', e)
            resp.halt(500, 'describe consumer group error: ' + e.message)
        }
    }

    h.get('/lag') { req, resp ->
        def serviceIdStr = req.param('serviceId')
        def groupId = req.param('groupId')
        assert serviceIdStr && groupId
        def serviceId = serviceIdStr as int

        def service = new KmServiceDTO(id: serviceId).one()
        if (!service) {
            resp.halt(404, 'service not found')
        }

        if (service.status != KmServiceDTO.Status.running) {
            resp.halt(409, 'service must be running')
        }

        def connectionString = service.zkConnectString + service.zkChroot
        def client = CuratorPoolHolder.instance.create(connectionString)
        try {
            def offsetsPath = '/consumers/' + groupId + '/offsets'
            if (client.checkExists().forPath(offsetsPath) == null) {
                return [serviceId: serviceId, groupId: groupId, topics: [], note: 'ZK-based lag requires kafka-consumer-groups.sh for actual LOG_END_OFFSET; use kafka_exporter for new-style consumers']
            }

            List<String> topicNames = client.getChildren().forPath(offsetsPath)
            List<Map> topicSummaries = []

            for (String topicName : topicNames) {
                def topicOffsetPath = offsetsPath + '/' + topicName
                if (client.checkExists().forPath(topicOffsetPath) == null) {
                    continue
                }

                List<String> partitionIds = client.getChildren().forPath(topicOffsetPath)
                long lastOffset = 0L
                for (String partitionId : partitionIds) {
                    def partitionOffsetPath = topicOffsetPath + '/' + partitionId
                    if (client.checkExists().forPath(partitionOffsetPath) != null) {
                        def offsetData = new String(client.getData().forPath(partitionOffsetPath), 'UTF-8')
                        lastOffset += offsetData as long
                    }
                }

                topicSummaries << [
                        topic          : topicName,
                        partitionCount: partitionIds.size(),
                        lastOffset     : lastOffset,
                ]
            }

            [serviceId: serviceId, groupId: groupId, topics: topicSummaries,
             note     : 'ZK stores consumer offsets only, not LOG_END_OFFSET. Use kafka_exporter for actual lag.']
        } catch (Exception e) {
            log.error('consumer lag error', e)
            resp.halt(500, 'consumer lag error: ' + e.message)
        }
    }
}
