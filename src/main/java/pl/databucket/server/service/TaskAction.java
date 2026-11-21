package pl.databucket.server.service;

import pl.databucket.server.dto.DataClassItemDto;
import pl.databucket.server.dto.DataModifyDTO;
import pl.databucket.server.dto.TaskConfigDto;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The action of a saved task as the server runs it. Translated the same way as the "Run task" dialog of the web
 * application (TaskExecutionDialog.jsx) does: modify sets the tag, the reservation and the properties of the task
 * (properties by uuid, resolved to paths through the properties of the bucket's class, the task and its filter),
 * remove and clear history take only the rules, reserve takes the number, random and the target owner.
 *
 * @param changes  modify: what is set or removed
 * @param number   reserve: how many rows
 * @param random   reserve: random rows instead of the first ones by id
 * @param username reserve: the owner the rows are reserved for (only an admin may reserve for someone else)
 */
public record TaskAction(Type type, DataModifyDTO changes, int number, boolean random, String username) {

    public static final int MAX_RESERVATION = 1000;

    public enum Type {
        MODIFY("modify"), REMOVE("remove"), CLEAR_HISTORY("clear history"), RESERVE("reserve");

        private final String value;

        Type(String value) {
            this.value = value;
        }

        /** As stored in the task (configuration.actions.type). */
        public String value() {
            return value;
        }

        static Type of(Object value) {
            for (Type type : values())
                if (type.value.equals(value))
                    return type;
            return null;
        }
    }

    /** The task can't be run as it is (no action, nothing to change...); the message says what to fix. */
    public static class InvalidTaskException extends Exception {
        public InvalidTaskException(String message) {
            super(message);
        }
    }

    /**
     * @param classProperties  the properties of the bucket's class (may be null)
     * @param filterProperties the properties of the task's filter (may be null)
     */
    public static TaskAction of(TaskConfigDto configuration, List<DataClassItemDto> classProperties,
                                List<Map<String, Object>> filterProperties) throws InvalidTaskException {
        Map<String, Object> actions = configuration != null ? configuration.getActions() : null;
        Type type = actions != null ? Type.of(actions.get("type")) : null;
        if (type == null)
            throw new InvalidTaskException("The task has no action; choose modify, remove, clear history or reserve "
                    + "in the task's settings");

        return switch (type) {
            case MODIFY -> new TaskAction(type, changes(actions, paths(classProperties,
                    configuration.getProperties(), filterProperties)), 0, false, null);
            case RESERVE -> {
                int number = actions.get("number") instanceof Number n ? n.intValue() : 1;
                if (number < 1 || number > MAX_RESERVATION)
                    throw new InvalidTaskException("The task reserves " + number + " rows; it may reserve 1.."
                            + MAX_RESERVATION);
                yield new TaskAction(type, null, number, Boolean.TRUE.equals(actions.get("random")),
                        actions.get("username") instanceof String s && !s.isBlank() ? s : null);
            }
            default -> new TaskAction(type, null, 0, false, null);
        };
    }

    // the "modify" changes, as getModifyChanges() of the dialog
    @SuppressWarnings("unchecked")
    private static DataModifyDTO changes(Map<String, Object> actions, Map<String, String> paths) throws InvalidTaskException {
        DataModifyDTO changes = new DataModifyDTO();
        boolean any = false;
        if (Boolean.TRUE.equals(actions.get("setTag"))) {
            if (!(actions.get("tagId") instanceof Number tagId))
                throw new InvalidTaskException("The task sets the tag, but no tag is chosen");
            changes.setTagId(tagId.longValue());
            any = true;
        }
        if (Boolean.TRUE.equals(actions.get("setReserved"))) {
            changes.setReserved(Boolean.TRUE.equals(actions.get("reserved")));
            any = true;
        }
        Map<String, Object> toSet = new LinkedHashMap<>();
        List<String> toRemove = new ArrayList<>();
        if (actions.get("properties") instanceof List<?> properties)
            for (Object item : properties) {
                if (!(item instanceof Map<?, ?> property))
                    continue;
                String path = paths.get(String.valueOf(property.get("uuid")));
                if (path == null)
                    continue; // a property no longer defined anywhere, skipped as in the dialog
                Object action = property.get("action");
                if ("setValue".equals(action))
                    toSet.put(path, property.get("value"));
                else if ("setNull".equals(action))
                    toSet.put(path, null);
                else
                    toRemove.add(path);
            }
        if (!toSet.isEmpty()) {
            changes.setPropertiesToSet(toSet);
            any = true;
        }
        if (!toRemove.isEmpty()) {
            changes.setPropertiesToRemove(toRemove);
            any = true;
        }
        if (!any)
            throw new InvalidTaskException("The task changes nothing; set a tag, the reservation or properties "
                    + "in the task's settings");
        return changes;
    }

    // uuid -> path of the properties of the class, the task and the filter, merged as in the dialog: by path, the
    // first definition of a path wins
    private static Map<String, String> paths(List<DataClassItemDto> classProperties,
                                             List<Map<String, Object>> taskProperties,
                                             List<Map<String, Object>> filterProperties) {
        Map<String, String> paths = new HashMap<>();
        Set<String> seen = new HashSet<>();
        if (classProperties != null)
            for (DataClassItemDto property : classProperties)
                add(paths, seen, property.getUuid(), property.getPath());
        for (List<Map<String, Object>> list : java.util.Arrays.asList(taskProperties, filterProperties))
            if (list != null)
                for (Map<String, Object> property : list)
                    add(paths, seen, property.get("uuid"), property.get("path"));
        return paths;
    }

    private static void add(Map<String, String> paths, Set<String> seen, Object uuid, Object path) {
        if (uuid == null || !(path instanceof String p) || p.isBlank() || !seen.add(p))
            return;
        paths.putIfAbsent(String.valueOf(uuid), p);
    }
}
