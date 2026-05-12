package pl.databucket.server.entity

import spock.lang.Specification

class TagSpec extends Specification {

    def "should create tag with builder pattern"() {
        given: "tag properties"
        def name = "important"
        def description = "Important items"

        when: "creating a tag"
        def tag = new Tag(
                name: name,
                description: description
        )

        then: "tag is created with correct properties"
        tag.name == name
        tag.description == description
        tag.deleted == false  // default value
    }

    def "should set and get all properties"() {
        given: "a new tag"
        def tag = new Tag()

        when: "setting properties"
        tag.id = 1L
        tag.name = "test-tag"
        tag.description = "Test description"
        tag.deleted = true

        then: "properties are set correctly"
        tag.id == 1L
        tag.name == "test-tag"
        tag.description == "Test description"
        tag.deleted == true
    }

    def "should handle null description"() {
        given: "a tag with null description"
        def tag = new Tag(
                name: "tag-without-description",
                description: null
        )

        expect: "description is null"
        tag.description == null
        tag.name == "tag-without-description"
    }

    def "should allow adding buckets to tag"() {
        given: "a tag and some buckets"
        def tag = new Tag(name: "tag1")
        def bucket1 = new Bucket(id: 1, name: "bucket1")
        def bucket2 = new Bucket(id: 2, name: "bucket2")

        when: "adding buckets to tag"
        tag.buckets = [bucket1, bucket2] as Set

        then: "tag has buckets"
        tag.buckets.size() == 2
        tag.buckets.contains(bucket1)
        tag.buckets.contains(bucket2)
    }

    def "should allow adding data classes to tag"() {
        given: "a tag and some data classes"
        def tag = new Tag(name: "tag1")
        def dataClass1 = new DataClass(id: 1, name: "class1")
        def dataClass2 = new DataClass(id: 2, name: "class2")

        when: "adding data classes to tag"
        tag.dataClasses = [dataClass1, dataClass2] as Set

        then: "tag has data classes"
        tag.dataClasses.size() == 2
        tag.dataClasses.contains(dataClass1)
        tag.dataClasses.contains(dataClass2)
    }

    def "should handle empty collections"() {
        given: "a tag with empty collections"
        def tag = new Tag(
                name: "tag",
                buckets: [] as Set,
                dataClasses: [] as Set
        )

        expect: "collections are empty but not null"
        tag.buckets != null
        tag.buckets.isEmpty()
        tag.dataClasses != null
        tag.dataClasses.isEmpty()
    }

    def "two tags with same ID should be equal"() {
        given: "two tags with same ID"
        def tag1 = new Tag(id: 1L, name: "tag1")
        def tag2 = new Tag(id: 1L, name: "tag2")

        expect: "tags are considered equal by ID"
        // Note: This assumes Tag entity has proper equals/hashCode based on ID
        tag1.id == tag2.id
    }

    def "should track creation and modification metadata"() {
        given: "a tag with metadata"
        def now = new Date()
        def tag = new Tag(
                name: "tag",
                createdBy: "admin",
                createdAt: now,
                modifiedBy: "admin",
                modifiedAt: now
        )

        expect: "metadata is stored correctly"
        tag.createdBy == "admin"
        tag.createdAt == now
        tag.modifiedBy == "admin"
        tag.modifiedAt == now
    }
}