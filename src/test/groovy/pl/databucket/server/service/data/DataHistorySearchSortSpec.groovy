package pl.databucket.server.service.data

import pl.databucket.server.exception.InvalidRuleException
import spock.lang.Specification
import spock.lang.Unroll

class DataHistorySearchSortSpec extends Specification {

    @Unroll
    def "should sort the history by '#sort'"() {
        expect:
        DataHistorySearchService.orderBy(sort) as List == expected

        where:
        sort                || expected
        null                || ["id", "ASC"]
        "id"                || ["id", "ASC"]
        "desc(id)"          || ["id", "DESC"]
        "asc(modifiedAt)"   || ["modified_at", "ASC"]
        " desc( dataId ) "  || ["data_id", "DESC"]
    }

    @Unroll
    def "should refuse to sort the history by '#sort'"() {
        when:
        DataHistorySearchService.orderBy(sort)

        then:
        thrown(InvalidRuleException)

        where:
        sort << ["owner", "desc(properties)", "random", "id; drop table x", "desc(\$.a)"]
    }
}
