package pl.databucket.server.service

import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import spock.lang.Specification
import spock.lang.Subject

class ProjectContentServiceSpec extends Specification {

    NamedParameterJdbcTemplate namedJdbc = Mock()
    JdbcTemplate jdbc = Mock()

    @Subject
    ProjectContentService service = new ProjectContentService()

    def setup() {
        service.jdbcTemplate = namedJdbc
        namedJdbc.getJdbcTemplate() >> jdbc
    }

    def "should drop the tables of all buckets of the project, also the deleted ones, and remove its objects"() {
        given: "the project has two buckets"
        namedJdbc.queryForList({ it.startsWith("SELECT bucket_id FROM buckets WHERE project_id = :p") && !it.contains("deleted") },
                _ as MapSqlParameterSource, Long) >> [7L, 9L]
        def statements = []
        namedJdbc.update(_ as String, _ as MapSqlParameterSource) >> { String sql, MapSqlParameterSource p ->
            assert p.getValue("p") == 5
            statements << sql
            0
        }

        when:
        service.clearProject(5)

        then: "the data and history tables are dropped"
        1 * jdbc.execute('DROP TABLE IF EXISTS "x-bucket-7-h"')
        1 * jdbc.execute('DROP TABLE IF EXISTS "x-bucket-7"')
        1 * jdbc.execute('DROP TABLE IF EXISTS "x-bucket-9-h"')
        1 * jdbc.execute('DROP TABLE IF EXISTS "x-bucket-9"')

        and: "the links go before the objects, and the objects in the order of their foreign keys"
        def firstObject = statements.findIndexOf { it == "DELETE FROM views WHERE project_id = :p" }
        statements.findIndexOf { it.startsWith("DELETE FROM users_teams") } < firstObject
        statements.findIndexOf { it.startsWith("DELETE FROM views_buckets") } < firstObject
        def order = ["views", "tasks", "buckets", "data_filters", "data_columns", "data_classes", "teams"]
                .collect { table -> statements.indexOf("DELETE FROM " + table + " WHERE project_id = :p") }
        order.every { it >= 0 }
        order == order.toSorted()
    }

    def "should leave the users of the project"() {
        given:
        namedJdbc.queryForList(_ as String, _ as MapSqlParameterSource, Long) >> []
        def statements = []
        namedJdbc.update(_ as String, _ as MapSqlParameterSource) >> { String sql, MapSqlParameterSource p -> statements << sql; 0 }

        when:
        service.clearProject(5)

        then:
        statements.every { !it.contains("users_projects") && !it.startsWith("DELETE FROM users ") && !it.contains("projects WHERE") }
    }
}
