package pl.databucket.server.service.data

import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.transaction.PlatformTransactionManager
import pl.databucket.server.dto.DataGetDTO
import pl.databucket.server.entity.Bucket
import pl.databucket.server.entity.User
import pl.databucket.server.exception.DataNotFoundException
import spock.lang.Specification
import spock.lang.Subject
import spock.lang.Unroll

/**
 * The history of a data row keeps the whole properties of every version, so in a bucket with protected data a user
 * below ADMIN sees it only for a row reserved by them, as the data itself.
 */
class DataHistoryProtectionSpec extends Specification {

    NamedParameterJdbcTemplate jdbcTemplate = Mock()

    @Subject
    DataService dataService = new DataService(jdbcTemplate, Mock(PlatformTransactionManager))

    Bucket bucket(boolean protectedData) {
        new Bucket(id: 7L, name: 'users', protectedData: protectedData)
    }

    User user(boolean admin) {
        Stub(User) {
            isAdminUser() >> admin
            getUsername() >> 'member1'
        }
    }

    @Unroll
    def "the visibility check of a row requires its reservation by the user: #expected (protected: #protectedData, admin: #admin)"() {
        given:
        String sql = null
        Map<String, Object> params = null

        when:
        dataService.canSeeData(user(admin), bucket(protectedData), 42L)

        then:
        1 * jdbcTemplate.queryForObject(_ as String, _ as Map, Long) >> { String s, Map p, Class c -> sql = s; params = p; 1L }
        sql.contains('"x-bucket-7"')
        params.containsValue(42L)
        params.containsValue('member1') == expected

        where:
        protectedData | admin || expected
        true          | false || true
        true          | true  || false
        false         | false || false
    }

    def "the history of a row not reserved by the member is not read"() {
        when:
        dataService.getDataHistory(user(false), bucket(true), 42L)

        then:
        1 * jdbcTemplate.queryForObject(_ as String, _ as Map, Long) >> 0L
        0 * jdbcTemplate.queryForList(*_)
        thrown(DataNotFoundException)
    }

    def "the properties in the history of a row not reserved by the member are not read"() {
        when:
        dataService.getDataHistoryProperties(user(false), bucket(true), 42L, [1L, 2L])

        then:
        1 * jdbcTemplate.queryForObject(_ as String, _ as Map, Long) >> 0L
        0 * jdbcTemplate.queryForList(*_)
        thrown(DataNotFoundException)
    }

    def "the free rows are counted without the owner, so also a member of a bucket with protected data sees how many can be reserved"() {
        given:
        def filter = DataFilters.of('member1', new DataGetDTO(rules: [['$.country', '==', 'PL']]), '2')
        String sql = null
        Map<String, Object> params = null

        when:
        def count = dataService.countFreeData(bucket(true), filter)

        then:
        1 * jdbcTemplate.queryForObject(_ as String, _ as Map, Long) >> { String s, Map p, Class c -> sql = s; params = p; 5L }
        count == 5
        sql.contains('reserved')
        params.containsValue(false)
        !params.containsValue('member1')
        !sql.contains('reserved_by')
    }

    def "the history of a row reserved by the member is read"() {
        when:
        def history = dataService.getDataHistory(user(false), bucket(true), 42L)

        then:
        1 * jdbcTemplate.queryForObject(_ as String, _ as Map, Long) >> 1L
        1 * jdbcTemplate.queryForList({ it.contains('"x-bucket-7-h"') }, _ as Map) >> []
        history == []
    }
}
