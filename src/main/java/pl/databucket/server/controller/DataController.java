package pl.databucket.server.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import pl.databucket.server.dto.*;
import pl.databucket.server.entity.Bucket;
import pl.databucket.server.entity.User;
import pl.databucket.server.exception.*;
import pl.databucket.server.response.GetDataResponse;
import pl.databucket.server.response.MessageResponse;
import pl.databucket.server.response.ReserveDataResponse;
import pl.databucket.server.service.BucketService;
import pl.databucket.server.service.UserService;
import pl.databucket.server.service.data.*;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@Tag(name = "SECURED", description = "Secured data operations")
@CrossOrigin(origins = "*", allowedHeaders = "*")
@RequestMapping("/api/bucket/{bucketName}")
@RestController
public class DataController {

    private final ExceptionFormatter exceptionFormatter = new ExceptionFormatter(DataController.class);

    @Autowired
    private DataService dataService;

    @Autowired
    private BucketService bucketService;

    @Autowired
    private UserService userService;


    @PreAuthorize("hasAnyRole('MEMBER', 'ROBOT')")
    @Operation(summary = "Insert data", description = "Inserts one data set into the selected bucket.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Created"),
            @ApiResponse(responseCode = "404", description = "Not found - the user doesn't have access to the bucket or the bucket was not found"),
            @ApiResponse(responseCode = "406", description = "Not acceptable - used not existing tagId"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PostMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> insertData(
            @Parameter(description = "bucket name", required = true)
            @PathVariable String bucketName,
            @Parameter(description = "payload - data", required = true)
            @RequestBody DataCreateDTO dataCreateDTO) {

        Bucket bucket = bucketService.getBucket(bucketName);
        if (bucket == null)
            return exceptionFormatter.customException(new BucketNotFoundException(bucketName), HttpStatus.NOT_FOUND);

        try {
            User user = userService.getCurrentUser();
            if (bucketService.hasUserAccessToBucket(bucket, user)) {
                DataDTO dataDTO = dataService.createData(user, bucket, dataCreateDTO);
                return new ResponseEntity<>(dataDTO, HttpStatus.CREATED);
            } else
                return exceptionFormatter.customException(new NoAccessToBucketException(bucketName), HttpStatus.NOT_FOUND);
        } catch (DataIntegrityViolationException e) {
            if (e.getMessage().contains("is not present in table \"tags\""))
                return exceptionFormatter.customException(new ItemNotFoundException(pl.databucket.server.entity.Tag.class, dataCreateDTO.getTagId()), HttpStatus.NOT_ACCEPTABLE);
            else
                return exceptionFormatter.customException(e, HttpStatus.NOT_ACCEPTABLE);
        } catch (Exception e) {
            return exceptionFormatter.defaultException(e);
        }
    }

    @PreAuthorize("hasAnyRole('MEMBER', 'ROBOT')")
    @Operation(summary = "Insert multi data", description = "Inserts multiple data sets into the selected bucket.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Created"),
            @ApiResponse(responseCode = "404", description = "Not found - the user doesn't have access to the bucket or the bucket was not found"),
            @ApiResponse(responseCode = "406", description = "Not acceptable - used not existing tagId"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PostMapping(value = {"/multi"}, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> insertMultiData(
            @Parameter(description = "bucket name", required = true)
            @PathVariable String bucketName,
            @Parameter(description = "payload - a list of data", required = true)
            @RequestBody List<DataCreateDTO> dataList) {

        Bucket bucket = bucketService.getBucket(bucketName);
        if (bucket == null)
            return exceptionFormatter.customException(new BucketNotFoundException(bucketName), HttpStatus.NOT_FOUND);

        try {
            User user = userService.getCurrentUser();
            if (bucketService.hasUserAccessToBucket(bucket, user)) {
                dataService.createData(user, bucket, dataList);
                return new ResponseEntity<>(HttpStatus.CREATED);
            } else
                return exceptionFormatter.customException(new NoAccessToBucketException(bucketName), HttpStatus.NOT_FOUND);
        } catch (DataIntegrityViolationException e) {
            return exceptionFormatter.customException(e, HttpStatus.NOT_ACCEPTABLE);
        } catch (Exception e) {
            return exceptionFormatter.defaultException(e);
        }
    }


    @PreAuthorize("hasAnyRole('MEMBER', 'ROBOT')")
    @Operation(summary = "Modify data by data ids", description = "Makes changes on data based on data ids.")
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "OK",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = MessageResponse.class),
                            examples = @ExampleObject(value = "{\n\t\"message\": \"Modified 3 data row(s)\"\n}")
                    )
            ),
            @ApiResponse(responseCode = "404", description = "Not found - the user doesn't have access to the bucket or the bucket was not found"),
            @ApiResponse(responseCode = "406", description = "Not acceptable - used not existing tagId"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PutMapping(value = {"/{ids}"}, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> modifyData(
            @Parameter(description = "bucket name", required = true)
            @PathVariable String bucketName,
            @Parameter(description = "ids", example = "1,2,3", required = true)
            @PathVariable Optional<List<Long>> ids,
            @Parameter(description = "payload - data details (rules are ignored in this endpoint)", required = true)
            @RequestBody DataModifyDTO dataModifyDTO) {

        Bucket bucket = bucketService.getBucket(bucketName);
        if (bucket == null)
            return exceptionFormatter.customException(new BucketNotFoundException(bucketName), HttpStatus.NOT_FOUND);

        try {
            User user = userService.getCurrentUser();
            if (bucketService.hasUserAccessToBucket(bucket, user)) {
                int count = dataService.modifyData(user, bucket, ids, dataModifyDTO, new QueryRule(user.getUsername(), dataModifyDTO));
                return new ResponseEntity<>(new MessageResponse("Modified " + count + " data row(s)"), HttpStatus.OK);
            } else
                return exceptionFormatter.customException(new NoAccessToBucketException(bucketName), HttpStatus.NOT_FOUND);
        } catch (DataIntegrityViolationException e) {
            if (Objects.requireNonNull(e.getMessage()).contains("is not present in table \"tags\""))
                return exceptionFormatter.customException(new ItemNotFoundException(pl.databucket.server.entity.Tag.class, dataModifyDTO.getTagId()), HttpStatus.NOT_ACCEPTABLE);
            else if (e.getMessage().contains("cannot cast jsonb null"))
                return exceptionFormatter.customException("Failed to operate on an empty property!", HttpStatus.NOT_ACCEPTABLE);
            else
                return exceptionFormatter.customException(e, HttpStatus.NOT_ACCEPTABLE);
        } catch (Exception e) {
            return exceptionFormatter.defaultException(e);
        }
    }

    @PreAuthorize("hasAnyRole('MEMBER', 'ROBOT')")
    @Operation(summary = "Modify data by rules", description = "Makes changes on data based on data rules.")
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "OK",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = MessageResponse.class),
                            examples = @ExampleObject(value = "{\n\t\"message\": \"Modified 3 data row(s)\"\n}")
                    )
            ),
            @ApiResponse(responseCode = "404", description = "Not found - the user doesn't have access to the bucket or the bucket was not found"),
            @ApiResponse(responseCode = "406", description = "Not acceptable - used not existing tagId or rules contain not existing property"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PutMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> modifyData(
            @Parameter(description = "bucket name", required = true)
            @PathVariable String bucketName,
            @Parameter(description = "payload - data details and rules", required = true)
            @RequestBody DataModifyDTO dataModifyDTO) {

        if ((dataModifyDTO.getConditions() == null || dataModifyDTO.getConditions().size() == 0)
                && (dataModifyDTO.getRules() == null || dataModifyDTO.getRules().size() == 0)
                && dataModifyDTO.getLogic() == null)
            return new ResponseEntity<>(new MessageResponse("Can not modify data without any rules!"), HttpStatus.NOT_ACCEPTABLE);

        Bucket bucket = bucketService.getBucket(bucketName);
        if (bucket == null)
            return exceptionFormatter.customException(new BucketNotFoundException(bucketName), HttpStatus.NOT_FOUND);

        try {
            User user = userService.getCurrentUser();
            if (bucketService.hasUserAccessToBucket(bucket, user)) {
                int count = dataService.modifyData(user, bucket, Optional.empty(), dataModifyDTO, new QueryRule(user.getUsername(), dataModifyDTO));
                return new ResponseEntity<>(new MessageResponse("Modified " + count + " data row(s)"), HttpStatus.OK);
            } else
                return exceptionFormatter.customException(new NoAccessToBucketException(bucketName), HttpStatus.NOT_FOUND);
        } catch (DataIntegrityViolationException e) {
            if (Objects.requireNonNull(e.getMessage()).contains("is not present in table \"tags\""))
                return exceptionFormatter.customException(new ItemNotFoundException(pl.databucket.server.entity.Tag.class, dataModifyDTO.getTagId()), HttpStatus.NOT_ACCEPTABLE);
            else if (e.getMessage().contains("cannot cast jsonb null"))
                return exceptionFormatter.customException("Failed to operate on an empty property!", HttpStatus.NOT_ACCEPTABLE);
            else
                return exceptionFormatter.customException(e, HttpStatus.NOT_ACCEPTABLE);
        } catch (InvalidRuleException e) {
            return exceptionFormatter.customException(e, HttpStatus.BAD_REQUEST);
        } catch (Exception e) {
            return exceptionFormatter.defaultException(e);
        }
    }


    @Operation(summary = "Get data by data ids", description = "Retrieves data based on data ids.")
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "OK",
                    content = @Content(schema = @Schema(implementation = DataDTO.class))
            ),
            @ApiResponse(responseCode = "404", description = "Not found - the user doesn't have access to the bucket or the bucket was not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PreAuthorize("hasAnyRole('MEMBER', 'ROBOT')")
    @GetMapping(value = {"/{ids}"}, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> getData(
            @Parameter(description = "bucket name", required = true)
            @PathVariable String bucketName,
            @Parameter(description = "ids", example = "1,2,3", required = true)
            @PathVariable List<Long> ids) {

        Bucket bucket = bucketService.getBucket(bucketName);
        if (bucket == null)
            return exceptionFormatter.customException(new BucketNotFoundException(bucketName), HttpStatus.NOT_FOUND);

        try {
            User user = userService.getCurrentUser();
            if (bucketService.hasUserAccessToBucket(bucket, user)) {
                List<DataDTO> dataDtoList = dataService.getData(user, bucket, ids);
                if (ids.size() == 1 && dataDtoList.size() == 1)
                    return new ResponseEntity<>(dataDtoList.get(0), HttpStatus.OK);
                else
                    return new ResponseEntity<>(dataDtoList, HttpStatus.OK);
            } else
                return exceptionFormatter.customException(new NoAccessToBucketException(bucketName), HttpStatus.NOT_FOUND);
        } catch (Exception ee) {
            return exceptionFormatter.defaultException(ee);
        }
    }


    @Operation(summary = "Remove data by data ids", description = "Removes data based on data ids.")
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "OK",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = MessageResponse.class),
                            examples = @ExampleObject(value = "{\n\t\"message\": \"Removed 3 data row(s)\"\n}")
                    )
            ),
            @ApiResponse(responseCode = "404", description = "Not found - the user doesn't have access to the bucket or the bucket was not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PreAuthorize("hasAnyRole('MEMBER', 'ROBOT')")
    @DeleteMapping(value = {"/{ids}"}, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> deleteData(
            @Parameter(description = "bucket name", required = true)
            @PathVariable String bucketName,
            @Parameter(description = "ids", example = "1,2,3", required = true)
            @PathVariable List<Long> ids) {

        Bucket bucket = bucketService.getBucket(bucketName);
        if (bucket == null)
            return exceptionFormatter.customException(new BucketNotFoundException(bucketName), HttpStatus.NOT_FOUND);

        try {
            User user = userService.getCurrentUser();
            if (bucketService.hasUserAccessToBucket(bucket, user)) {
                int count = dataService.deleteDataByIds(user, bucket, ids);
                return new ResponseEntity<>(new MessageResponse("Removed " + count + " data row(s)"), HttpStatus.OK);
            } else
                return exceptionFormatter.customException(new NoAccessToBucketException(bucketName), HttpStatus.NOT_FOUND);
        } catch (Exception ee) {
            return exceptionFormatter.defaultException(ee);
        }
    }

    @PreAuthorize("hasAnyRole('MEMBER', 'ROBOT')")
    @Operation(summary = "Get data by rules", description = "Retrieves data based on data rules. Define 'columns' to limit the retrieved data sets. Search for 'data' if columns are not defined and 'customData' if columns are defined.")
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "OK",
                    content = @Content(schema = @Schema(implementation = GetDataResponse.class))
            ),
            @ApiResponse(responseCode = "404", description = "Not found - the user doesn't have access to the bucket or the bucket was not found"),
            @ApiResponse(responseCode = "406", description = "Not acceptable - rules contain not existing property"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PostMapping(value = "/get", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> getData(
            @Parameter(description = "bucket name", required = true)
            @PathVariable String bucketName,
            @Parameter(description = "page", example = "1")
            @RequestParam(required = false, defaultValue = "1") Integer page,
            @Parameter(description = "limit", example = "1")
            @RequestParam(required = false, defaultValue = "1") Integer limit,
            @Parameter(description = "sort", example = "id")
            @RequestParam(required = false, defaultValue = "id") String sort,
            @Parameter(description = "payload - rules (required), columns (optional)", required = true)
            @RequestBody DataGetDTO dataGetDTO) {

        Bucket bucket = bucketService.getBucket(bucketName);
        if (bucket == null)
            return exceptionFormatter.customException(new BucketNotFoundException(bucketName), HttpStatus.NOT_FOUND);

        try {
            GetDataResponse response = new GetDataResponse();
            response.setLimit(limit);
            if (limit > 0) {
                response.setPage(page);
                response.setSort(sort);
            }

            User user = userService.getCurrentUser();
            if (bucketService.hasUserAccessToBucket(bucket, user)) {
                QueryRule queryRule = new QueryRule(user.getUsername(), dataGetDTO);
                Map<ResultField, Object> result = dataService.getData(user, bucket, Optional.ofNullable(dataGetDTO.getColumns()), queryRule, page, limit, sort);

                long total = (long) result.get(ResultField.TOTAL);
                response.setTotal(total);
                if (limit > 0)
                    response.setTotalPages((int) Math.ceil(total / (float) limit));
                if (result.containsKey(ResultField.DATA))
                    response.setData((List<DataDTO>) result.get(ResultField.DATA));
                else
                    response.setCustomData(result.get(ResultField.CUSTOM_DATA));

                if (response.getData() == null && response.getCustomData() == null && limit > 0)
                    response.setMessage("No data matches the rules!");

                return new ResponseEntity<>(response, HttpStatus.OK);
            } else
                return exceptionFormatter.customException(new NoAccessToBucketException(bucketName), HttpStatus.NOT_FOUND);
        } catch (DataIntegrityViolationException e) {
            if (Objects.requireNonNull(e.getMessage()).contains("cannot cast jsonb null"))
                return exceptionFormatter.customException("Failed to operate on an empty property!", HttpStatus.NOT_ACCEPTABLE);
            else
                return exceptionFormatter.customException(e, HttpStatus.NOT_ACCEPTABLE);
        } catch (InvalidRuleException e) {
            return exceptionFormatter.customException(e, HttpStatus.BAD_REQUEST);
        } catch (Exception ee) {
            return exceptionFormatter.defaultException(ee);
        }
    }

    @PreAuthorize("hasAnyRole('MEMBER', 'ROBOT')")
    @Operation(summary = "Reserve data by rules", description = "Reserves data based on data rules.")
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "OK",
                    content = @Content(schema = @Schema(implementation = ReserveDataResponse.class))
            ),
            @ApiResponse(responseCode = "404", description = "Not found - the user doesn't have access to the bucket or the bucket was not found"),
            @ApiResponse(responseCode = "406", description = "Not acceptable - rules contain not existing property"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PostMapping(value = {"/reserve"}, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> reserveData(
            @Parameter(description = "bucket name", example = "bucket", required = true)
            @PathVariable String bucketName,
            @Parameter(description = "limit", example = "1")
            @RequestParam(required = false, defaultValue = "1") Integer limit,
            @Parameter(description = "sort", example = "random")
            @RequestParam(required = false, defaultValue = "id") String sort,
            @Parameter(description = "payload - rules (required)", required = true)
            @RequestBody DataReserveDTO dataReserveDTO) {

        Bucket bucket = bucketService.getBucket(bucketName);
        if (bucket == null)
            return exceptionFormatter.customException(new BucketNotFoundException(bucketName), HttpStatus.NOT_FOUND);

        if (limit < 1)
            return exceptionFormatter.customException("The limit must be greater than 0!", HttpStatus.NOT_ACCEPTABLE);

        try {
            ReserveDataResponse response = new ReserveDataResponse();
            response.setLimit(limit);
            response.setSort(sort);

            User user = userService.getCurrentUser();
            if (bucketService.hasUserAccessToBucket(bucket, user)) {
                String targetOwnerUsername = user.getUsername();
                if (user.isAdminUser() && dataReserveDTO.getTargetOwnerUsername() != null)
                    targetOwnerUsername = dataReserveDTO.getTargetOwnerUsername();

                QueryRule queryRule = new QueryRule(user.getUsername(), dataReserveDTO);
                List<Long> dataIds = dataService.reserveData(user, bucket, queryRule, limit, sort, targetOwnerUsername);
                if (dataIds != null && dataIds.size() > 0)
                    response.setReserved(dataIds.size());
                else
                    response.setReserved(0);

                queryRule.getConditions().add(new Condition(COL.RESERVED, Operator.notEqual, true));
                Map<ResultField, Object> getDataResult = dataService.getData(user, bucket, Optional.empty(), queryRule, 0, 0, "id");
                response.setAvailable((Long) getDataResult.get(ResultField.TOTAL));

                if (dataIds != null && dataIds.size() > 0) {
                    List<DataDTO> dataDTOList = dataService.getData(user, bucket, dataIds);
                    response.setData(dataDTOList);
                } else if (limit > 0)
                    response.setMessage("No data matches the rules!");

                return new ResponseEntity<>(response, HttpStatus.OK);
            } else
                return exceptionFormatter.customException(new NoAccessToBucketException(bucketName), HttpStatus.NOT_FOUND);
        } catch (DataIntegrityViolationException e) {
            if (Objects.requireNonNull(e.getMessage()).contains("cannot cast jsonb null"))
                return exceptionFormatter.customException("Failed to operate on an empty property!", HttpStatus.NOT_ACCEPTABLE);
            else
                return exceptionFormatter.customException(e, HttpStatus.NOT_ACCEPTABLE);
        } catch (InvalidRuleException e) {
            return exceptionFormatter.customException(e, HttpStatus.BAD_REQUEST);
        } catch (Exception ee) {
            return exceptionFormatter.defaultException(ee);
        }
    }


    @PreAuthorize("hasAnyRole('MEMBER', 'ROBOT')")
    @Operation(summary = "Remove data by rules", description = "Removes data based on data rules.")
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "OK",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = MessageResponse.class),
                            examples = @ExampleObject(value = "{\n\t\"message\": \"Removed 3 data row(s)\"\n}")
                    )
            ),
            @ApiResponse(responseCode = "404", description = "Not found - the user doesn't have access to the bucket or the bucket was not found"),
            @ApiResponse(responseCode = "406", description = "Not acceptable - rules contain not existing property"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @DeleteMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> deleteData(
            @Parameter(description = "bucket name", required = true)
            @PathVariable String bucketName,
            @Parameter(description = "payload - rules (required)", required = true)
            @RequestBody DataRemoveDTO dataRemoveDTO) {

        if ((dataRemoveDTO.getConditions() == null || dataRemoveDTO.getConditions().size() == 0)
                && (dataRemoveDTO.getRules() == null || dataRemoveDTO.getRules().size() == 0)
                && dataRemoveDTO.getLogic() == null)
            return new ResponseEntity<>(new MessageResponse("Can not remove data without any rules!"), HttpStatus.NOT_ACCEPTABLE);

        Bucket bucket = bucketService.getBucket(bucketName);
        if (bucket == null)
            return exceptionFormatter.customException(new BucketNotFoundException(bucketName), HttpStatus.NOT_FOUND);

        try {
            User user = userService.getCurrentUser();
            if (bucketService.hasUserAccessToBucket(bucket, user)) {
                int count = dataService.deleteDataByRules(user, bucket, new QueryRule(user.getUsername(), dataRemoveDTO));
                return new ResponseEntity<>(new MessageResponse("Removed " + count + " data row(s)"), HttpStatus.OK);
            } else
                return exceptionFormatter.customException(new NoAccessToBucketException(bucketName), HttpStatus.NOT_FOUND);
        } catch (DataIntegrityViolationException e) {
            if (e.getMessage().contains("cannot cast jsonb null"))
                return exceptionFormatter.customException("Failed to operate on an empty property!", HttpStatus.NOT_ACCEPTABLE);
            else
                return exceptionFormatter.customException(e, HttpStatus.NOT_ACCEPTABLE);
        } catch (InvalidRuleException e) {
            return exceptionFormatter.customException(e, HttpStatus.BAD_REQUEST);
        } catch (Exception ee) {
            return exceptionFormatter.defaultException(ee);
        }
    }
}
