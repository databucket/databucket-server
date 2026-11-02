package pl.databucket.server.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.databucket.server.dto.*;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Reads the content of the project of the current security context (the tenant) as a dataset, the reverse of
 * {@link DatasetImporter}: the objects with their ids (deleted ones left out) and the data rows of each bucket.
 * References to objects that are not in the dataset (deleted before) are removed, so that the dataset can be imported.
 */
@Service
public class DatasetExporter {

    private static final TypeReference<LinkedHashMap<String, Object>> PROPERTIES = new TypeReference<>() {
    };

    @Autowired private TeamService teamService;
    @Autowired private DataEnumService dataEnumService;
    @Autowired private DataClassService dataClassService;
    @Autowired private GroupService groupService;
    @Autowired private BucketService bucketService;
    @Autowired private TagService tagService;
    @Autowired private DataColumnsService dataColumnsService;
    @Autowired private DataFilterService dataFilterService;
    @Autowired private ViewService viewService;
    @Autowired private TaskService taskService;
    @Autowired private SvgService svgService;
    @Autowired private ModelMapper modelMapper;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private NamedParameterJdbcTemplate jdbcTemplate;

    @Transactional(readOnly = true)
    public DatasetDto exportDataset(String name, String description) {
        DatasetDto dataset = new DatasetDto();
        dataset.setFormat(DatasetCheck.FORMAT);
        dataset.setName(name);
        dataset.setDescription(description);

        dataset.setTeams(teamService.getTeams());
        dataset.setEnums(mapAll(dataEnumService.getDataEnums(), DataEnumDto.class));
        dataset.setClasses(mapAll(dataClassService.getDataClasses(), DataClassDto.class));
        dataset.setGroups(groupService.getGroups());
        dataset.setBuckets(bucketService.getBuckets());
        dataset.setTags(tagService.getTags());
        dataset.setColumns(mapAll(dataColumnsService.getColumns(), DataColumnsDto.class));
        dataset.setFilters(mapAll(dataFilterService.getFilters(), DataFilterDto.class));
        dataset.setViews(viewService.getViews());
        dataset.setTasks(taskService.getTasks());
        dataset.setIcons(mapAll(svgService.getSvgList(), SvgDto.class));

        // the bucket ids come from the project's buckets above; the table names are built from them
        for (BucketDto bucket : dataset.getBuckets()) {
            List<DataCreateDTO> rows = jdbcTemplate.getJdbcTemplate().query(
                    "SELECT tag_id, reserved, properties FROM \"x-bucket-" + bucket.getId() + "\" ORDER BY data_id",
                    (rs, i) -> {
                        DataCreateDTO row = new DataCreateDTO();
                        row.setTagId(rs.getObject("tag_id") != null ? rs.getLong("tag_id") : null);
                        row.setReserved(rs.getBoolean("reserved"));
                        row.setProperties(properties(rs.getString("properties")));
                        return row;
                    });
            if (!rows.isEmpty())
                dataset.getData().put(bucket.getName(), rows);
        }

        removeMissingReferences(dataset);
        return dataset;
    }

    private <T> List<T> mapAll(List<?> entities, Class<T> type) {
        return entities.stream().map(entity -> modelMapper.map(entity, type)).collect(Collectors.toList());
    }

    private Map<String, Object> properties(String json) {
        try {
            return objectMapper.readValue(json, PROPERTIES);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Objects deleted before stay linked to the others (only marked as deleted); a dataset refers only to its own
     * objects, as the import maps the references by these ids. Users are not part of a dataset at all.
     */
    static void removeMissingReferences(DatasetDto dataset) {
        Set<Short> teams = ids(dataset.getTeams(), TeamDto::getId);
        Set<Integer> enums = ids(dataset.getEnums(), DataEnumDto::getId);
        Set<Long> classes = ids(dataset.getClasses(), DataClassDto::getId);
        Set<Long> groups = ids(dataset.getGroups(), GroupDto::getId);
        Set<Long> buckets = ids(dataset.getBuckets(), BucketDto::getId);
        Set<Long> tags = ids(dataset.getTags(), TagDto::getId);
        Set<Long> columns = ids(dataset.getColumns(), DataColumnsDto::getId);
        Set<Long> filters = ids(dataset.getFilters(), DataFilterDto::getId);

        dataset.getTeams().forEach(team -> team.setUsersIds(null));

        dataset.getClasses().forEach(dataClass -> {
            if (dataClass.getConfiguration() != null)
                dataClass.getConfiguration().forEach(item -> item.setEnumId(known(enums, item.getEnumId())));
        });

        dataset.getGroups().forEach(group -> {
            group.setUsersIds(null);
            group.setBucketsIds(only(buckets, group.getBucketsIds()));
            group.setTeamsIds(only(teams, group.getTeamsIds()));
        });

        dataset.getBuckets().forEach(bucket -> {
            bucket.setUsersIds(null);
            bucket.setClassId(known(classes, bucket.getClassId()));
            bucket.setGroupsIds(only(groups, bucket.getGroupsIds()));
            bucket.setTeamsIds(only(teams, bucket.getTeamsIds()));
        });

        dataset.getTags().forEach(tag -> {
            tag.setBucketsIds(only(buckets, tag.getBucketsIds()));
            tag.setClassesIds(only(classes, tag.getClassesIds()));
        });

        dataset.getColumns().forEach(column -> {
            column.setClassId(known(classes, column.getClassId()));
            if (column.getConfiguration() != null)
                removeMissingEnums(enums, column.getConfiguration().getProperties());
        });

        dataset.getFilters().forEach(filter -> {
            filter.setClassId(known(classes, filter.getClassId()));
            if (filter.getConfiguration() != null)
                removeMissingEnums(enums, filter.getConfiguration().getProperties());
        });

        // a view can't be without its columns
        dataset.getViews().removeIf(view -> !columns.contains(view.getColumnsId()));
        dataset.getViews().forEach(view -> {
            view.setUsersIds(null);
            view.setFilterId(known(filters, view.getFilterId()));
            view.setBucketsIds(only(buckets, view.getBucketsIds()));
            view.setClassesIds(only(classes, view.getClassesIds()));
            view.setTeamsIds(only(teams, view.getTeamsIds()));
        });

        dataset.getTasks().forEach(task -> {
            task.setClassId(known(classes, task.getClassId()));
            task.setFilterId(known(filters, task.getFilterId()));
            task.setBucketsIds(only(buckets, task.getBucketsIds()));
            task.setClassesIds(only(classes, task.getClassesIds()));
            if (task.getConfiguration() != null) {
                removeMissingEnums(enums, task.getConfiguration().getProperties());
                Map<String, Object> actions = task.getConfiguration().getActions();
                // 0 means no tag
                if (actions != null && actions.get("tagId") instanceof Number tagId && !tags.contains(tagId.longValue()))
                    actions.put("tagId", 0);
            }
        });

        dataset.getData().values().forEach(rows -> rows.forEach(row -> row.setTagId(known(tags, row.getTagId()))));
    }

    private static <T, K> Set<K> ids(List<T> objects, Function<T, K> id) {
        return objects.stream().map(id).collect(Collectors.toSet());
    }

    private static <K> K known(Set<K> ids, K id) {
        return id != null && ids.contains(id) ? id : null;
    }

    // sorted, so that an exported file doesn't change from one export to the next
    private static <K extends Comparable<K>> Set<K> only(Set<K> ids, Collection<K> keys) {
        return keys == null ? null : keys.stream().filter(ids::contains).collect(Collectors.toCollection(TreeSet::new));
    }

    private static void removeMissingEnums(Set<Integer> enums, List<Map<String, Object>> properties) {
        if (properties != null)
            properties.forEach(property -> {
                if (property.get("enumId") instanceof Number enumId && !enums.contains(enumId.intValue()))
                    property.put("enumId", null);
            });
    }
}
