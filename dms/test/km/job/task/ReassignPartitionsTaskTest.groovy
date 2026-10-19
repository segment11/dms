package km.job.task

import spock.lang.Specification

class ReassignPartitionsTaskTest extends Specification {

    void 'filter topic names returns all when no csv filter given'() {
        expect:
        ReassignPartitionsTask.filterTopicNames(['a', 'b', 'c'], null) == ['a', 'b', 'c']
        ReassignPartitionsTask.filterTopicNames(['a', 'b', 'c'], '') == ['a', 'b', 'c']
    }

    void 'filter topic names intersects with csv filter and trims'() {
        expect:
        ReassignPartitionsTask.filterTopicNames(['a', 'b', 'c'], 'a,c') == ['a', 'c']
        ReassignPartitionsTask.filterTopicNames(['a', 'b', 'c'], ' a , c ') == ['a', 'c']
    }

    void 'filter topic names empty when nothing matches'() {
        expect:
        ReassignPartitionsTask.filterTopicNames(['a', 'b', 'c'], 'x,y') == []
    }
}
