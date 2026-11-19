package pl.databucket.server.service.data

import org.springframework.jdbc.core.RowMapper
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.transaction.PlatformTransactionManager
import pl.databucket.server.dto.DataGetDTO
import pl.databucket.server.entity.Bucket
import pl.databucket.server.entity.User
import pl.databucket.server.service.data.filter.LogicFilter
import spock.lang.Specification
import spock.lang.Subject

/**
 * Counting all the rows matching the rules reads every one of them, so a client that already knows the total (the
 * next page of the same rules) can get a page without it.
 */
class DataGetCountSpec extends Specification {

    NamedParameterJdbcTemplate jdbcTemplate = Mock()

    @Subject
    DataService dataService = new DataService(jdbcTemplate, Mock(PlatformTransactionManager))

    Bucket bucket = new Bucket(id: 7L, name: 'users')

    User user = Stub(User) {
        isAdminUser() >> true
        getUsername() >> 'admin1'
    }

    LogicFilter filter() {
        LogicFilter.of('admin1', new DataGetDTO(rules: [['$.country', '==', 'PL']]))
    }

    def "a page with count has the total of the matching rows"() {
        when:
        def result = dataService.getData(user, bucket, Optional.empty(), filter(), 2, 10, 'id', true)

        then:
        1 * jdbcTemplate.queryForObject({ it.contains('count(*)') }, _ as Map, Long.TYPE) >> 25L
        1 * jdbcTemplate.query(_ as String, _ as Map, _ as RowMapper) >> []
        result[ResultField.TOTAL] == 25L
    }

    def "a page without count isn't counted and has no total"() {
        when:
        def result = dataService.getData(user, bucket, Optional.empty(), filter(), 2, 10, 'id', false)

        then:
        0 * jdbcTemplate.queryForObject(*_)
        1 * jdbcTemplate.query(_ as String, _ as Map, _ as RowMapper) >> []
        !result.containsKey(ResultField.TOTAL)
    }

    def "a limit of 0 is the request for the count, so it is counted also without count"() {
        when:
        def result = dataService.getData(user, bucket, Optional.empty(), filter(), 1, 0, 'id', false)

        then:
        1 * jdbcTemplate.queryForObject({ it.contains('count(*)') }, _ as Map, Long.TYPE) >> 25L
        0 * jdbcTemplate.query(*_)
        result[ResultField.TOTAL] == 25L
    }

    def "without the parameter the page is counted, as before"() {
        when:
        def result = dataService.getData(user, bucket, Optional.empty(), filter(), 1, 10, 'id')

        then:
        1 * jdbcTemplate.queryForObject({ it.contains('count(*)') }, _ as Map, Long.TYPE) >> 3L
        1 * jdbcTemplate.query(_ as String, _ as Map, _ as RowMapper) >> []
        result[ResultField.TOTAL] == 3L
    }
}
