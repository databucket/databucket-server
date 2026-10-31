package pl.databucket.server.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.databucket.server.logging.AuditLog;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The content of a project: everything except the project itself and its users. Used by the super-admin area
 * (outside the project filter), so it works on the tables directly, for any project.
 */
@Service
public class ProjectContentService {

    // the project's own tables, in an order the foreign keys allow deleting them in
    // (views -> columns, filters; tasks -> filters; buckets, columns, filters -> classes)
    private static final List<String[]> CONTENT = List.of(
            new String[]{"views", "Views"},
            new String[]{"tasks", "Tasks"},
            new String[]{"buckets", "Buckets"},
            new String[]{"groups", "Groups"},
            new String[]{"tags", "Tags"},
            new String[]{"data_filters", "Filters"},
            new String[]{"data_columns", "Columns"},
            new String[]{"data_classes", "Classes"},
            new String[]{"data_enum", "Enums"},
            new String[]{"teams", "Teams"},
            new String[]{"svg", "Icons"}
    );

    private static final String BUCKETS = "SELECT bucket_id FROM buckets WHERE project_id = :p";
    private static final String GROUPS = "SELECT group_id FROM groups WHERE project_id = :p";
    private static final String TAGS = "SELECT tag_id FROM tags WHERE project_id = :p";
    private static final String TASKS = "SELECT task_id FROM tasks WHERE project_id = :p";
    private static final String VIEWS = "SELECT view_id FROM views WHERE project_id = :p";
    private static final String TEAMS = "SELECT team_id FROM teams WHERE project_id = :p";
    private static final String CLASSES = "SELECT class_id FROM data_classes WHERE project_id = :p";

    // the links between the project's objects, and between them and users
    private static final List<String> LINKS = List.of(
            "DELETE FROM buckets_teams WHERE bucket_id IN (" + BUCKETS + ") OR team_id IN (" + TEAMS + ")",
            "DELETE FROM buckets_users WHERE bucket_id IN (" + BUCKETS + ")",
            "DELETE FROM groups_buckets WHERE bucket_id IN (" + BUCKETS + ") OR group_id IN (" + GROUPS + ")",
            "DELETE FROM groups_teams WHERE group_id IN (" + GROUPS + ") OR team_id IN (" + TEAMS + ")",
            "DELETE FROM groups_users WHERE group_id IN (" + GROUPS + ")",
            "DELETE FROM tags_buckets WHERE bucket_id IN (" + BUCKETS + ") OR tag_id IN (" + TAGS + ")",
            "DELETE FROM tags_dataclasses WHERE tag_id IN (" + TAGS + ") OR class_id IN (" + CLASSES + ")",
            "DELETE FROM tasks_buckets WHERE bucket_id IN (" + BUCKETS + ") OR task_id IN (" + TASKS + ")",
            "DELETE FROM tasks_dataclasses WHERE task_id IN (" + TASKS + ") OR class_id IN (" + CLASSES + ")",
            "DELETE FROM views_buckets WHERE bucket_id IN (" + BUCKETS + ") OR view_id IN (" + VIEWS + ")",
            "DELETE FROM views_classes WHERE view_id IN (" + VIEWS + ") OR class_id IN (" + CLASSES + ")",
            "DELETE FROM views_teams WHERE view_id IN (" + VIEWS + ") OR team_id IN (" + TEAMS + ")",
            "DELETE FROM views_users WHERE view_id IN (" + VIEWS + ")",
            "DELETE FROM users_teams WHERE team_id IN (" + TEAMS + ")"
    );

    @Autowired
    private NamedParameterJdbcTemplate jdbcTemplate;

    /**
     * What the project holds, as shown before clearing it: the number of objects of each kind (deleted ones excluded)
     * and the number of data rows in its buckets.
     */
    @Transactional(readOnly = true)
    public Map<String, Long> getContent(int projectId) {
        MapSqlParameterSource params = new MapSqlParameterSource("p", projectId);
        Map<String, Long> content = new LinkedHashMap<>();
        for (String[] table : CONTENT) {
            String deleted = "svg".equals(table[0]) ? "" : " AND deleted = false";
            content.put(table[1], jdbcTemplate.queryForObject(
                    "SELECT count(*) FROM " + table[0] + " WHERE project_id = :p" + deleted, params, Long.class));
        }

        long rows = 0;
        for (Long bucketId : bucketIds(projectId, false)) {
            Long count = jdbcTemplate.queryForObject(
                    "SELECT CASE WHEN to_regclass(:t) IS NULL THEN 0 ELSE 1 END", Map.of("t", quoted(bucketId)), Long.class);
            if (count != null && count > 0)
                rows += jdbcTemplate.getJdbcTemplate().queryForObject("SELECT count(*) FROM " + quoted(bucketId), Long.class);
        }
        content.put("Data rows", rows);
        return content;
    }

    // nothing in the project (objects deleted before don't count, they are only marked as deleted)
    @Transactional(readOnly = true)
    public boolean isEmpty(int projectId) {
        MapSqlParameterSource params = new MapSqlParameterSource("p", projectId);
        for (String[] table : CONTENT) {
            String deleted = "svg".equals(table[0]) ? "" : " AND deleted = false";
            Boolean exists = jdbcTemplate.queryForObject(
                    "SELECT EXISTS (SELECT 1 FROM " + table[0] + " WHERE project_id = :p" + deleted + ")", params, Boolean.class);
            if (Boolean.TRUE.equals(exists))
                return false;
        }
        return true;
    }

    /**
     * Removes the content of the project for good: the bucket tables with their data and history, and all objects of
     * the project, also those deleted before (they are only marked as deleted). The project and its users stay.
     */
    @Transactional
    public void clearProject(int projectId) {
        MapSqlParameterSource params = new MapSqlParameterSource("p", projectId);

        List<Long> buckets = bucketIds(projectId, true);
        for (Long bucketId : buckets) {
            jdbcTemplate.getJdbcTemplate().execute("DROP TABLE IF EXISTS " + quotedHistory(bucketId));
            jdbcTemplate.getJdbcTemplate().execute("DROP TABLE IF EXISTS " + quoted(bucketId));
        }

        for (String sql : LINKS)
            jdbcTemplate.update(sql, params);
        for (String[] table : CONTENT)
            jdbcTemplate.update("DELETE FROM " + table[0] + " WHERE project_id = :p", params);

        AuditLog.log("project.content-removed", "id={} buckets={}", projectId, buckets.size());
    }

    private List<Long> bucketIds(int projectId, boolean withDeleted) {
        return jdbcTemplate.queryForList(BUCKETS + (withDeleted ? "" : " AND deleted = false"),
                new MapSqlParameterSource("p", projectId), Long.class);
    }

    // the table names are built from ids read from the database, never from the client
    private static String quoted(long bucketId) {
        return "\"x-bucket-" + bucketId + "\"";
    }

    private static String quotedHistory(long bucketId) {
        return "\"x-bucket-" + bucketId + "-h\"";
    }
}
