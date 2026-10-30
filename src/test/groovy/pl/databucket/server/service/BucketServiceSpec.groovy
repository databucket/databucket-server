package pl.databucket.server.service

import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import pl.databucket.server.configuration.ApplicationConfiguration
import pl.databucket.server.dto.BucketDto
import pl.databucket.server.dto.IconDto
import pl.databucket.server.entity.Bucket
import pl.databucket.server.entity.Group
import pl.databucket.server.repository.BucketRepository
import pl.databucket.server.repository.GroupRepository
import spock.lang.Specification
import spock.lang.Subject

import java.lang.reflect.Field

class BucketServiceSpec extends Specification {

    BucketRepository bucketRepository = Mock()
    GroupRepository groupRepository = Mock()
    NamedParameterJdbcTemplate namedJdbcTemplate = Mock()
    JdbcTemplate jdbcTemplate = Mock()

    @Subject
    BucketService bucketService

    def setup() {
        bucketService = new BucketService()
        setField(bucketService, "bucketRepository", bucketRepository)
        setField(bucketService, "groupRepository", groupRepository)
        setField(bucketService, "jdbcTemplate", namedJdbcTemplate)
        setField(bucketService, "modelMapper", new ApplicationConfiguration().modelMapper())
        namedJdbcTemplate.getJdbcTemplate() >> jdbcTemplate
    }

    // ==================== createBucket Tests ====================

    def "should create bucket with default icon when icon is not given"() {
        given: "a bucket without icon"
        def bucketDto = new BucketDto(name: "orders")
        bucketRepository.existsByNameAndDeleted("orders", false) >> false
        bucketRepository.saveAndFlush(_ as Bucket) >> { Bucket b -> b.id = 1L; b }
        bucketRepository.save(_ as Bucket) >> { Bucket b -> b }

        when: "creating the bucket"
        def result = bucketService.createBucket(bucketDto)

        then: "the default icon is set"
        result.icon.name == "trip_origin"
        result.icon.color == null
        result.icon.svg == null
    }

    def "should create bucket with given icon"() {
        given: "a bucket with icon"
        def bucketDto = new BucketDto(name: "orders", icon: new IconDto(name: "star", color: "#ff0000"))
        bucketRepository.existsByNameAndDeleted("orders", false) >> false
        bucketRepository.saveAndFlush(_ as Bucket) >> { Bucket b -> b.id = 1L; b }
        bucketRepository.save(_ as Bucket) >> { Bucket b -> b }

        when: "creating the bucket"
        def result = bucketService.createBucket(bucketDto)

        then: "the given icon is kept"
        result.icon.name == "star"
        result.icon.color == "#ff0000"
    }

    // ==================== modifyBucket Tests ====================

    def "should keep icon and remove groups when icon and groups are not given"() {
        given: "an existing bucket with icon and one group"
        def group = new Group(id: 5L, buckets: [] as Set)
        def bucket = new Bucket(id: 1L, name: "orders", iconName: "star", iconColor: "#ff0000", groups: [group] as Set)
        group.buckets.add(bucket)
        bucketRepository.findByIdAndDeleted(1L, false) >> bucket
        bucketRepository.save(_ as Bucket) >> { Bucket b -> b }

        and: "a request without icon, groups, users and teams"
        def bucketDto = new BucketDto(id: 1L, name: "orders")

        when: "modifying the bucket"
        def result = bucketService.modifyBucket(bucketDto)

        then: "the bucket is removed from the group"
        1 * groupRepository.findAllByDeletedAndIdIn(false, [5L] as Set) >> [group]
        1 * groupRepository.findAllByDeletedAndIdIn(false, [] as Set) >> []
        !group.buckets.contains(bucket)

        and: "the icon is unchanged"
        result.icon.name == "star"
        result.icon.color == "#ff0000"
        result.groupsIds == null
    }

    private void setField(Object target, String fieldName, Object value) {
        Field field = target.class.getDeclaredField(fieldName)
        field.setAccessible(true)
        field.set(target, value)
    }
}
