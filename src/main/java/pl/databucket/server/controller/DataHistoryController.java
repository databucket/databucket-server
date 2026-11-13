package pl.databucket.server.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import pl.databucket.server.configuration.OpenApiConfiguration;
import pl.databucket.server.dto.DataHistoryGetDTO;
import pl.databucket.server.exception.InvalidRuleException;
import pl.databucket.server.exception.UnknownColumnException;
import pl.databucket.server.response.GetDataHistoryResponse;
import pl.databucket.server.service.data.DataHistorySearchService;
import pl.databucket.server.service.data.v2.LogicFilter;
import pl.databucket.server.service.data.v2.LogicSql;
import pl.databucket.server.service.data.v2.RulesToLogic;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import pl.databucket.server.dto.DataRemoveDTO;
import pl.databucket.server.entity.Bucket;
import pl.databucket.server.entity.User;
import pl.databucket.server.exception.BucketNotFoundException;
import pl.databucket.server.exception.DataNotFoundException;
import pl.databucket.server.exception.ExceptionFormatter;
import pl.databucket.server.exception.NoAccessToBucketException;
import pl.databucket.server.response.MessageResponse;
import pl.databucket.server.service.BucketService;
import pl.databucket.server.service.UserService;
import pl.databucket.server.service.data.DataService;
import pl.databucket.server.service.data.DataFilters;

import java.util.List;

@Tag(name = "Data history", description = "The changes of the data rows (buckets with history enabled). "
        + "Requires the AGENT role or higher and access to the bucket (clearing the history: ROBOT or higher); in a "
        + "bucket with protected data, below ADMIN, only the history of the rows reserved by the user.")
@SecurityRequirement(name = OpenApiConfiguration.BEARER)
@CrossOrigin(origins = "*", allowedHeaders = "*")
@PreAuthorize("hasRole('AGENT')")
@RequestMapping("/api")
@RestController
public class DataHistoryController {

    private final ExceptionFormatter exceptionFormatter;
    private final DataService service;

    @Autowired
    private BucketService bucketService;

    @Autowired
    private UserService userService;

    @Autowired
    private DataService dataService;

    @Autowired
    private DataHistorySearchService historySearch;

    public DataHistoryController(DataService service) {
        this.service = service;
        this.exceptionFormatter = new ExceptionFormatter(DataHistoryController.class);
    }

    @Operation(summary = "History of a data row", description = "The changes of one data row, oldest first; only the "
            + "items that changed are set (properties: true when they changed; get them with 'History properties').")
    @GetMapping(value = {
            "/bucket/{bucketName}/{id}/history"},
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> getDataHistory(
            @PathVariable String bucketName,
            @PathVariable Long id) {

        Bucket bucket = bucketService.getBucket(bucketName);
        if (bucket == null)
            return exceptionFormatter.customException(new BucketNotFoundException(bucketName), HttpStatus.NOT_FOUND);

        try {
            User user = userService.getCurrentUser();
            if (bucketService.hasUserAccessToBucket(bucket, user)) {
                return new ResponseEntity<>(service.getDataHistory(user, bucket, id), HttpStatus.OK);
            } else
                return exceptionFormatter.customException(new NoAccessToBucketException(bucketName), HttpStatus.NOT_FOUND);
        } catch (DataNotFoundException e) {
            return exceptionFormatter.customException(e, HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            return exceptionFormatter.defaultException(e);
        }
    }

    @Operation(summary = "History properties", description = "The properties of a data row as they were in the given "
            + "history entries (ids of the entries, comma-separated).")
    @GetMapping(value = {
            "/bucket/{bucketName}/{id}/history/{ids}"},
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> getDataHistoryProperties(
            @PathVariable String bucketName,
            @PathVariable Long id,
            @PathVariable List<Long> ids) {

        Bucket bucket = bucketService.getBucket(bucketName);
        if (bucket == null)
            return exceptionFormatter.customException(new BucketNotFoundException(bucketName), HttpStatus.NOT_FOUND);

        try {
            User user = userService.getCurrentUser();
            if (bucketService.hasUserAccessToBucket(bucket, user)) {
                return new ResponseEntity<>(service.getDataHistoryProperties(user, bucket, id, ids), HttpStatus.OK);
            } else
                return exceptionFormatter.customException(new NoAccessToBucketException(bucketName), HttpStatus.NOT_FOUND);
        } catch (DataNotFoundException e) {
            return exceptionFormatter.customException(e, HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            return exceptionFormatter.defaultException(e);
        }
    }


    @Operation(summary = "Clear the history of a data row", description = "Removes the history entries of one data row; "
            + "the row stays.")
    @PreAuthorize("hasRole('ROBOT')")
    @DeleteMapping(value = {
            "/bucket/{bucketName}/{id}/history/clear"},
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> clearHistory(
//            @ApiParam(value = "bucket name", required = true)
            @PathVariable String bucketName,
//            @ApiParam(value = "id", example = "1", required = true)
            @PathVariable Long id
            ) {

        Bucket bucket = bucketService.getBucket(bucketName);
        if (bucket == null)
            return exceptionFormatter.customException(new BucketNotFoundException(bucketName), HttpStatus.NOT_FOUND);

        try {
            User user = userService.getCurrentUser();
            if (bucketService.hasUserAccessToBucket(bucket, user)) {
                int count = dataService.clearDataHistory(user, bucket, id);
                return new ResponseEntity<>(new MessageResponse("Removed " + count + " data history row(s)"), HttpStatus.OK);
            } else
                return exceptionFormatter.customException(new NoAccessToBucketException(bucketName), HttpStatus.NOT_FOUND);
        } catch (DataNotFoundException e) {
            return exceptionFormatter.customException(e, HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            return exceptionFormatter.defaultException(e);
        }
    }


    @Operation(summary = "Clear the history by rules", description = "Removes the history of all data rows matching the "
            + "rules (at least one rule); the rows stay.")
    @PreAuthorize("hasRole('ROBOT')")
    @DeleteMapping(value = {
            "/bucket/{bucketName}/history/clear"},
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> clearHistory(
//            @ApiParam(value="bucket name", required = true)
            @PathVariable String bucketName,
            @RequestHeader(value = DataFilters.VERSION_HEADER, required = false) String apiVersion,
            @RequestBody DataRemoveDTO dataRemoveDTO) {

        if ((dataRemoveDTO.getConditions() == null || dataRemoveDTO.getConditions().size() == 0)
                && (dataRemoveDTO.getRules() == null || dataRemoveDTO.getRules().size() == 0)
                && dataRemoveDTO.getLogic() == null)
            return new ResponseEntity<>(new MessageResponse("Can not modify data history without any rules!"), HttpStatus.NOT_ACCEPTABLE);

        Bucket bucket = bucketService.getBucket(bucketName);
        if (bucket == null)
            return exceptionFormatter.customException(new BucketNotFoundException(bucketName), HttpStatus.NOT_FOUND);
        try {
            User user = userService.getCurrentUser();
            if (bucketService.hasUserAccessToBucket(bucket, user)) {
                int count = dataService.clearDataHistory(user, bucket, DataFilters.of(user.getUsername(), dataRemoveDTO, apiVersion));
                return new ResponseEntity<>(new MessageResponse("Removed history on " + count + " data rows."), HttpStatus.OK);
            } else
                return exceptionFormatter.customException(new NoAccessToBucketException(bucketName), HttpStatus.NOT_FOUND);
        } catch (pl.databucket.server.exception.InvalidRuleException | pl.databucket.server.exception.UnknownColumnException e) {
            return exceptionFormatter.customException(e, HttpStatus.BAD_REQUEST);
        } catch (Exception e) {
            return exceptionFormatter.defaultException(e);
        }
    }

    @Operation(summary = "Search the history", description = "History entries of a bucket, one page at a time: of the data "
            + "rows matching 'rules' / 'logic' (by their current state; without them all rows), filtered by "
            + "'historyRules' / 'historyLogic' on the fields of the entries (id, dataId, tagId, reserved, modifiedBy, "
            + "modifiedAt). Only with the header " + DataFilters.VERSION_HEADER + ": 2.")
    @PostMapping(value = "/bucket/{bucketName}/history/get", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> searchHistory(
            @PathVariable String bucketName,
            @Parameter(description = "Page number, starting from 1", example = "1")
            @RequestParam(required = false, defaultValue = "1") Integer page,
            @Parameter(description = "Entries per page (at most " + DataHistorySearchService.MAX_LIMIT + "); 0 only counts", example = "20")
            @RequestParam(required = false, defaultValue = "20") Integer limit,
            @Parameter(description = "id, dataId, tagId, reserved, modifiedBy or modifiedAt, e.g. desc(modifiedAt)", example = "desc(id)")
            @RequestParam(required = false, defaultValue = "id") String sort,
            @Parameter(description = "true: also the properties of each version (when they changed)")
            @RequestParam(required = false, defaultValue = "false") boolean properties,
            @Parameter(description = "Must be 2")
            @RequestHeader(value = DataFilters.VERSION_HEADER, required = false) String apiVersion,
            @RequestBody(required = false) DataHistoryGetDTO request) {

        if (!"2".equals(apiVersion == null ? null : apiVersion.trim()))
            return exceptionFormatter.customException("Searching the history needs the header " + DataFilters.VERSION_HEADER + ": 2", HttpStatus.BAD_REQUEST);
        if (page < 1 || limit < 0 || limit > DataHistorySearchService.MAX_LIMIT)
            return exceptionFormatter.customException("The page starts from 1 and the limit is 0.." + DataHistorySearchService.MAX_LIMIT, HttpStatus.BAD_REQUEST);
        DataHistoryGetDTO body = request != null ? request : new DataHistoryGetDTO();

        Bucket bucket = bucketService.getBucket(bucketName);
        if (bucket == null)
            return exceptionFormatter.customException(new BucketNotFoundException(bucketName), HttpStatus.NOT_FOUND);

        try {
            User user = userService.getCurrentUser();
            if (!bucketService.hasUserAccessToBucket(bucket, user))
                return exceptionFormatter.customException(new NoAccessToBucketException(bucketName), HttpStatus.NOT_FOUND);

            LogicFilter dataFilter = LogicFilter.of(user.getUsername(), body);
            List<Object> historyLogic = new java.util.ArrayList<>();
            if (body.getHistoryLogic() != null && !body.getHistoryLogic().isEmpty())
                historyLogic.add(body.getHistoryLogic());
            if (body.getHistoryRules() != null && !body.getHistoryRules().isEmpty())
                historyLogic.add(RulesToLogic.convert(body.getHistoryRules(), LogicSql.Fields.HISTORY.names()));

            DataHistorySearchService.Result result = historySearch.search(user, bucket, dataFilter,
                    historyLogic.isEmpty() ? null : java.util.Map.of("and", historyLogic), page, limit, sort, properties);

            GetDataHistoryResponse response = new GetDataHistoryResponse();
            response.setLimit(limit);
            response.setTotal(result.total());
            if (limit > 0) {
                response.setPage(page);
                response.setSort(sort);
                response.setTotalPages((int) Math.ceil(result.total() / (double) limit));
                response.setHistory(result.entries());
            }
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (InvalidRuleException | UnknownColumnException e) {
            return exceptionFormatter.customException(e, HttpStatus.BAD_REQUEST);
        } catch (Exception e) {
            return exceptionFormatter.defaultException(e);
        }
    }
}
