package pl.databucket.server.mcp;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springaicommunity.mcp.annotation.McpTool;
import org.springaicommunity.mcp.annotation.McpToolParam;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;
import pl.databucket.server.configuration.Constants;
import pl.databucket.server.dto.DataCreateDTO;
import pl.databucket.server.dto.DataDTO;
import pl.databucket.server.dto.DataGetDTO;
import pl.databucket.server.dto.DataHistoryEntryDTO;
import pl.databucket.server.dto.DataModifyDTO;
import pl.databucket.server.entity.Bucket;
import pl.databucket.server.entity.User;
import pl.databucket.server.service.BucketService;
import pl.databucket.server.service.UserService;
import pl.databucket.server.service.data.DataHistorySearchService;
import pl.databucket.server.service.data.DataService;
import pl.databucket.server.service.data.ResultField;
import pl.databucket.server.service.data.SearchRules;
import pl.databucket.server.service.data.v2.LogicFilter;
import pl.databucket.server.service.data.v2.LogicSql;
import pl.databucket.server.service.data.v2.RulesToLogic;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * The tools of the MCP server (/mcp) for AI agents. They do what the REST API (version 2 of the rules) does, as the
 * user of the request's token: in the user's project, in the buckets the user has access to. The server is stateless
 * and runs each call on the request thread, so the security context and the project (tenant) of the token apply.
 * Errors thrown here reach the agent as the tool's error message, so they say what to do instead.
 */
@Component
public class McpTools {

    static final int MAX_ROWS = 100;
    private static final String RULES = "Rules selecting the data rows, combined with 'and'; each rule is "
            + "[left, operator, right], e.g. [\"$.city\", \"==\", \"Paris\"], or {\"or\": [rule, ...]}, {\"and\": [...]}, "
            + "{\"!\": rule}. Left: a property path ($.name, $.address.city) or a field (id, tagId, owner, reserved, "
            + "createdBy, createdAt, modifiedBy, modifiedAt). Operators: ==, !=, >, >=, <, <=, in, !in (right: a list), "
            + "is, is not (right: null), like, !like (% and _), similar, !similar, ~, !~, ~*, !~* (regular expressions). "
            + "Empty or missing: all rows.";
    private static final String LOGIC = "Instead of or next to 'rules': a JsonLogic expression, e.g. "
            + "{\"and\": [{\"==\": [{\"var\": \"$.city\"}, \"Paris\"]}, {\">\": [{\"var\": \"$.age\"}, 30]}]}.";
    private static final String SORT = "Sort order: a field or property, asc/desc, e.g. id, desc(id), asc($.name); "
            + "or random. Default id.";

    private final ObjectMapper objectMapper;
    private final DataService dataService;
    private final DataHistorySearchService historySearch;
    private final BucketService bucketService;
    private final UserService userService;
    private final McpBucketInfoService bucketInfo;
    private final TransactionTemplate transactionTemplate;

    public McpTools(ObjectMapper objectMapper, DataService dataService, DataHistorySearchService historySearch,
                    BucketService bucketService, UserService userService, McpBucketInfoService bucketInfo,
                    TransactionTemplate transactionTemplate) {
        this.objectMapper = objectMapper;
        this.dataService = dataService;
        this.historySearch = historySearch;
        this.bucketService = bucketService;
        this.userService = userService;
        this.bucketInfo = bucketInfo;
        this.transactionTemplate = transactionTemplate;
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record DataPage(long total, Integer page, int limit, Integer totalPages, List<DataDTO> data) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record Reservation(int reserved, long available, List<DataDTO> data, String message) {
    }

    public record Count(int count, String message) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record HistoryPage(long total, Integer page, int limit, Integer totalPages, List<DataHistoryEntryDTO> history) {
    }

    // ---------------------------------------------------------------- buckets

    @McpTool(name = "list_buckets", description = "Lists the buckets (tables of test data) the user has access to in "
            + "the project of the token: name, description, data class, whether the history of changes is kept and "
            + "whether the data is protected (then the user sees only the rows they reserved). Start here.",
            annotations = @McpTool.McpAnnotations(title = "List buckets", readOnlyHint = true, destructiveHint = false,
                    idempotentHint = true, openWorldHint = false))
    public String listBuckets() throws JsonProcessingException {
        return json(bucketInfo.listBuckets());
    }

    @McpTool(name = "describe_bucket", description = "Describes a bucket: the fields of its data class (title, "
            + "property path such as $.name, type, allowed values of an enum) and the tags its rows can have (id and "
            + "name). Call it before searching or writing data, to use the right property paths and tag ids.",
            annotations = @McpTool.McpAnnotations(title = "Describe a bucket", readOnlyHint = true, destructiveHint = false,
                    idempotentHint = true, openWorldHint = false))
    public String describeBucket(
            @McpToolParam(description = "Bucket name, as returned by list_buckets") String bucket) throws JsonProcessingException {
        return json(bucketInfo.describeBucket(bucket));
    }

    // ---------------------------------------------------------------- reading

    @McpTool(name = "find_data", description = "Finds the data rows of a bucket matching the rules, one page at a "
            + "time. Each row: id, tagId, reserved, owner (who reserved it), properties (the data, JSON), created/modified "
            + "by/at. Returns total (all matching rows), page, totalPages and data. limit 0 only counts.",
            annotations = @McpTool.McpAnnotations(title = "Find data", readOnlyHint = true, destructiveHint = false,
                    idempotentHint = true, openWorldHint = false))
    public String findData(
            @McpToolParam(description = "Bucket name") String bucket,
            @McpToolParam(description = RULES, required = false) List<Object> rules,
            @McpToolParam(description = LOGIC, required = false) Map<String, Object> logic,
            @McpToolParam(description = "Page number, from 1. Default 1.", required = false) Integer page,
            @McpToolParam(description = "Rows per page, 0.." + MAX_ROWS + ". Default 20.", required = false) Integer limit,
            @McpToolParam(description = SORT, required = false) String sort) throws Exception {
        int pageNo = page != null ? page : 1;
        int rows = limit != null ? limit : 20;
        if (pageNo < 1 || rows < 0 || rows > MAX_ROWS)
            throw new IllegalArgumentException("The page starts from 1 and the limit is 0.." + MAX_ROWS);
        Access access = access(bucket);
        String order = sort != null && !sort.isBlank() ? sort : "id";

        Map<ResultField, Object> result = withFriendlyErrors(() -> dataService.getData(access.user(), access.bucket(),
                Optional.empty(), filter(access.user(), rules, logic), pageNo, rows, order));
        long total = (long) result.get(ResultField.TOTAL);
        @SuppressWarnings("unchecked")
        List<DataDTO> data = (List<DataDTO>) result.get(ResultField.DATA);
        return json(rows > 0
                ? new DataPage(total, pageNo, rows, (int) Math.ceil(total / (double) rows), data)
                : new DataPage(total, null, 0, null, null));
    }

    @McpTool(name = "get_data", description = "Gets data rows of a bucket by their ids (ids that don't exist, or rows "
            + "of others in a bucket with protected data, are left out).",
            annotations = @McpTool.McpAnnotations(title = "Get data by ids", readOnlyHint = true, destructiveHint = false,
                    idempotentHint = true, openWorldHint = false))
    public String getData(
            @McpToolParam(description = "Bucket name") String bucket,
            @McpToolParam(description = "Data row ids") List<Long> ids) throws Exception {
        requireIds(ids);
        Access access = access(bucket);
        return json(dataService.getData(access.user(), access.bucket(), ids));
    }

    // ---------------------------------------------------------------- reserving

    @McpTool(name = "reserve_data", description = "Reserves free (not reserved) data rows matching the rules for the "
            + "user and returns them, so parallel tests or agents never get the same row. Release the rows with "
            + "release_data when done. 'available' is the number of free rows still matching the rules.",
            annotations = @McpTool.McpAnnotations(title = "Reserve data", readOnlyHint = false, destructiveHint = false,
                    idempotentHint = false, openWorldHint = false))
    public String reserveData(
            @McpToolParam(description = "Bucket name") String bucket,
            @McpToolParam(description = RULES, required = false) List<Object> rules,
            @McpToolParam(description = LOGIC, required = false) Map<String, Object> logic,
            @McpToolParam(description = "Number of rows to reserve, 1.." + MAX_ROWS + ". Default 1.", required = false) Integer limit,
            @McpToolParam(description = SORT, required = false) String sort) throws Exception {
        int rows = limit != null ? limit : 1;
        if (rows < 1 || rows > MAX_ROWS)
            throw new IllegalArgumentException("The limit is 1.." + MAX_ROWS);
        Access access = access(bucket);
        String order = sort != null && !sort.isBlank() ? sort : "id";
        User user = access.user();

        List<Long> ids = withFriendlyErrors(() -> dataService.reserveData(user, access.bucket(),
                filter(user, rules, logic), rows, order, user.getUsername()));
        long available = dataService.countFreeData(access.bucket(), filter(user, rules, logic));
        if (ids == null || ids.isEmpty())
            return json(new Reservation(0, available, null, "No free data row matches the rules"));
        return json(new Reservation(ids.size(), available, dataService.getData(user, access.bucket(), ids), null));
    }

    @McpTool(name = "release_data", description = "Releases reserved data rows by their ids, so others can reserve them.",
            annotations = @McpTool.McpAnnotations(title = "Release data", readOnlyHint = false, destructiveHint = false,
                    idempotentHint = true, openWorldHint = false))
    public String releaseData(
            @McpToolParam(description = "Bucket name") String bucket,
            @McpToolParam(description = "Data row ids") List<Long> ids) throws Exception {
        requireIds(ids);
        Access access = access(bucket);
        DataModifyDTO change = new DataModifyDTO();
        change.setReserved(false);
        int count = withFriendlyErrors(() -> dataService.modifyData(access.user(), access.bucket(), Optional.of(ids),
                change, filter(access.user(), null, null)));
        return json(new Count(count, "Released " + count + " data row(s)"));
    }

    // ---------------------------------------------------------------- writing

    @McpTool(name = "insert_data", description = "Inserts data rows into a bucket and returns them with their ids. "
            + "All rows or none. Use the property paths and tag ids from describe_bucket.",
            annotations = @McpTool.McpAnnotations(title = "Insert data", readOnlyHint = false, destructiveHint = false,
                    idempotentHint = false, openWorldHint = false))
    public String insertData(
            @McpToolParam(description = "Bucket name") String bucket,
            @McpToolParam(description = "The rows (1.." + MAX_ROWS + "), each {\"properties\": {...the data...}, "
                    + "\"tagId\": optional tag id, \"reserved\": optional, true reserves the row for the user}")
            List<DataCreateDTO> rows) throws Exception {
        if (rows == null || rows.isEmpty() || rows.size() > MAX_ROWS)
            throw new IllegalArgumentException("Give 1.." + MAX_ROWS + " rows");
        Access access = access(bucket);
        List<DataDTO> inserted = withFriendlyErrors(() -> transactionTemplate.execute(status -> {
            List<DataDTO> result = new ArrayList<>();
            for (DataCreateDTO row : rows) {
                try {
                    result.add(dataService.createData(access.user(), access.bucket(), row));
                } catch (RuntimeException e) {
                    throw e;
                } catch (Exception e) {
                    throw new IllegalStateException(e.getMessage(), e);
                }
            }
            return result;
        }));
        return json(inserted);
    }

    @McpTool(name = "update_data", description = "Changes data rows selected by ids or by rules: sets or removes "
            + "properties, sets the tag, reserves or releases. Items not given stay unchanged. Reserving changes only "
            + "rows that are free. Returns the number of changed rows.",
            annotations = @McpTool.McpAnnotations(title = "Update data", readOnlyHint = false, destructiveHint = false,
                    idempotentHint = true, openWorldHint = false))
    public String updateData(
            @McpToolParam(description = "Bucket name") String bucket,
            @McpToolParam(description = "Data row ids; either ids or rules/logic", required = false) List<Long> ids,
            @McpToolParam(description = RULES + " Either ids or rules/logic.", required = false) List<Object> rules,
            @McpToolParam(description = LOGIC, required = false) Map<String, Object> logic,
            @McpToolParam(description = "Properties to set, by path: {\"$.status\": \"used\", \"$.address.city\": "
                    + "\"Paris\"}; other properties stay", required = false) Map<String, Object> propertiesToSet,
            @McpToolParam(description = "Property paths to remove, e.g. [\"$.comment\"]", required = false)
            List<String> propertiesToRemove,
            @McpToolParam(description = "Tag id to set", required = false) Long tagId,
            @McpToolParam(description = "true reserves the rows for the user, false releases them", required = false)
            Boolean reserved) throws Exception {
        boolean byIds = ids != null && !ids.isEmpty();
        if (byIds == hasRules(rules, logic))
            throw new IllegalArgumentException("Select the rows either by ids or by rules/logic");
        if (isEmpty(propertiesToSet) && (propertiesToRemove == null || propertiesToRemove.isEmpty())
                && tagId == null && reserved == null)
            throw new IllegalArgumentException("Nothing to change: give propertiesToSet, propertiesToRemove, tagId or reserved");
        Access access = access(bucket);

        DataModifyDTO change = new DataModifyDTO();
        change.setPropertiesToSet(propertiesToSet);
        change.setPropertiesToRemove(propertiesToRemove);
        change.setTagId(tagId);
        change.setReserved(reserved);
        int count = withFriendlyErrors(() -> dataService.modifyData(access.user(), access.bucket(),
                byIds ? Optional.of(ids) : Optional.empty(), change, filter(access.user(), rules, logic)));
        return json(new Count(count, "Modified " + count + " data row(s)"));
    }

    @McpTool(name = "delete_data", description = "Removes data rows, with their history, selected by ids or by rules. "
            + "Can't be undone. Needs the ROBOT role or higher (not AGENT). Returns the number of removed rows.",
            annotations = @McpTool.McpAnnotations(title = "Delete data", readOnlyHint = false, destructiveHint = true,
                    idempotentHint = true, openWorldHint = false))
    public String deleteData(
            @McpToolParam(description = "Bucket name") String bucket,
            @McpToolParam(description = "Data row ids; either ids or rules/logic", required = false) List<Long> ids,
            @McpToolParam(description = RULES + " Either ids or rules/logic.", required = false) List<Object> rules,
            @McpToolParam(description = LOGIC, required = false) Map<String, Object> logic) throws Exception {
        boolean byIds = ids != null && !ids.isEmpty();
        if (byIds == hasRules(rules, logic))
            throw new IllegalArgumentException("Select the rows either by ids or by rules/logic");
        Access access = access(bucket);
        if (!access.user().hasRoleAtLeast(Constants.ROLE_ROBOT))
            throw new IllegalArgumentException("Removing data rows needs the ROBOT role or higher; the user's role is "
                    + access.user().getTopRole().getName());

        int count = withFriendlyErrors(() -> byIds
                ? dataService.deleteDataByIds(access.user(), access.bucket(), ids)
                : dataService.deleteDataByRules(access.user(), access.bucket(), filter(access.user(), rules, logic)));
        return json(new Count(count, "Removed " + count + " data row(s)"));
    }

    // ---------------------------------------------------------------- history

    @McpTool(name = "search_history", description = "Searches the history of changes of a bucket (only buckets with "
            + "history). Each entry: id, dataId (the row), tagId, reserved, owner, modifiedBy, modifiedAt, and with "
            + "properties=true the properties of that version. Select the rows with rules/logic (by their current "
            + "state, e.g. [[\"id\", \"==\", 5]]) and the entries with historyRules (fields id, dataId, tagId, reserved, "
            + "modifiedBy, modifiedAt; e.g. [[\"modifiedBy\", \"==\", \"anna\"]]).",
            annotations = @McpTool.McpAnnotations(title = "Search history", readOnlyHint = true, destructiveHint = false,
                    idempotentHint = true, openWorldHint = false))
    public String searchHistory(
            @McpToolParam(description = "Bucket name") String bucket,
            @McpToolParam(description = RULES, required = false) List<Object> rules,
            @McpToolParam(description = LOGIC, required = false) Map<String, Object> logic,
            @McpToolParam(description = "Rules on the history entries, same format as rules", required = false)
            List<Object> historyRules,
            @McpToolParam(description = "Page number, from 1. Default 1.", required = false) Integer page,
            @McpToolParam(description = "Entries per page, 0.." + MAX_ROWS + ". Default 20.", required = false) Integer limit,
            @McpToolParam(description = "id, dataId, tagId, reserved, modifiedBy or modifiedAt, e.g. desc(modifiedAt). "
                    + "Default id.", required = false) String sort,
            @McpToolParam(description = "true: also the properties of each version", required = false)
            Boolean properties) throws Exception {
        int pageNo = page != null ? page : 1;
        int rows = limit != null ? limit : 20;
        if (pageNo < 1 || rows < 0 || rows > MAX_ROWS)
            throw new IllegalArgumentException("The page starts from 1 and the limit is 0.." + MAX_ROWS);
        Access access = access(bucket);
        if (!access.bucket().isHistory())
            throw new IllegalArgumentException("The bucket '" + bucket + "' keeps no history");

        Object historyLogic = historyRules != null && !historyRules.isEmpty()
                ? RulesToLogic.convert(historyRules, LogicSql.Fields.HISTORY.names()) : null;
        DataHistorySearchService.Result result = withFriendlyErrors(() -> historySearch.search(access.user(),
                access.bucket(), filter(access.user(), rules, logic), historyLogic, pageNo, rows,
                sort != null && !sort.isBlank() ? sort : "id", Boolean.TRUE.equals(properties)));
        return json(rows > 0
                ? new HistoryPage(result.total(), pageNo, rows, (int) Math.ceil(result.total() / (double) rows), result.entries())
                : new HistoryPage(result.total(), null, 0, null, null));
    }

    // ---------------------------------------------------------------- helpers

    record Access(User user, Bucket bucket) {
    }

    static IllegalArgumentException noBucket(String bucketName) {
        return new IllegalArgumentException("There is no bucket '" + bucketName + "' you have access to; see list_buckets");
    }

    private Access access(String bucketName) {
        if (bucketName == null || bucketName.isBlank())
            throw new IllegalArgumentException("The bucket name is missing");
        User user = userService.getCurrentUser();
        Bucket bucket = bucketService.getBucket(bucketName);
        if (bucket == null || !bucketService.hasUserAccessToBucket(bucket, user))
            throw noBucket(bucketName);
        return new Access(user, bucket);
    }

    private static LogicFilter filter(User user, List<Object> rules, Map<String, Object> logic) throws Exception {
        DataGetDTO searchRules = new DataGetDTO();
        searchRules.setRules(rules);
        searchRules.setLogic(isEmpty(logic) ? null : logic);
        return LogicFilter.of(user.getUsername(), (SearchRules) searchRules);
    }

    private static boolean hasRules(List<Object> rules, Map<String, Object> logic) {
        return (rules != null && !rules.isEmpty()) || !isEmpty(logic);
    }

    private static boolean isEmpty(Map<String, Object> map) {
        return map == null || map.isEmpty();
    }

    private static void requireIds(List<Long> ids) {
        if (ids == null || ids.isEmpty())
            throw new IllegalArgumentException("Give at least one id");
        if (ids.size() > MAX_ROWS)
            throw new IllegalArgumentException("At most " + MAX_ROWS + " ids at once");
    }

    @FunctionalInterface
    interface Call<T> {
        T run() throws Exception;
    }

    /** The database errors the REST API also turns into messages, without the SQL. */
    private static <T> T withFriendlyErrors(Call<T> call) throws Exception {
        try {
            return call.run();
        } catch (DataIntegrityViolationException e) {
            String message = String.valueOf(e.getMessage());
            if (message.contains("is not present in table \"tags\""))
                throw new IllegalArgumentException("The tag doesn't exist; see the tags in describe_bucket");
            if (message.contains("cannot cast jsonb null"))
                throw new IllegalArgumentException("A property in the rules is empty and can't be compared");
            throw new IllegalArgumentException("The data could not be saved: " + e.getMostSpecificCause().getMessage());
        }
    }

    private String json(Object value) throws JsonProcessingException {
        return objectMapper.writeValueAsString(value);
    }
}
