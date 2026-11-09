package pl.databucket.server.service.data;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import pl.databucket.server.dto.DataHistoryEntryDTO;
import pl.databucket.server.entity.Bucket;
import pl.databucket.server.entity.User;
import pl.databucket.server.exception.ConditionNotAllowedException;
import pl.databucket.server.exception.InvalidRuleException;
import pl.databucket.server.exception.UnknownColumnException;
import pl.databucket.server.service.data.v2.LogicFilter;
import pl.databucket.server.service.data.v2.LogicSql;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Search of the history of a bucket (API version 2): history entries of the data rows matching the data filter
 * (their current state), filtered by the history filter on the fields of the entries (id, dataId, tagId, reserved,
 * modifiedBy, modifiedAt). In a bucket with protected data, below the role ADMIN, only the history of the rows
 * reserved by the user (as for the history of one row).
 */
@Service
public class DataHistorySearchService {

    private static final Logger logger = LoggerFactory.getLogger(DataHistorySearchService.class);
    private static final ObjectMapper mapper = new ObjectMapper();
    private static final Pattern SORT = Pattern.compile("^\\s*(?:(asc|desc)\\(\\s*(\\w+)\\s*\\)|(\\w+))\\s*$");

    public static final int MAX_LIMIT = 1000;

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public DataHistorySearchService(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /** One page of history entries and the number of all matching ones. */
    public record Result(long total, List<DataHistoryEntryDTO> entries) {
    }

    /**
     * @param dataFilter    the data rows (by their current state); empty for all rows
     * @param historyLogic  JsonLogic on the fields of the history entries, or null
     * @param limit         0 only counts
     * @param withProperties also the properties of each version (when they changed)
     */
    public Result search(User user, Bucket bucket, LogicFilter dataFilter, Object historyLogic, int page, int limit,
                         String sort, boolean withProperties)
            throws InvalidRuleException, UnknownColumnException, ConditionNotAllowedException {

        Map<String, Object> paramMap = new HashMap<>();
        List<String> where = new ArrayList<>();

        String historySql = LogicSql.toSql(historyLogic, user.getUsername(), paramMap, "hl", LogicSql.Fields.HISTORY);
        if (historySql != null)
            where.add(historySql);

        boolean protectedRows = bucket.isProtectedData() && !user.isAdminUser();
        if (protectedRows)
            dataFilter.require(COL.RESERVED_BY, Operator.equal, user.getUsername());
        if (protectedRows || !dataFilter.isEmpty()) {
            Query rows = dataFilter.applyTo(new Query(bucket.getTableName()).select(COL.DATA_ID).from(), paramMap);
            where.add("(" + COL.DATA_ID + " IN (" + rows.toString(logger, paramMap) + "))");
        }

        String table = "\"" + bucket.getTableHistoryName() + "\"";
        String whereSql = where.isEmpty() ? "" : " WHERE " + String.join(" AND ", where);

        String countSql = "SELECT count(*) FROM " + table + whereSql;
        logger.debug(countSql);
        Long total = jdbcTemplate.queryForObject(countSql, paramMap, Long.class);
        if (limit == 0 || total == null || total == 0)
            return new Result(total == null ? 0 : total, List.of());

        String[] order = orderBy(sort);
        String sql = "SELECT " + COL.ID + ", " + COL.DATA_ID + ", " + COL.TAG_ID + ", " + COL.RESERVED + ", "
                + COL.PROPERTIES + " IS NOT NULL AS properties_changed, "
                + (withProperties ? COL.PROPERTIES + "::varchar AS properties_json, " : "")
                + COL.MODIFIED_AT + ", " + COL.MODIFIED_BY
                + " FROM " + table + whereSql
                + " ORDER BY " + order[0] + " " + order[1] + (COL.ID.equals(order[0]) ? "" : ", " + COL.ID + " " + order[1])
                + " LIMIT :hlLimit OFFSET :hlOffset";
        paramMap.put("hlLimit", limit);
        paramMap.put("hlOffset", (long) (page - 1) * limit);
        logger.debug(sql);

        List<DataHistoryEntryDTO> entries = jdbcTemplate.query(sql, paramMap, (rs, i) -> {
            DataHistoryEntryDTO entry = new DataHistoryEntryDTO();
            entry.setId(rs.getLong(COL.ID));
            entry.setDataId(rs.getLong(COL.DATA_ID));
            long tagId = rs.getLong(COL.TAG_ID);
            entry.setTagId(rs.wasNull() ? null : tagId);
            boolean reserved = rs.getBoolean(COL.RESERVED);
            entry.setReserved(rs.wasNull() ? null : reserved);
            entry.setPropertiesChanged(rs.getBoolean("properties_changed"));
            if (withProperties)
                entry.setProperties(properties(rs.getString("properties_json")));
            Timestamp modifiedAt = rs.getTimestamp(COL.MODIFIED_AT);
            entry.setModifiedAt(modifiedAt == null ? null : new Date(modifiedAt.getTime()));
            entry.setModifiedBy(rs.getString(COL.MODIFIED_BY));
            return entry;
        });
        return new Result(total, entries);
    }

    /** [column, ASC|DESC] from "id", "asc(modifiedAt)", "desc(dataId)" - the fields of the history entries only. */
    static String[] orderBy(String sort) throws InvalidRuleException {
        Matcher matcher = SORT.matcher(sort == null ? "id" : sort);
        if (!matcher.matches())
            throw new InvalidRuleException("Invalid sort '" + sort + "', e.g. desc(id) or asc(modifiedAt)");
        String name = matcher.group(3) != null ? matcher.group(3) : matcher.group(2);
        String column = LogicSql.Fields.HISTORY.fields().get(name);
        if (column == null)
            throw new InvalidRuleException("The history can't be sorted by '" + name + "' (allowed: "
                    + new java.util.TreeSet<>(LogicSql.Fields.HISTORY.names()) + ")");
        return new String[]{column, "desc".equals(matcher.group(1)) ? "DESC" : "ASC"};
    }

    private static Map<String, Object> properties(String json) {
        if (json == null)
            return null;
        try {
            return mapper.readValue(json, new TypeReference<>() {
            });
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Invalid properties in the history", e);
        }
    }
}
