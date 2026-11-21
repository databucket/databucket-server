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
import pl.databucket.server.dto.IndexedPropertyDto;
import pl.databucket.server.entity.Bucket;
import pl.databucket.server.entity.User;
import pl.databucket.server.service.BucketService;
import pl.databucket.server.service.UserService;
import pl.databucket.server.service.data.DataHistorySearchService;
import pl.databucket.server.service.data.DataService;
import pl.databucket.server.service.data.ResultField;
import pl.databucket.server.service.data.SearchRules;
import pl.databucket.server.service.data.filter.LogicFilter;
import pl.databucket.server.service.data.filter.LogicSql;
import pl.databucket.server.service.data.filter.RulesToLogic;
import pl.databucket.server.service.data.generator.DataGenerateService;
import pl.databucket.server.dto.DataGenerateDTO;
import pl.databucket.server.dto.TaskRunDTO;
import pl.databucket.server.exception.BucketNotFoundException;
import pl.databucket.server.service.TaskAction;
import pl.databucket.server.service.TaskRunService;
import pl.databucket.server.audit.AuditService;

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
    static final int MAX_GENERATED = DataGenerateService.MAX_ROWS;
    private static final String RULES = "Rules selecting the data rows, combined with 'and'; each rule is "
            + "[left, operator, right], e.g. [\"$.city\", \"==\", \"Paris\"], or {\"or\": [rule, ...]}, {\"and\": [...]}, "
            + "{\"!\": rule}. Left: a property path ($.name, $.address.city) or a field (id, tagId, owner, reserved, "
            + "createdBy, createdAt, modifiedBy, modifiedAt). Operators: ==, !=, >, >=, <, <=, in, !in (right: a list), "
            + "is, is not (right: null), like, !like (% and _), similar, !similar, ~, !~, ~*, !~* (regular expressions). "
            + "Values: \"@currentUser\" = the signed-in user; \"@now\", \"@now-2h\", \"@now+1d\" (s, m, h, d, w) = "
            + "the current moment, e.g. [\"modifiedAt\", \"<\", \"@now-1d\"]. Empty or missing: all rows.";
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
    private final McpBucketCreator bucketCreator;
    private final DataGenerateService generateService;
    private final TaskRunService taskRunService;
    private final AuditService auditService;

    public McpTools(ObjectMapper objectMapper, DataService dataService, DataHistorySearchService historySearch,
                    BucketService bucketService, UserService userService, McpBucketInfoService bucketInfo,
                    TransactionTemplate transactionTemplate, McpBucketCreator bucketCreator,
                    DataGenerateService generateService, TaskRunService taskRunService, AuditService auditService) {
        this.taskRunService = taskRunService;
        this.auditService = auditService;
        this.bucketCreator = bucketCreator;
        this.generateService = generateService;
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

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public record CreatedBucket(McpBucketInfoService.BucketDescription bucket, String view, List<String> enums,
                                List<String> skipped, String message) {
    }

    public record Generated(int inserted, long firstN, long lastN, List<Map<String, Object>> sample, String message) {
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
            + "name). Call it before searching or writing data, to use the right property paths and tag ids. "
            + "indexedProperties are fast to search in a big bucket: prefer rules on them. tasks are the saved tasks "
            + "of the bucket, run with run_task.",
            annotations = @McpTool.McpAnnotations(title = "Describe a bucket", readOnlyHint = true, destructiveHint = false,
                    idempotentHint = true, openWorldHint = false))
    public String describeBucket(
            @McpToolParam(description = "Bucket name, as returned by list_buckets") String bucket) throws JsonProcessingException {
        return json(bucketInfo.describeBucket(bucket));
    }

    @McpTool(name = "create_bucket", description = "Creates a new bucket ready to use: its data class with the fields "
            + "(types, enums with their values), the bucket with the data and history tables, the columns and a view "
            + "of the data table in the web application, all named after the bucket. Give an example row in 'sample' "
            + "(the types are guessed from the values) and/or the fields in 'fields' (they win, by path). Needs the "
            + "ADMIN role. Returns the bucket as describe_bucket does; fill it with generate_data or insert_data.",
            annotations = @McpTool.McpAnnotations(title = "Create a bucket", readOnlyHint = false, destructiveHint = false,
                    idempotentHint = false, openWorldHint = false))
    public String createBucket(
            @McpToolParam(description = "Name of the new bucket, 1..30 characters, unique in the project") String name,
            @McpToolParam(description = "What the data is for", required = false) String description,
            @McpToolParam(description = "An example row, e.g. {\"login\": \"anna\", \"age\": 34, \"active\": true, "
                    + "\"birthDate\": \"1990-05-01\", \"address\": {\"city\": \"Paris\"}}: each value gives a field "
                    + "(nested objects give paths such as $.address.city): a number -> numeric, true/false -> boolean, "
                    + "yyyy-MM-dd -> date, an ISO time stamp -> datetime, HH:mm -> time, other -> string. Lists get no "
                    + "field.", required = false) Map<String, Object> sample,
            @McpToolParam(description = "The fields, or changes to the fields of the sample: [{\"path\": \"$.status\", "
                    + "\"type\": \"enum\", \"values\": [\"active\", \"blocked\"]}, {\"path\": \"$.age\", \"title\": "
                    + "\"Age\", \"type\": \"numeric\"}]. Types: string, numeric, boolean, date, datetime, time, enum "
                    + "(with values). The title defaults to the path (postCode -> Post code).", required = false)
            List<Map<String, Object>> fields,
            @McpToolParam(description = "Keep the history of changes of each row. Default true.", required = false)
            Boolean history,
            @McpToolParam(description = "Protected data: users see only the rows they reserved (admins see all). "
                    + "Default false.", required = false) Boolean protectedData,
            @McpToolParam(description = "Up to 5 properties with a database index, for big buckets searched by them: "
                    + "[{\"path\": \"$.login\", \"type\": \"text\"}, {\"path\": \"$.age\", \"type\": \"number\"}]",
                    required = false) List<IndexedPropertyDto> indexedProperties,
            @McpToolParam(description = "The lowest role that may use the bucket: AGENT (default: everyone in the "
                    + "project, also AI agents), ROBOT, MEMBER or ADMIN", required = false) String access) throws Exception {
        User user = userService.getCurrentUser();
        if (!user.hasRoleAtLeast(Constants.ROLE_ADMIN))
            throw new IllegalArgumentException("Creating a bucket needs the ADMIN role; the user's role is "
                    + user.getTopRole().getName() + ". Ask an admin, or sign in as one.");
        BucketSchema.Result schema = BucketSchema.of(sample, fields);
        McpBucketCreator.Created created = bucketCreator.create(new McpBucketCreator.Request(name, description,
                schema.fields(), history == null || history, Boolean.TRUE.equals(protectedData), indexedProperties, access));
        return json(new CreatedBucket(bucketInfo.describeBucket(name.trim()), created.view(), created.enums(),
                schema.skipped(), "Created the bucket '" + name.trim() + "' with the data class '" + created.dataClass()
                + "' and the view '" + created.view() + "'. Fill it with generate_data."));
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

    @McpTool(name = "generate_data", description = "Generates random data rows on the server and inserts them into "
            + "a bucket: give a generator per property instead of writing the rows, up to " + MAX_GENERATED
            + " rows per call. All rows or none. Returns the number of rows, the range of {n} used and a sample. "
            + "Use the property paths and enum values from describe_bucket.",
            annotations = @McpTool.McpAnnotations(title = "Generate data", readOnlyHint = false, destructiveHint = false,
                    idempotentHint = false, openWorldHint = false))
    public String generateData(
            @McpToolParam(description = "Bucket name") String bucket,
            @McpToolParam(description = "Property path -> generator, e.g. {\"$.firstName\": \"#{Name.firstName}\", "
                    + "\"$.email\": \"user-{n}@example.com\", \"$.address.city\": \"#{Address.city}\", "
                    + "\"$.phone\": \"#{numerify '###-###-###'}\", \"$.age\": {\"number\": [18, 90]}, "
                    + "\"$.balance\": {\"number\": [0, 5000], \"decimals\": 2}, "
                    + "\"$.status\": {\"oneOf\": [\"active\", \"blocked\"], \"weights\": [9, 1]}, "
                    + "\"$.birthDate\": {\"date\": [\"1950-01-01\", \"2005-12-31\"]}, "
                    + "\"$.lastLogin\": {\"datetime\": [\"2025-01-01\", \"2025-12-31T23:59:59Z\"]}, "
                    + "\"$.type\": \"customer\"}. A text with #{...} is a Datafaker expression (providers such as "
                    + "Name, Address, Internet, PhoneNumber, Company, Commerce, Finance, Lorem; numerify ###, "
                    + "letterify ???, regexify); {n} in a text is the number of the row, unique; any other text, "
                    + "number, boolean or null is the same in every row.")
            Map<String, Object> fields,
            @McpToolParam(description = "Number of rows, 1.." + MAX_GENERATED) Integer count,
            @McpToolParam(description = "Tag id of the rows", required = false) Long tagId,
            @McpToolParam(description = "true reserves the rows for the user", required = false) Boolean reserved,
            @McpToolParam(description = "Language of the values, e.g. en (default), pl, de, fr-FR", required = false)
            String locale,
            @McpToolParam(description = "Seed: the same seed and fields give the same rows again", required = false)
            Long seed,
            @McpToolParam(description = "Number {n} of the first row. Default: after the last id of the bucket, so {n} "
                    + "stays unique between calls", required = false) Long startN) throws Exception {
        DataGenerateDTO request = new DataGenerateDTO();
        request.setFields(fields);
        request.setCount(count);
        request.setTagId(tagId);
        request.setReserved(reserved);
        request.setLocale(locale);
        request.setSeed(seed);
        request.setStartN(startN);
        Access access = access(bucket);
        DataGenerateService.Generated generated = withFriendlyErrors(() ->
                generateService.generate(access.user(), access.bucket(), request));
        return json(new Generated(generated.inserted(), generated.firstN(), generated.lastN(), generated.rows(),
                "Inserted " + generated.inserted() + " data row(s)"));
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

    @McpTool(name = "run_task", description = "Runs a saved task of a bucket (see the tasks in describe_bucket): "
            + "modify (sets the tag, the reservation or properties), remove (removes rows with their history, needs "
            + "ROBOT or higher), clear history (needs ROBOT or higher) or reserve. It runs on the rows matching the "
            + "task's filter and the given rules together; a task without a filter needs rules. Use preview=true "
            + "first to see how many rows it would change. Returns the number of rows.",
            annotations = @McpTool.McpAnnotations(title = "Run a task", readOnlyHint = false, destructiveHint = true,
                    idempotentHint = false, openWorldHint = false))
    public String runTask(
            @McpToolParam(description = "Bucket name") String bucket,
            @McpToolParam(description = "Task id or name, from describe_bucket") String task,
            @McpToolParam(description = RULES + " Narrow the task's filter; required when the task has no filter.",
                    required = false) List<Object> rules,
            @McpToolParam(description = LOGIC, required = false) Map<String, Object> logic,
            @McpToolParam(description = "true: change nothing, only count the rows the task would run on. Default false.",
                    required = false) Boolean preview) throws Exception {
        if (task == null || task.isBlank())
            throw new IllegalArgumentException("Give the task id or name; see the tasks in describe_bucket");
        TaskRunDTO request = new TaskRunDTO();
        request.setRules(rules);
        request.setLogic(isEmpty(logic) ? null : logic);
        try {
            return json(withFriendlyErrors(() -> taskRunService.run(bucket, task.trim(), request, Boolean.TRUE.equals(preview))));
        } catch (BucketNotFoundException e) {
            throw noBucket(bucket);
        } catch (TaskRunService.TaskNotFoundException e) {
            throw new IllegalArgumentException("The bucket '" + bucket + "' has no task '" + task
                    + "'; see the tasks in describe_bucket");
        } catch (TaskAction.InvalidTaskException | TaskRunService.ForbiddenTaskException e) {
            throw new IllegalArgumentException(e.getMessage());
        }
    }

    // ---------------------------------------------------------------- activity

    @McpTool(name = "get_activity", description = "Lists the changes of the project's settings, newest first: who "
            + "created, changed or deleted which bucket, class, view, task, team, user..., with the properties before "
            + "and after; also sign-ins, task runs and bulk data operations (action e.g. auth.signin, data.delete). "
            + "Only for ADMIN or higher. Filters are optional.",
            annotations = @McpTool.McpAnnotations(title = "Get activity", readOnlyHint = true, destructiveHint = false,
                    idempotentHint = true, openWorldHint = false))
    public String getActivity(
            @McpToolParam(description = "Who made the change (user name)", required = false) String actor,
            @McpToolParam(description = "The type of the object: Bucket, Class, Columns, Enum, Filter, Group, Icon, "
                    + "Tag, Task, Schedule, Team, View, Project, User, Account (sign-ins), Data (bulk operations)",
                    required = false) String type,
            @McpToolParam(description = "CREATE, UPDATE, DELETE or an operation such as auth.signin, data.delete",
                    required = false) String action,
            @McpToolParam(description = "From: 2026-11-21T10:00:00Z or relative, e.g. @now-1d, @now-2h", required = false)
            String from,
            @McpToolParam(description = "To, as from", required = false) String to,
            @McpToolParam(description = "Page number, from 1. Default 1.", required = false) Integer page,
            @McpToolParam(description = "Events per page, 1.." + MAX_ROWS + ". Default 20.", required = false) Integer limit)
            throws Exception {
        User user = userService.getCurrentUser();
        if (!user.hasRoleAtLeast(Constants.ROLE_ADMIN))
            throw new IllegalArgumentException("The activity of the project needs the ADMIN role or higher; the user's role is "
                    + user.getTopRole().getName());
        Integer projectId = currentProject();
        if (projectId == null)
            throw new IllegalArgumentException("Sign in to a project");
        int rows = limit != null ? limit : 20;
        if (rows < 1 || rows > MAX_ROWS)
            throw new IllegalArgumentException("The limit is 1.." + MAX_ROWS);
        AuditService.Filter filter;
        try {
            filter = new AuditService.Filter(projectId, false, actor, type, action, AuditService.instant(from),
                    AuditService.instant(to));
        } catch (java.time.format.DateTimeParseException e) {
            throw new IllegalArgumentException("A date like 2026-11-21T10:00:00Z or @now-1d, not: " + e.getParsedString());
        }
        return json(auditService.search(filter, page != null ? page : 1, rows));
    }

    private static Integer currentProject() {
        Object principal = org.springframework.security.core.context.SecurityContextHolder.getContext()
                .getAuthentication().getPrincipal();
        return principal instanceof pl.databucket.server.security.CustomUserDetails details ? details.getProjectId() : null;
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
