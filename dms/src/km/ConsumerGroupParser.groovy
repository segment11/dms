package km

import groovy.transform.CompileStatic

@CompileStatic
class ConsumerGroupParser {

    static List<String> parseGroupList(String output) {
        if (!output) {
            return []
        }
        output.readLines().findAll { line ->
            def t = line.trim()
            t && !t.startsWith('Note:') && !t.startsWith('Warning:') && !t.startsWith('To see') &&
                    !t.contains('does not exist') && !t.contains('--bootstrap-server')
        }
    }

    static Map<String, Object> parseGroupDescribe(String output, String groupId) {
        List<String> lines = dataLines(output)

        if (lines.size() < 2) {
            return [groupId: groupId, partitions: []]
        }

        List<String> headers = lines[0].trim().split(/\s+/).toList()
        List<Map> partitions = []
        for (int i = 1; i < lines.size(); i++) {
            List<String> cols = lines[i].trim().split(/\s+/).toList()
            if (cols.size() >= headers.size()) {
                Map entry = [:]
                headers.eachWithIndex { String header, int j ->
                    entry[header] = cols[j]
                }
                partitions << entry
            }
        }
        [groupId: groupId, partitions: partitions]
    }

    static Map<String, Object> parseGroupLag(String output, String groupId) {
        List<String> lines = dataLines(output)

        if (lines.size() < 2) {
            return [groupId: groupId, totalLag: -1L, topics: [], raw: lines]
        }

        List<String> headers = lines[0].trim().split(/\s+/).toList()
        int topicIdx = headers.indexOf('TOPIC')
        int lagIdx = headers.indexOf('LAG')
        int offsetIdx = headers.indexOf('CURRENT-OFFSET')
        int logEndIdx = headers.indexOf('LOG-END-OFFSET')

        if (topicIdx < 0 || lagIdx < 0) {
            return [groupId: groupId, totalLag: -1L, topics: [], raw: lines]
        }

        long totalLag = 0L
        Map<String, Long> topicLag = [:]
        Map<String, Long> topicLogEnd = [:]
        Map<String, Long> topicCurrent = [:]
        Map<String, Integer> topicPartitionCount = [:]

        for (int i = 1; i < lines.size(); i++) {
            List<String> cols = lines[i].trim().split(/\s+/).toList()
            if (cols.size() <= Math.max(topicIdx, lagIdx)) {
                continue
            }
            def topic = cols[topicIdx]
            def lag = (cols[lagIdx] == '-') ? 0L : (cols[lagIdx] as long)
            totalLag += lag
            topicLag[topic] = (topicLag[topic] ?: 0L) + lag
            if (logEndIdx >= 0 && cols[logEndIdx] != '-') {
                topicLogEnd[topic] = (topicLogEnd[topic] ?: 0L) + (cols[logEndIdx] as long)
            }
            if (offsetIdx >= 0 && cols[offsetIdx] != '-') {
                topicCurrent[topic] = (topicCurrent[topic] ?: 0L) + (cols[offsetIdx] as long)
            }
            topicPartitionCount[topic] = (topicPartitionCount[topic] ?: 0) + 1
        }

        List<Map<String, Object>> topics = []
        topicLag.each { String topic, Long lag ->
            Map<String, Object> entry = new LinkedHashMap<String, Object>()
            entry['topic'] = topic
            entry['lag'] = lag
            entry['logEndOffset'] = topicLogEnd[topic] ?: 0L
            entry['currentOffset'] = topicCurrent[topic] ?: 0L
            entry['partitionCount'] = topicPartitionCount[topic]
            topics << entry
        }

        [groupId: groupId, totalLag: totalLag, topics: topics]
    }

    private static List<String> dataLines(String output) {
        if (!output) {
            return []
        }
        output.readLines().findAll { line ->
            def t = line.trim()
            t && !t.startsWith('Note:') && !t.startsWith('Warning:') && !t.startsWith('To see')
        }
    }
}
