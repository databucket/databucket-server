package pl.databucket.server.service

import pl.databucket.server.dto.*
import spock.lang.Specification

/**
 * An exported dataset refers only to its own objects (deleted ones are not exported), as the import maps the
 * references by these ids.
 */
class DatasetExporterSpec extends Specification {

    def "should remove references to objects that are not in the dataset"() {
        given:
        DatasetDto dataset = new DatasetDto()
        dataset.teams = [new TeamDto(id: 1 as Short, usersIds: [7L] as Set)]
        dataset.enums = [new DataEnumDto(id: 1)]
        dataset.classes = [new DataClassDto(id: 1L, configuration: [new DataClassItemDto(enumId: 1), new DataClassItemDto(enumId: 9)])]
        dataset.groups = [new GroupDto(id: 1L, bucketsIds: [1L, 9L] as Set, teamsIds: [1 as Short, 9 as Short] as Set, usersIds: [7L] as Set)]
        dataset.buckets = [new BucketDto(id: 1L, classId: 9L, groupsIds: [1L, 9L] as Set, teamsIds: [9 as Short] as Set, usersIds: [7L] as Set)]
        dataset.tags = [new TagDto(id: 1L, bucketsIds: [9L] as Set, classesIds: [1L] as Set)]
        dataset.columns = [new DataColumnsDto(id: 1L, classId: 1L)]
        dataset.filters = [new DataFilterDto(id: 1L, classId: 9L)]
        dataset.views = [
                new ViewDto(id: 1L, columnsId: 1L, filterId: 9L, bucketsIds: [1L, 9L] as Set, usersIds: [7L] as Set),
                new ViewDto(id: 2L, columnsId: 9L)
        ]
        TaskConfigDto taskConfig = new TaskConfigDto(actions: [tagId: 9, setTag: true], properties: [[enumId: 9], [enumId: 1]])
        dataset.tasks = [new TaskDto(id: 1L, filterId: 1L, classesIds: [9L] as Set, configuration: taskConfig)]
        dataset.data = [b: [new DataCreateDTO(tagId: 1L), new DataCreateDTO(tagId: 9L), new DataCreateDTO(tagId: null)]]

        when:
        DatasetExporter.removeMissingReferences(dataset)

        then:
        dataset.teams[0].usersIds == null
        dataset.classes[0].configuration*.enumId == [1, null]
        dataset.groups[0].bucketsIds == [1L] as Set
        dataset.groups[0].teamsIds == [1 as Short] as Set
        dataset.groups[0].usersIds == null
        dataset.buckets[0].classId == null
        dataset.buckets[0].groupsIds == [1L] as Set
        dataset.buckets[0].teamsIds.isEmpty()
        dataset.buckets[0].usersIds == null
        dataset.tags[0].bucketsIds.isEmpty()
        dataset.tags[0].classesIds == [1L] as Set
        dataset.columns[0].classId == 1L
        dataset.filters[0].classId == null

        and: "a view without its columns is left out"
        dataset.views*.id == [1L]
        dataset.views[0].filterId == null
        dataset.views[0].bucketsIds == [1L] as Set
        dataset.views[0].usersIds == null

        and:
        dataset.tasks[0].filterId == 1L
        dataset.tasks[0].classesIds.isEmpty()
        dataset.tasks[0].configuration.actions.tagId == 0
        dataset.tasks[0].configuration.properties*.enumId == [null, 1]
        dataset.data.b*.tagId == [1L, null, null]
    }
}
