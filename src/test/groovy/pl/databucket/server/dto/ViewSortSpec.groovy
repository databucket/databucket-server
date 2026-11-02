package pl.databucket.server.dto

import jakarta.validation.Validation
import jakarta.validation.Validator
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Unroll

// the default sort of a view, in the format of the sort parameter of the API
class ViewSortSpec extends Specification {

    @Shared
    Validator validator = Validation.buildDefaultValidatorFactory().validator

    @Unroll
    def "should accept the sort #sort"() {
        expect:
        validator.validateProperty(new ViewDto(sort: sort), "sort").isEmpty()

        where:
        sort << [null, "asc(id)", "desc(modifiedAt)", "asc(tagId)", "desc(reserved)", "asc(owner)", "asc(createdBy)",
                 "asc(\$.lastName)", "desc(\$.contact.email)", "asc(\$.scenarios[0].name)"]
    }

    @Unroll
    def "should refuse the sort #sort"() {
        expect:
        !validator.validateProperty(new ViewDto(sort: sort), "sort").isEmpty()

        where:
        sort << ["id", "desc(name)", "up(id)", "asc()", "asc(\$.)", "asc(\$.a') OR 1=1 --)", "desc(RANDOM())", "asc(\$.a)x"]
    }
}
