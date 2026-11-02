package pl.databucket.server.service

import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.validation.Validation
import jakarta.validation.Validator
import pl.databucket.server.dto.*
import spock.lang.Shared
import spock.lang.Specification

/**
 * A dataset from a file is checked before the import: valid objects (as through the API) and references only to
 * objects of the same dataset.
 */
class DatasetCheckSpec extends Specification {

    @Shared
    Validator validator = Validation.buildDefaultValidatorFactory().validator

    DatasetDto demo() {
        new ObjectMapper().readValue(DatasetCheckSpec.getResourceAsStream("/datasets/demo.json"), DatasetDto)
    }

    def "should accept the DEMO dataset"() {
        expect:
        DatasetCheck.problems(demo(), validator).isEmpty()
    }

    def "should refuse a dataset of a newer format"() {
        given:
        DatasetDto dataset = demo()
        dataset.format = DatasetCheck.FORMAT + 1

        expect:
        DatasetCheck.problems(dataset, validator) == ["The dataset is of a newer format (2) than this Databucket reads (1)"]
    }

    def "should list missing references, unsafe icons and invalid objects"() {
        given:
        DatasetDto dataset = new DatasetDto(name: "X")
        dataset.teams = [new TeamDto(id: 1 as Short, name: "team")]
        dataset.classes = [new DataClassDto(id: 1L, name: "class", configuration: [new DataClassItemDto(enumId: 5)])]
        dataset.buckets = [
                new BucketDto(id: 1L, name: "b", classId: 9L, teamsIds: [1 as Short, 2 as Short] as Set,
                        icon: new IconDto("x", null, '<svg><style>*{}</style></svg>')),
                new BucketDto(id: 1L, name: "b")
        ]
        dataset.columns = [new DataColumnsDto(id: 1L, name: "columns")]
        dataset.views = [new ViewDto(name: "view", columnsId: 3L, filterId: 4L)]
        dataset.tasks = [new TaskDto(name: "task", configuration: new TaskConfigDto(actions: [tagId: 7]))]
        dataset.icons = [new SvgDto(name: "evil", structure: '<script>alert(1)</script>')]
        dataset.data = [b: [new DataCreateDTO(tagId: 3L)], other: [new DataCreateDTO()]]

        when:
        List<String> problems = DatasetCheck.problems(dataset, validator)

        then:
        problems.containsAll([
                "Bucket b: icon.svg: The icon contains a forbidden element: style",
                "Icon evil: structure: The icon contains a forbidden element: script",
                "The id 1 is used twice in buckets",
                "Bucket b: the name is used twice",
                "Class class: refers to a missing enum 5",
                "Bucket b: refers to a missing class 9",
                "Bucket b: refers to a missing team 2",
                "View view: refers to a missing columns 3",
                "View view: refers to a missing filter 4",
                "Task task: refers to a missing tag 7",
                "Data of b: refers to missing tags [3]",
                "Data of other: there is no such bucket in the dataset"
        ])
    }

    def "should require a name and list at most 20 problems"() {
        given:
        DatasetDto dataset = new DatasetDto()
        dataset.views = (1..30).collect { new ViewDto(name: "view $it", columnsId: 99L) }

        when:
        List<String> problems = DatasetCheck.problems(dataset, validator)

        then:
        problems.size() == 20
        problems[0] == "The dataset has no name"
    }
}
