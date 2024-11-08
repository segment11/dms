package common

import spock.lang.Specification

class PagerTest extends Specification {
    def 'test all'() {
        given:
        def pager = new Pager<String>(1, 10)
        pager.totalCount = 100
        pager.list = ['a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j']

        expect:
        pager.totalPage == 10
        pager.hasNext()
        !pager.hasPre()
        pager.start == 0
        pager.end == 10
        pager.toString() == '1/10 Total:100 Page Size:10 Page List:a,b,c,d,e,f,g,h,i,j,'

        when:
        pager.pageNum = 10
        pager.totalCount = 99
        then:
        pager.totalPage == 10
        !pager.hasNext()
        pager.hasPre()
        pager.start == 90
        pager.end == 99

        when:
        pager.list = null
        then:
        pager.toString() == '10/10 Total:99 Page Size:10'

        when:
        pager.pageNum = 1
        pager.totalCount = 10
        then:
        pager.end == 10
    }
}
