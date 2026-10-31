package pl.databucket.server.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.databucket.server.dto.*;
import pl.databucket.server.entity.Bucket;
import pl.databucket.server.entity.User;
import pl.databucket.server.service.data.DataService;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Creates the content of a dataset in the project of the current security context (the tenant), in one transaction:
 * a failure leaves the project as it was, bucket tables included (DDL is transactional in PostgreSQL).
 * The objects are created through their services, in the order of their references, and the ids of the dataset
 * are mapped to the new ones, also inside the configurations (enums of class properties, tags in filters and tasks).
 */
@Service
public class DatasetImporter {

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
    @Autowired private DataService dataService;

    // the new ids of the dataset's objects, by the ids in the dataset
    private static class Ids {
        final Map<Short, Short> teams = new HashMap<>();
        final Map<Integer, Integer> enums = new HashMap<>();
        final Map<Long, Long> classes = new HashMap<>();
        final Map<Long, Long> groups = new HashMap<>();
        final Map<Long, Long> buckets = new HashMap<>();
        final Map<Long, Long> tags = new HashMap<>();
        final Map<Long, Long> columns = new HashMap<>();
        final Map<Long, Long> filters = new HashMap<>();
    }

    @Transactional
    public int importDataset(DatasetDto dataset, User user) throws Exception {
        Ids ids = new Ids();

        // the importing user joins the teams, so that what the teams give access to is visible to them
        for (TeamDto team : dataset.getTeams()) {
            Short id = team.getId();
            team.setId(null);
            team.setUsersIds(Set.of(user.getId()));
            ids.teams.put(id, teamService.createTeam(team).getId());
        }

        for (DataEnumDto dataEnum : dataset.getEnums()) {
            Integer id = dataEnum.getId();
            dataEnum.setId(null);
            ids.enums.put(id, dataEnumService.createDataEnum(dataEnum).getId());
        }

        for (DataClassDto dataClass : dataset.getClasses()) {
            Long id = dataClass.getId();
            dataClass.setId(null);
            if (dataClass.getConfiguration() != null)
                dataClass.getConfiguration().forEach(item -> item.setEnumId(map(ids.enums, item.getEnumId())));
            ids.classes.put(id, dataClassService.createDataClass(dataClass).getId());
        }

        // the buckets join their groups when they are created
        for (GroupDto group : dataset.getGroups()) {
            Long id = group.getId();
            group.setId(null);
            group.setBucketsIds(new HashSet<>());   // empty, not null: the buckets add themselves to it
            group.setUsersIds(null);
            group.setTeamsIds(mapAll(ids.teams, group.getTeamsIds()));
            ids.groups.put(id, groupService.createGroup(group).getId());
        }

        Map<String, Long> bucketIdsByName = new HashMap<>();
        for (BucketDto bucket : dataset.getBuckets()) {
            Long id = bucket.getId();
            bucket.setId(null);
            bucket.setUsersIds(null);
            bucket.setClassId(map(ids.classes, bucket.getClassId()));
            bucket.setGroupsIds(mapAll(ids.groups, bucket.getGroupsIds()));
            bucket.setTeamsIds(mapAll(ids.teams, bucket.getTeamsIds()));
            Long newId = bucketService.createBucket(bucket).getId();
            ids.buckets.put(id, newId);
            bucketIdsByName.put(bucket.getName(), newId);
        }

        for (TagDto tag : dataset.getTags()) {
            Long id = tag.getId();
            tag.setId(null);
            tag.setBucketsIds(mapAll(ids.buckets, tag.getBucketsIds()));
            tag.setClassesIds(mapAll(ids.classes, tag.getClassesIds()));
            ids.tags.put(id, tagService.createTag(tag).getId());
        }

        for (DataColumnsDto columns : dataset.getColumns()) {
            Long id = columns.getId();
            columns.setId(null);
            columns.setClassId(map(ids.classes, columns.getClassId()));
            if (columns.getConfiguration() != null)
                mapEnums(ids, columns.getConfiguration().getProperties());
            ids.columns.put(id, dataColumnsService.createColumns(columns).getId());
        }

        for (DataFilterDto filter : dataset.getFilters()) {
            Long id = filter.getId();
            filter.setId(null);
            filter.setClassId(map(ids.classes, filter.getClassId()));
            if (filter.getConfiguration() != null) {
                mapEnums(ids, filter.getConfiguration().getProperties());
                mapTagIds(ids, filter.getConfiguration().getLogic());
                mapTagIds(ids, filter.getConfiguration().getTree());
            }
            ids.filters.put(id, dataFilterService.createFilter(filter).getId());
        }

        for (ViewDto view : dataset.getViews()) {
            view.setId(null);
            view.setUsersIds(null);
            view.setColumnsId(map(ids.columns, view.getColumnsId()));
            view.setFilterId(map(ids.filters, view.getFilterId()));
            view.setBucketsIds(mapAll(ids.buckets, view.getBucketsIds()));
            view.setClassesIds(mapAll(ids.classes, view.getClassesIds()));
            view.setTeamsIds(mapAll(ids.teams, view.getTeamsIds()));
            viewService.createView(view);
        }

        for (TaskDto task : dataset.getTasks()) {
            task.setId(null);
            task.setClassId(map(ids.classes, task.getClassId()));
            task.setFilterId(map(ids.filters, task.getFilterId()));
            task.setBucketsIds(mapAll(ids.buckets, task.getBucketsIds()));
            task.setClassesIds(mapAll(ids.classes, task.getClassesIds()));
            if (task.getConfiguration() != null) {
                mapEnums(ids, task.getConfiguration().getProperties());
                Map<String, Object> actions = task.getConfiguration().getActions();
                // 0 means no tag (an action that doesn't set one)
                if (actions != null && actions.get("tagId") instanceof Number tagId && tagId.longValue() > 0)
                    actions.put("tagId", map(ids.tags, tagId.longValue()));
            }
            taskService.createTask(task);
        }

        int rows = 0;
        for (Map.Entry<String, List<DataCreateDTO>> entry : dataset.getData().entrySet()) {
            if (entry.getValue().isEmpty())
                continue;
            if (!bucketIdsByName.containsKey(entry.getKey()))
                throw new IllegalArgumentException("The dataset has data of an unknown bucket: " + entry.getKey());
            entry.getValue().forEach(row -> row.setTagId(map(ids.tags, row.getTagId())));
            Bucket bucket = bucketService.getBucket(entry.getKey());
            dataService.createData(user, bucket, entry.getValue());
            rows += entry.getValue().size();
        }
        return rows;
    }

    // a reference to an object of the dataset; null stays null, an unknown id is an error in the dataset
    private static <K, V> V map(Map<K, V> ids, K id) {
        if (id == null)
            return null;
        V newId = ids.get(id);
        if (newId == null)
            throw new IllegalArgumentException("The dataset refers to a missing object: " + id);
        return newId;
    }

    private static <K, V> Set<V> mapAll(Map<K, V> ids, Collection<K> keys) {
        return keys == null ? null : keys.stream().map(id -> map(ids, id)).collect(Collectors.toSet());
    }

    // class properties copied into columns, filters and tasks carry the enum of the property
    private static void mapEnums(Ids ids, List<Map<String, Object>> properties) {
        if (properties == null)
            return;
        for (Map<String, Object> property : properties)
            if (property.get("enumId") instanceof Number enumId)
                property.put("enumId", map(ids.enums, enumId.intValue()));
    }

    // Tags are compared by id in filters: in the logic {"==": [{"var": "tagId"}, 1]} or {"in": [{"var": "tagId"}, [1, 4]]},
    // in the query builder tree as the value of a rule with "field": "tagId"
    @SuppressWarnings("unchecked")
    private static void mapTagIds(Ids ids, Object node) {
        if (node instanceof Map<?, ?> map) {
            Map<String, Object> object = (Map<String, Object>) map;
            if (object.get("properties") instanceof Map<?, ?> rule && "tagId".equals(rule.get("field"))) {
                Map<String, Object> properties = (Map<String, Object>) rule;
                properties.put("value", mapTagValues(ids, properties.get("value")));
            }
            object.values().forEach(value -> mapTagIds(ids, value));
        } else if (node instanceof List<?> list) {
            boolean tagOperation = list.stream().anyMatch(item -> item instanceof Map<?, ?> m && "tagId".equals(m.get("var")));
            if (tagOperation) {
                List<Object> operands = (List<Object>) list;
                for (int i = 0; i < operands.size(); i++)
                    if (!(operands.get(i) instanceof Map))
                        operands.set(i, mapTagValues(ids, operands.get(i)));
            } else
                list.forEach(item -> mapTagIds(ids, item));
        }
    }

    private static Object mapTagValues(Ids ids, Object value) {
        if (value instanceof Number tagId)
            return ids.tags.getOrDefault(tagId.longValue(), tagId.longValue());
        if (value instanceof List<?> list)
            return list.stream().map(item -> mapTagValues(ids, item)).collect(Collectors.toList());
        return value;
    }
}
