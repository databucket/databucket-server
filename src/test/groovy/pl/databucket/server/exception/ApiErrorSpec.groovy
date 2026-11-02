package pl.databucket.server.exception

import org.postgresql.util.PSQLException
import org.postgresql.util.ServerErrorMessage
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.jdbc.BadSqlGrammarException
import org.springframework.jdbc.UncategorizedSQLException
import org.springframework.http.HttpStatus
import spock.lang.Specification
import spock.lang.Unroll

import java.sql.BatchUpdateException

/**
 * Database errors reach the client without the SQL statement Spring puts into the message.
 */
class ApiErrorSpec extends Specification {

    static final String SQL = 'PreparedStatementCallback; SQL [INSERT INTO "x-bucket-5" (tag_id, properties) VALUES (?, ?::jsonb)]; '

    static PSQLException psql(String state, String message, String detail = null) {
        String fields = "SERROR\u0000C${state}\u0000M${message}\u0000" + (detail != null ? "D${detail}\u0000" : "")
        new PSQLException(new ServerErrorMessage(fields))
    }

    @Unroll
    def "should translate #description"() {
        when:
        ApiError error = ApiError.of(exception, chosen)

        then:
        error.message() == message
        error.status() == status
        !error.message().contains("INSERT")

        where:
        description                  | exception                                                                                                                                                      | chosen                         || message                                                                              | status
        "a missing tag"              | new DataIntegrityViolationException(SQL + "ERROR: ...", psql("23503", 'insert or update on table "x-bucket-5" violates foreign key constraint "fk_tag_id"', 'Key (tag_id)=(99) is not present in table "tags".')) | HttpStatus.NOT_ACCEPTABLE || "The tag 99 doesn't exist"                                                           | HttpStatus.NOT_ACCEPTABLE
        "a missing tag in a batch"   | new DataIntegrityViolationException(SQL, batch(psql("23503", 'violates foreign key constraint "fk_tag_id"', 'Key (tag_id)=(7) is not present in table "tags".'))) | HttpStatus.INTERNAL_SERVER_ERROR || "The tag 7 doesn't exist"                                                    | HttpStatus.BAD_REQUEST
        "a value of a wrong type"    | new UncategorizedSQLException("rules", "SELECT ...", psql("22P02", 'invalid input syntax for type numeric: "abc"'))                                        | HttpStatus.INTERNAL_SERVER_ERROR || 'Invalid value: invalid input syntax for type numeric: "abc"'                 | HttpStatus.BAD_REQUEST
        "an empty property"          | new DataIntegrityViolationException(SQL, psql("22023", "cannot cast jsonb null to type numeric"))                                                             | HttpStatus.NOT_ACCEPTABLE      || "Failed to operate on an empty property"                                            | HttpStatus.NOT_ACCEPTABLE
        "a rule for other types"     | new BadSqlGrammarException("rules", "SELECT ...", psql("42883", "operator does not exist: jsonb > integer"))                                                | HttpStatus.INTERNAL_SERVER_ERROR || "The rules can't be applied to the data: operator does not exist: jsonb > integer" | HttpStatus.BAD_REQUEST
        "a conflict"                 | new DataIntegrityViolationException(SQL, psql("23505", "duplicate key value violates unique constraint", "Key (name)=(x) already exists."))                 | HttpStatus.INTERNAL_SERVER_ERROR || "The data conflicts with other data: Key (name)=(x) already exists."           | HttpStatus.CONFLICT
        "a cancelled query"          | new UncategorizedSQLException("rules", "SELECT ...", psql("57014", "canceling statement due to statement timeout"))                                        | HttpStatus.INTERNAL_SERVER_ERROR || "The query took too long and was cancelled"                                      | HttpStatus.SERVICE_UNAVAILABLE
        "another database error"     | new UncategorizedSQLException("x", "SELECT ...", psql("53300", "too many connections"))                                                                     | HttpStatus.INTERNAL_SERVER_ERROR || "Database error"                                                                 | HttpStatus.INTERNAL_SERVER_ERROR
    }

    def "should keep the message of other exceptions"() {
        expect:
        ApiError.of(new ItemNotFoundException(pl.databucket.server.entity.Tag, 5L), HttpStatus.NOT_FOUND).message().contains("5")
        ApiError.of(new IllegalArgumentException("Wrong limit"), HttpStatus.BAD_REQUEST) == new ApiError("Wrong limit", HttpStatus.BAD_REQUEST)
        ApiError.of(new NullPointerException(), HttpStatus.INTERNAL_SERVER_ERROR).message() == "Unexpected error: NullPointerException"
    }

    static BatchUpdateException batch(PSQLException next) {
        BatchUpdateException batch = new BatchUpdateException('Batch entry 0 INSERT INTO "x-bucket-5" ... was aborted', "23503", new int[0])
        batch.setNextException(next)
        batch
    }
}
