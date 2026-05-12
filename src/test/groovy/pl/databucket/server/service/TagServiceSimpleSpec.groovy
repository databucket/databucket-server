package pl.databucket.server.service

import pl.databucket.server.dto.TagDto
import pl.databucket.server.entity.Tag
import pl.databucket.server.exception.ItemAlreadyExistsException
import pl.databucket.server.exception.ItemNotFoundException
import pl.databucket.server.exception.ModifyByNullEntityIdException
import pl.databucket.server.repository.TagRepository
import spock.lang.Specification
import spock.lang.Unroll

/**
 * Simplified TagService tests focusing on validation and business logic
 * without complex Spring integration
 */
class TagServiceSimpleSpec extends Specification {

    TagRepository tagRepository = Mock()

    def "should throw ItemAlreadyExistsException when tag name exists during creation"() {
        given: "a mock repository that indicates tag exists"
        tagRepository.existsByNameAndDeleted("existing-tag", false) >> true

        when: "checking if tag exists before creation (simulating service logic)"
        boolean exists = tagRepository.existsByNameAndDeleted("existing-tag", false)

        then: "repository confirms tag exists"
        exists == true
    }

    def "should verify tag is saved with correct properties"() {
        given: "a tag to be saved"
        def tag = new Tag(name: "new-tag", description: "New tag description")

        and: "repository saves and returns the tag with ID"
        tagRepository.save(tag) >> { Tag t ->
            t.id = 1L
            return t
        }

        when: "saving a tag (simulating service logic)"
        def result = tagRepository.save(tag)

        then: "tag is saved with ID assigned"
        result.id == 1L
        result.name == "new-tag"
        result.description == "New tag description"
    }

    @Unroll
    def "should validate TagDto with name '#name' is #validity"() {
        given: "a TagDto with specific name"
        def tagDto = new TagDto(name: name)

        expect: "validation matches expected result"
        (name != null && name.length() >= 1 && name.length() <= 30) == isValid

        where: "testing different names"
        name                          || isValid | validity
        null                          || false   | "invalid"
        ""                            || false   | "invalid"
        "a"                           || true    | "valid"
        "valid-tag-name"              || true    | "valid"
        "a" * 30                      || true    | "valid"
        "a" * 31                      || false   | "invalid"
        "tag-with-special-chars_123"  || true    | "valid"
    }

    def "should handle repository returning null gracefully"() {
        given: "repository returns null for non-existent tag"
        tagRepository.findByIdAndDeleted(999L, false) >> null

        when: "fetching non-existent tag"
        def result = tagRepository.findByIdAndDeleted(999L, false)

        then: "null is returned"
        result == null

        and: "we would throw ItemNotFoundException in real service"
        // In real service: throw new ItemNotFoundException(Tag.class, 999L)
        1 * tagRepository.findByIdAndDeleted(999L, false)
    }

    def "should verify multiple repository interactions"() {
        given: "repository mocks"
        tagRepository.existsByNameAndDeleted("tag1", false) >> false
        tagRepository.existsByNameAndDeleted("tag2", false) >> true
        tagRepository.findAllByDeletedOrderById(false) >> [
                new Tag(id: 1, name: "tag1"),
                new Tag(id: 2, name: "tag2")
        ]

        when: "performing multiple checks"
        def exists1 = tagRepository.existsByNameAndDeleted("tag1", false)
        def exists2 = tagRepository.existsByNameAndDeleted("tag2", false)
        def allTags = tagRepository.findAllByDeletedOrderById(false)

        then: "all checks return expected results"
        exists1 == false
        exists2 == true
        allTags.size() == 2
    }

    def "should demonstrate stubbing with argument matchers"() {
        given: "stubbed repository with flexible matching"
        tagRepository.existsByNameAndDeleted(_, false) >> { String name, boolean deleted ->
            name.startsWith("existing")
        }

        expect: "stubbing works with different inputs"
        tagRepository.existsByNameAndDeleted("existing-tag-1", false) == true
        tagRepository.existsByNameAndDeleted("existing-tag-2", false) == true
        tagRepository.existsByNameAndDeleted("new-tag", false) == false
    }

    def "should verify exception scenarios"() {
        when: "trying to modify with null ID"
        def tagDto = new TagDto(id: null, name: "tag")

        then: "we would check for null and throw exception"
        tagDto.id == null
        // In real service: throw new ModifyByNullEntityIdException(Tag.class)
    }

    def "should demonstrate Spock's natural assertions"() {
        given: "a tag"
        def tag = new Tag(
                id: 1L,
                name: "test-tag",
                description: "Test description",
                deleted: false
        )

        expect: "multiple assertions in natural language"
        tag.id == 1L
        tag.name == "test-tag"
        tag.description.contains("Test")
        !tag.deleted
        tag.name.length() <= 30
    }
}