package pl.databucket.server.service

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.core.io.support.PathMatchingResourcePatternResolver
import pl.databucket.server.dto.DatasetDto
import spock.lang.Specification
import spock.lang.Unroll

/**
 * The dataset files shipped with Databucket (src/main/resources/datasets): each must be readable, and every
 * reference between its objects must point to an object of the same file, as the import maps them by these ids.
 */
class DatasetFilesSpec extends Specification {

    static List<String> datasetFiles() {
        new PathMatchingResourcePatternResolver().getResources("classpath:datasets/*.json")*.filename
    }

    static DatasetDto read(String file) {
        new ObjectMapper().readValue(DatasetFilesSpec.getResourceAsStream("/datasets/" + file), DatasetDto)
    }

    def "should ship the DEMO dataset"() {
        expect:
        datasetFiles().contains("demo.json")
        read("demo.json").name == "DEMO"
    }

    @Unroll
    def "should refer only to its own objects in #file"() {
        given:
        DatasetDto d = read(file)
        def teams = d.teams*.id as Set
        def enums = d.enums*.id as Set
        def classes = d.classes*.id as Set
        def groups = d.groups*.id as Set
        def buckets = d.buckets*.id as Set
        def tags = d.tags*.id as Set
        def columns = d.columns*.id as Set
        def filters = d.filters*.id as Set
        def known = { Set ids, id -> id == null || ids.contains(id) }
        def allKnown = { Set ids, Collection list -> list == null || ids.containsAll(list) }

        expect: "names and ids are unique"
        d.name
        d.buckets*.name.unique().size() == d.buckets.size()
        [d.teams, d.enums, d.classes, d.groups, d.buckets, d.tags, d.columns, d.filters, d.views, d.tasks]
                .every { list -> list*.id.unique().size() == list.size() }

        and: "the references"
        d.classes.every { c -> c.configuration.every { known(enums, it.enumId) } }
        d.groups.every { allKnown(teams, it.teamsIds) }
        d.buckets.every { known(classes, it.classId) && allKnown(groups, it.groupsIds) && allKnown(teams, it.teamsIds) }
        d.tags.every { allKnown(buckets, it.bucketsIds) && allKnown(classes, it.classesIds) }
        d.columns.every { known(classes, it.classId) }
        d.filters.every { known(classes, it.classId) }
        d.views.every { columns.contains(it.columnsId) && known(filters, it.filterId)
                && allKnown(buckets, it.bucketsIds) && allKnown(classes, it.classesIds) && allKnown(teams, it.teamsIds) }
        d.tasks.every { known(classes, it.classId) && known(filters, it.filterId)
                && allKnown(buckets, it.bucketsIds) && allKnown(classes, it.classesIds) }
        d.tasks.every { def tagId = it.configuration?.actions?.tagId; tagId == null || tagId == 0 || tags.contains(tagId as Long) }

        and: "the data belongs to buckets of the dataset and uses its tags"
        d.buckets*.name.containsAll(d.data.keySet())
        d.data.values().flatten().every { known(tags, it.tagId) }

        where:
        file << datasetFiles()
    }
}
