package pl.databucket.server.service;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import pl.databucket.server.configuration.Constants;
import pl.databucket.server.dto.*;

import java.time.Duration;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Checks a dataset before it is imported: a file from a disk or a server folder may have been edited by hand or come
 * from another installation. The objects must be valid as when they are saved through the API (names, sizes, safe
 * icons) and every reference must point to an object of the same dataset, as the import maps them by these ids.
 */
public final class DatasetCheck {

    // the format this version reads and writes; a newer file can't be read reliably
    // 2: users and the history of the data
    public static final int FORMAT = 2;

    // the roles a user of a dataset may have (never SUPER)
    static final Set<String> USER_ROLES = Set.of(Constants.ROLE_ADMIN, Constants.ROLE_MEMBER, Constants.ROLE_ROBOT, Constants.ROLE_AGENT);

    private static final int MAX_PROBLEMS = 20;

    private final Validator validator;
    private final List<String> problems = new ArrayList<>();

    private DatasetCheck(Validator validator) {
        this.validator = validator;
    }

    /**
     * @return what is wrong with the dataset (at most 20 problems), empty when it can be imported
     */
    public static List<String> problems(DatasetDto dataset, Validator validator) {
        DatasetCheck check = new DatasetCheck(validator);
        check.check(dataset);
        return check.problems.size() > MAX_PROBLEMS ? check.problems.subList(0, MAX_PROBLEMS) : check.problems;
    }

    private void check(DatasetDto d) {
        if (d.getFormat() != null && d.getFormat() > FORMAT) {
            problems.add("The dataset is of a newer format (" + d.getFormat() + ") than this Databucket reads (" + FORMAT + ")");
            return;
        }
        if (d.getName() == null || d.getName().isBlank())
            problems.add("The dataset has no name");

        valid("Team", d.getTeams(), TeamDto::getName);
        valid("Enum", d.getEnums(), DataEnumDto::getName);
        valid("Class", d.getClasses(), DataClassDto::getName);
        valid("Group", d.getGroups(), GroupDto::getShortName);
        valid("Bucket", d.getBuckets(), BucketDto::getName);
        valid("Tag", d.getTags(), TagDto::getName);
        valid("Columns", d.getColumns(), DataColumnsDto::getName);
        valid("Filter", d.getFilters(), DataFilterDto::getName);
        valid("View", d.getViews(), ViewDto::getName);
        valid("Task", d.getTasks(), TaskDto::getName);
        valid("Icon", d.getIcons(), SvgDto::getName);

        Set<Short> teams = ids("teams", d.getTeams(), TeamDto::getId);
        Set<Integer> enums = ids("enums", d.getEnums(), DataEnumDto::getId);
        Set<Long> classes = ids("classes", d.getClasses(), DataClassDto::getId);
        Set<Long> groups = ids("groups", d.getGroups(), GroupDto::getId);
        Set<Long> buckets = ids("buckets", d.getBuckets(), BucketDto::getId);
        Set<Long> tags = ids("tags", d.getTags(), TagDto::getId);
        Set<Long> columns = ids("columns", d.getColumns(), DataColumnsDto::getId);
        Set<Long> filters = ids("filters", d.getFilters(), DataFilterDto::getId);

        Set<String> bucketNames = new HashSet<>();
        for (BucketDto bucket : d.getBuckets())
            if (bucket.getName() != null && !bucketNames.add(bucket.getName()))
                problems.add("Bucket " + bucket.getName() + ": the name is used twice");

        for (DataClassDto c : d.getClasses())
            if (c.getConfiguration() != null)
                c.getConfiguration().forEach(item -> ref("Class " + c.getName(), "enum", enums, item.getEnumId()));
        for (GroupDto g : d.getGroups())
            refs("Group " + g.getShortName(), "team", teams, g.getTeamsIds());
        for (BucketDto b : d.getBuckets()) {
            ref("Bucket " + b.getName(), "class", classes, b.getClassId());
            refs("Bucket " + b.getName(), "group", groups, b.getGroupsIds());
            refs("Bucket " + b.getName(), "team", teams, b.getTeamsIds());
        }
        for (TagDto t : d.getTags()) {
            refs("Tag " + t.getName(), "bucket", buckets, t.getBucketsIds());
            refs("Tag " + t.getName(), "class", classes, t.getClassesIds());
        }
        for (DataColumnsDto c : d.getColumns())
            ref("Columns " + c.getName(), "class", classes, c.getClassId());
        for (DataFilterDto f : d.getFilters())
            ref("Filter " + f.getName(), "class", classes, f.getClassId());
        for (ViewDto v : d.getViews()) {
            ref("View " + v.getName(), "columns", columns, v.getColumnsId());
            ref("View " + v.getName(), "filter", filters, v.getFilterId());
            refs("View " + v.getName(), "bucket", buckets, v.getBucketsIds());
            refs("View " + v.getName(), "class", classes, v.getClassesIds());
            refs("View " + v.getName(), "team", teams, v.getTeamsIds());
        }
        for (TaskDto t : d.getTasks()) {
            ref("Task " + t.getName(), "class", classes, t.getClassId());
            ref("Task " + t.getName(), "filter", filters, t.getFilterId());
            refs("Task " + t.getName(), "bucket", buckets, t.getBucketsIds());
            refs("Task " + t.getName(), "class", classes, t.getClassesIds());
            Map<String, Object> actions = t.getConfiguration() != null ? t.getConfiguration().getActions() : null;
            // 0 means no tag
            if (actions != null && actions.get("tagId") instanceof Number tagId && tagId.longValue() != 0)
                ref("Task " + t.getName(), "tag", tags, tagId.longValue());
        }

        for (Map.Entry<String, List<DataCreateDTO>> rows : d.getData().entrySet()) {
            if (!bucketNames.contains(rows.getKey())) {
                problems.add("Data of " + rows.getKey() + ": there is no such bucket in the dataset");
                continue;
            }
            Set<Long> missingTags = rows.getValue().stream().map(DataCreateDTO::getTagId)
                    .filter(tagId -> tagId != null && !tags.contains(tagId)).collect(Collectors.toCollection(TreeSet::new));
            if (!missingTags.isEmpty())
                problems.add("Data of " + rows.getKey() + ": refers to missing tags " + missingTags);
        }

        Set<String> users = checkUsers(d.getUsers());
        checkHistory(d, users, tags);
    }

    private Set<String> checkUsers(List<DatasetUserDto> list) {
        Set<String> users = new HashSet<>();
        for (DatasetUserDto user : list) {
            String name = user.getUsername();
            if (name == null || name.isBlank() || name.length() > Constants.USERNAME_MAX || !name.equals(name.trim())) {
                problems.add("User " + name + ": the name must have 1 to " + Constants.USERNAME_MAX + " characters, without spaces around");
                continue;
            }
            if (!users.add(name))
                problems.add("User " + name + ": the name is used twice");
            if (user.getPassword() == null || user.getPassword().isEmpty())
                problems.add("User " + name + ": has no password");
            if (!USER_ROLES.contains(user.getRole()))
                problems.add("User " + name + ": the role must be one of " + new TreeSet<>(USER_ROLES));
        }
        return users;
    }

    private void checkHistory(DatasetDto d, Set<String> users, Set<Long> tags) {
        DatasetHistoryDto history = d.getHistory();
        if (history == null)
            return;
        Duration since = duration(history.getSince());
        if (since == null) {
            problems.add("History: since must be an ISO-8601 duration, e.g. P30D");
            return;
        }
        int i = 0;
        for (DatasetHistoryDto.Change change : history.getChanges()) {
            String owner = "History change " + i++;
            List<DataCreateDTO> rows = change.getBucket() == null ? null : d.getData().get(change.getBucket());
            if (rows == null)
                problems.add(owner + ": there are no data of the bucket " + change.getBucket());
            else if (change.getRow() == null || change.getRow() < 0 || change.getRow() >= rows.size())
                problems.add(owner + ": there is no row " + change.getRow() + " in the data of " + change.getBucket());
            Duration ago = duration(change.getAgo());
            if (ago == null || ago.compareTo(since) > 0)
                problems.add(owner + ": ago must be an ISO-8601 duration, at most since (" + history.getSince() + ")");
            if (change.getUser() != null && !users.contains(change.getUser()))
                problems.add(owner + ": refers to a missing user " + change.getUser());
            if (change.getTagId() != null && change.getTagId() != 0 && !tags.contains(change.getTagId()))
                problems.add(owner + ": refers to a missing tag " + change.getTagId());
            if (change.getTagId() == null && change.getReserved() == null && change.getProperties() == null)
                problems.add(owner + ": changes nothing");
        }
    }

    // a non-negative ISO-8601 duration, or null
    private static Duration duration(String text) {
        try {
            Duration duration = text == null ? null : Duration.parse(text);
            return duration == null || duration.isNegative() ? null : duration;
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    // valid as when saved through the API (the constraints of the DTOs, e.g. sizes and safe icons)
    private <T> void valid(String kind, List<T> objects, Function<T, String> name) {
        for (T object : objects)
            for (ConstraintViolation<T> violation : validator.validate(object))
                problems.add(kind + " " + name.apply(object) + ": " + violation.getPropertyPath() + ": " + violation.getMessage());
    }

    private <T, K> Set<K> ids(String kind, List<T> objects, Function<T, K> id) {
        Set<K> ids = new HashSet<>();
        for (T object : objects) {
            K value = id.apply(object);
            if (value == null)
                problems.add("An object of " + kind + " has no id");
            else if (!ids.add(value))
                problems.add("The id " + value + " is used twice in " + kind);
        }
        return ids;
    }

    private <K> void ref(String owner, String kind, Set<K> ids, K id) {
        if (id != null && !ids.contains(id))
            problems.add(owner + ": refers to a missing " + kind + " " + id);
    }

    private <K> void refs(String owner, String kind, Set<K> ids, Collection<K> keys) {
        if (keys != null)
            keys.forEach(id -> ref(owner, kind, ids, id));
    }
}
