package pl.databucket.server.dto

import jakarta.validation.Validation
import jakarta.validation.Validator
import spock.lang.Specification
import spock.lang.Unroll

class TagDtoValidationSpec extends Specification {

    Validator validator = Validation.buildDefaultValidatorFactory().getValidator()

    def "should pass validation with valid data"() {
        given: "a valid TagDto"
        def tagDto = new TagDto(
                name: "valid-tag",
                description: "A valid description"
        )

        when: "validating the DTO"
        def violations = validator.validate(tagDto)

        then: "no validation errors"
        violations.isEmpty()
    }

    def "should fail validation when name is empty"() {
        given: "a TagDto with empty name"
        def tagDto = new TagDto(
                name: "",
                description: "Description"
        )

        when: "validating the DTO"
        def violations = validator.validate(tagDto)

        then: "validation fails on name"
        !violations.isEmpty()
        violations.any { it.propertyPath.toString() == "name" }
    }

    def "should fail validation when name is too long"() {
        given: "a TagDto with very long name (>30 chars, Constants.NAME_MAX = 30)"
        def tagDto = new TagDto(
                name: "a" * 50,  // 50 characters - exceeds limit of 30
                description: "Description"
        )

        when: "validating the DTO"
        def violations = validator.validate(tagDto)

        then: "validation fails on name size"
        !violations.isEmpty()
        violations.any { it.propertyPath.toString() == "name" }
    }

    def "should fail validation when description is too long"() {
        given: "a TagDto with very long description"
        def tagDto = new TagDto(
                name: "tag",
                description: "a" * 1000  // Assuming Constants.DESCRIPTION_MAX < 1000
        )

        when: "validating the DTO"
        def violations = validator.validate(tagDto)

        then: "validation fails on description size"
        !violations.isEmpty()
        violations.any { it.propertyPath.toString() == "description" }
    }

    @Unroll
    def "should validate name with length #length"() {
        given: "a TagDto with name of specific length"
        def tagDto = new TagDto(
                name: "a" * length,
                description: "Description"
        )

        when: "validating the DTO"
        def violations = validator.validate(tagDto)

        then: "validation result matches expectation"
        violations.isEmpty() == isValid

        where: "testing different name lengths (NAME_MAX = 30)"
        length || isValid
        0      || false    // empty
        1      || true     // minimum valid
        10     || true     // normal
        30     || true     // max valid (NAME_MAX = 30)
        31     || false    // exceeds max
        50     || false    // way too long
    }

    def "should allow null description"() {
        given: "a TagDto with null description"
        def tagDto = new TagDto(
                name: "tag",
                description: null
        )

        when: "validating the DTO"
        def violations = validator.validate(tagDto)

        then: "validation passes (description is optional)"
        violations.isEmpty()
    }

    def "should allow null buckets and classes IDs"() {
        given: "a TagDto with null collections"
        def tagDto = new TagDto(
                name: "tag",
                description: "Description",
                bucketsIds: null,
                classesIds: null
        )

        when: "validating the DTO"
        def violations = validator.validate(tagDto)

        then: "validation passes"
        violations.isEmpty()
    }

    def "should allow empty buckets and classes IDs collections"() {
        given: "a TagDto with empty collections"
        def tagDto = new TagDto(
                name: "tag",
                description: "Description",
                bucketsIds: [] as Set,
                classesIds: [] as Set
        )

        when: "validating the DTO"
        def violations = validator.validate(tagDto)

        then: "validation passes"
        violations.isEmpty()
    }

    def "should allow valid buckets and classes IDs"() {
        given: "a TagDto with valid ID collections"
        def tagDto = new TagDto(
                name: "tag",
                description: "Description",
                bucketsIds: [1L, 2L, 3L] as Set,
                classesIds: [10L, 20L] as Set
        )

        when: "validating the DTO"
        def violations = validator.validate(tagDto)

        then: "validation passes"
        violations.isEmpty()
    }
}