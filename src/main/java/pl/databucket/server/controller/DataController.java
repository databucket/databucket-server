package pl.databucket.server.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import java.io.IOException;
import java.io.InvalidObjectException;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pl.databucket.server.dto.DataCreateDTO;
import pl.databucket.server.dto.DataDTO;
import pl.databucket.server.dto.DataGetDTO;
import pl.databucket.server.dto.DataModifyDTO;
import pl.databucket.server.dto.DataRemoveDTO;
import pl.databucket.server.dto.DataReserveDTO;
import pl.databucket.server.entity.Bucket;
import pl.databucket.server.entity.User;
import pl.databucket.server.exception.BucketNotFoundException;
import pl.databucket.server.exception.ConditionNotAllowedException;
import pl.databucket.server.exception.ExceptionFormatter;
import pl.databucket.server.exception.NoAccessToBucketException;
import pl.databucket.server.exception.UnexpectedException;
import pl.databucket.server.exception.UnknownColumnException;
import pl.databucket.server.exception.ValidationException;
import pl.databucket.server.response.GetDataResponse;
import pl.databucket.server.response.MessageResponse;
import pl.databucket.server.response.ReserveDataResponse;
import pl.databucket.server.service.BucketService;
import pl.databucket.server.service.UserService;
import pl.databucket.server.service.data.COL;
import pl.databucket.server.service.data.Condition;
import pl.databucket.server.service.data.DataService;
import pl.databucket.server.service.data.Operator;
import pl.databucket.server.service.data.QueryRule;
import pl.databucket.server.service.data.ResultField;

@io.swagger.v3.oas.annotations.tags.Tag(name = "SECURED")
@CrossOrigin(origins = "*", allowedHeaders = "*")
@RequestMapping("/api/bucket/{bucketName}")
@RestController
@RequiredArgsConstructor
public class DataController {

    private final ExceptionFormatter exceptionFormatter = new ExceptionFormatter(DataController.class);

    private final DataService dataService;
    private final BucketService bucketService;
    private final UserService userService;

    @PreAuthorize("hasAnyRole('MEMBER', 'ROBOT')")
    @Operation(summary = "Insert data", description = "Inserts one data set into the selected bucket.",
        responses = {
            @ApiResponse(responseCode = "201", description = "Created", useReturnTypeSchema = true),
            @ApiResponse(responseCode = "404", description = "Not found - the user doesn't have access to the bucket or the bucket was not found"),
            @ApiResponse(responseCode = "406", description = "Not acceptable - used not existing tagId"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
        }
    )
    @PostMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<DataDTO> insertData(
        @Parameter(name = "bucket name", required = true) @PathVariable String bucketName,
        @Parameter(name = "payload - data", required = true) @RequestBody DataCreateDTO dataCreateDTO)
        throws BucketNotFoundException, NoAccessToBucketException, UnknownColumnException, ConditionNotAllowedException, SQLException, JsonProcessingException {

        Bucket bucket = bucketService.getBucket(bucketName);
        if (bucket == null) {
            throw new BucketNotFoundException(bucketName);
        }

        User user = userService.getCurrentUser();
        if (bucketService.hasUserAccessToBucket(bucket, user)) {
            DataDTO dataDTO = dataService.createData(user, bucket, dataCreateDTO);
            return new ResponseEntity<>(dataDTO, HttpStatus.CREATED);
        } else {
            throw new NoAccessToBucketException(bucketName);
        }
    }

    @PreAuthorize("hasAnyRole('MEMBER', 'ROBOT')")
    @Operation(summary = "Insert multi data", description = "Inserts multiple data sets into the selected bucket.",
        responses = {
            @ApiResponse(responseCode = "201", description = "Created"),
            @ApiResponse(responseCode = "404", description = "Not found - the user doesn't have access to the bucket or the bucket was not found"),
            @ApiResponse(responseCode = "406", description = "Not acceptable - used not existing tagId"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
        })
    @PostMapping(value = {"/multi"}, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> insertMultiData(
        @Parameter(name = "bucket name", required = true) @PathVariable String bucketName,
        @Parameter(name = "payload - a list of data", required = true) @RequestBody List<DataCreateDTO> dataList)
        throws BucketNotFoundException, NoAccessToBucketException {

        Bucket bucket = bucketService.getBucket(bucketName);
        if (bucket == null) {
            throw new BucketNotFoundException(bucketName);
        }
        User user = userService.getCurrentUser();
        if (bucketService.hasUserAccessToBucket(bucket, user)) {
            dataService.createData(user, bucket, dataList);
            return new ResponseEntity<>(HttpStatus.CREATED);
        } else {
            throw new NoAccessToBucketException(bucketName);
        }
    }


    @PreAuthorize("hasAnyRole('MEMBER', 'ROBOT')")
    @Operation(summary = "Modify data by data ids", description = "Makes changes on data based on data ids.",
        responses = {
            @ApiResponse(responseCode = "200", description = "OK", useReturnTypeSchema = true),
            @ApiResponse(responseCode = "404", description = "Not found - the user doesn't have access to the bucket or the bucket was not found"),
            @ApiResponse(responseCode = "406", description = "Not acceptable - used not exising tagId"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
        })
    @PutMapping(value = {"/{ids}"}, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<MessageResponse> modifyData(
        @Parameter(name = "bucket name", required = true) @PathVariable String bucketName,
        @Parameter(name = "ids", example = "1,2,3", required = true) @PathVariable Optional<List<Long>> ids,
        @Parameter(name = "payload - data details (rules are ignored in this endpoint)", required = true) @RequestBody DataModifyDTO dataModifyDTO)
        throws BucketNotFoundException, IOException, NoAccessToBucketException, UnknownColumnException, ConditionNotAllowedException, SQLException {

        Bucket bucket = bucketService.getBucket(bucketName);
        if (bucket == null) {
            throw new BucketNotFoundException(bucketName);
        }
        User user = userService.getCurrentUser();
        if (bucketService.hasUserAccessToBucket(bucket, user)) {
            int count = dataService.modifyData(user, bucket, ids, dataModifyDTO,
                new QueryRule(user.getUsername(), dataModifyDTO));
            return new ResponseEntity<>(new MessageResponse("Modified " + count + " data row(s)"), HttpStatus.OK);
        } else {
            throw new NoAccessToBucketException(bucketName);
        }
    }

    @PreAuthorize("hasAnyRole('MEMBER', 'ROBOT')")
    @Operation(summary = "Modify data by rules", description = "Makes changes on data based on data rules.",
        responses = {
            @ApiResponse(responseCode = "200", description = "OK", useReturnTypeSchema = true),
            @ApiResponse(responseCode = "404", description = "Not found - the user doesn't have access to the bucket or the bucket was not found"),
            @ApiResponse(responseCode = "406", description = "Not acceptable - used not existing tagId or rules contain not exising property"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
        })
    @PutMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<MessageResponse> modifyData(
        @Parameter(name = "bucket name", required = true) @PathVariable String bucketName,
        @Parameter(name = "payload - data details and rules", required = true) @RequestBody DataModifyDTO dataModifyDTO)
        throws BucketNotFoundException, IOException, NoAccessToBucketException, UnknownColumnException, ConditionNotAllowedException, SQLException {

        if ((dataModifyDTO.getConditions() == null || dataModifyDTO.getConditions().size() == 0)
            && (dataModifyDTO.getRules() == null || dataModifyDTO.getRules().size() == 0)
            && dataModifyDTO.getLogic() == null) {
            return new ResponseEntity<>(new MessageResponse("Can not modify data without any rules!"),
                HttpStatus.NOT_ACCEPTABLE);
        }
        Bucket bucket = bucketService.getBucket(bucketName);
        if (bucket == null) {
            throw new BucketNotFoundException(bucketName);
        }

        User user = userService.getCurrentUser();
        if (bucketService.hasUserAccessToBucket(bucket, user)) {
            int count = dataService.modifyData(user, bucket, Optional.empty(), dataModifyDTO,
                new QueryRule(user.getUsername(), dataModifyDTO));
            return new ResponseEntity<>(new MessageResponse("Modified " + count + " data row(s)"), HttpStatus.OK);
        } else {
            throw new NoAccessToBucketException(bucketName);
        }
    }


    @Operation(summary = "Get data by data ids", description = "Retrieves data based on data ids.",
        responses = {
            @ApiResponse(responseCode = "200", description = "OK", useReturnTypeSchema = true),
            @ApiResponse(responseCode = "404", description = "Not found - the user doesn't have access to the bucket or the bucket was not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
        })
    @PreAuthorize("hasAnyRole('MEMBER', 'ROBOT')")
    @GetMapping(value = {"/{ids}"}, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<DataDTO>> getData(
        @Parameter(name = "bucket name", required = true) @PathVariable String bucketName,
        @Parameter(name = "ids", example = "1,2,3", required = true) @PathVariable List<Long> ids)
        throws BucketNotFoundException, NoAccessToBucketException, UnknownColumnException, ConditionNotAllowedException {

        Bucket bucket = bucketService.getBucket(bucketName);
        if (bucket == null) {
            throw new BucketNotFoundException(bucketName);
        }

        User user = userService.getCurrentUser();
        if (bucketService.hasUserAccessToBucket(bucket, user)) {
            List<DataDTO> dataDtoList = dataService.getData(user, bucket, ids);
            return new ResponseEntity<>(dataDtoList, HttpStatus.OK);
        } else {
            throw new NoAccessToBucketException(bucketName);
        }
    }


    @Operation(summary = "Remove data by data ids", description = "Removes data based on data ids.",
        responses = {
            @ApiResponse(responseCode = "200", description = "OK", useReturnTypeSchema = true),
            @ApiResponse(responseCode = "404", description = "Not found - the user doesn't have access to the bucket or the bucket was not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
        })
    @PreAuthorize("hasAnyRole('MEMBER', 'ROBOT')")
    @DeleteMapping(value = {"/{ids}"}, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<MessageResponse> deleteData(
        @Parameter(name = "bucket name", required = true) @PathVariable String bucketName,
        @Parameter(name = "ids", example = "1,2,3", required = true) @PathVariable List<Long> ids)
        throws BucketNotFoundException, UnknownColumnException, ConditionNotAllowedException, NoAccessToBucketException {

        Bucket bucket = bucketService.getBucket(bucketName);
        if (bucket == null) {
            throw new BucketNotFoundException(bucketName);
        }

        User user = userService.getCurrentUser();
        if (bucketService.hasUserAccessToBucket(bucket, user)) {
            int count = dataService.deleteDataByIds(user, bucket, ids);
            return new ResponseEntity<>(new MessageResponse("Removed " + count + " data row(s)"), HttpStatus.OK);
        } else {
            throw new NoAccessToBucketException(bucketName);
        }
    }

    @PreAuthorize("hasAnyRole('MEMBER', 'ROBOT')")
    @Operation(summary = "Get data by rules", description = "Retrieves data based on data rules. Define 'columns' to limit the retrieved data sets. Search for 'data' if columns are not defined and 'customData' if columns are defined.",
        responses = {
            @ApiResponse(responseCode = "200", description = "OK", useReturnTypeSchema = true),
            @ApiResponse(responseCode = "404", description = "Not found - the user doesn't have access to the bucket or the bucket was not found"),
            @ApiResponse(responseCode = "406", description = "Not acceptable - rules contain not exising property"),
            @ApiResponse(responseCode = "500", description = "Internal server error"),
        })
    @PostMapping(value = "/get", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<GetDataResponse> getData(
        @Parameter(name = "bucket name", required = true) @PathVariable String bucketName,
        @Parameter(name = "page", example = "1") @RequestParam(required = false, defaultValue = "1") Integer page,
        @Parameter(name = "limit", example = "1") @RequestParam(required = false, defaultValue = "1") Integer limit,
        @Parameter(name = "sort", example = "id") @RequestParam(required = false, defaultValue = "id") String sort,
        @Parameter(name = "payload - rules (required), columns (optional)", required = true) @RequestBody DataGetDTO dataGetDTO)
        throws BucketNotFoundException, InvalidObjectException, UnknownColumnException, ConditionNotAllowedException, NoAccessToBucketException {

        Bucket bucket = bucketService.getBucket(bucketName);
        if (bucket == null) {
            throw new BucketNotFoundException(bucketName);
        }

        GetDataResponse response = new GetDataResponse();
        response.setLimit(limit);
        if (limit > 0) {
            response.setPage(page);
            response.setSort(sort);
        }

        User user = userService.getCurrentUser();
        if (bucketService.hasUserAccessToBucket(bucket, user)) {
            Map<ResultField, Object> result = dataService.getData(user, bucket,
                Optional.ofNullable(dataGetDTO.getColumns()), new QueryRule(user.getUsername(), dataGetDTO), page,
                limit, sort);

            long total = (long) result.get(ResultField.TOTAL);
            response.setTotal(total);
            if (limit > 0) {
                response.setTotalPages((int) Math.ceil(total / (float) limit));
            }
            if (result.containsKey(ResultField.DATA)) {
                response.setData((List<DataDTO>) result.get(ResultField.DATA));
            } else {
                response.setCustomData(result.get(ResultField.CUSTOM_DATA));
            }

            if (response.getData() == null && response.getCustomData() == null && limit > 0) {
                response.setMessage("No data matches the rules!");
            }

            return new ResponseEntity<>(response, HttpStatus.OK);
        } else {
            throw new NoAccessToBucketException(bucketName);
        }
    }

    @PreAuthorize("hasAnyRole('MEMBER', 'ROBOT')")
    @Operation(summary = "Reserve data by rules", description = "Reserves data based on data rules.",
        responses = {
            @ApiResponse(responseCode = "200", description = "OK", useReturnTypeSchema = true),
            @ApiResponse(responseCode = "404", description = "Not found - the user doesn't have access to the bucket or the bucket was not found"),
            @ApiResponse(responseCode = "406", description = "Not acceptable - rules contain not exising property"),
            @ApiResponse(responseCode = "500", description = "Internal server error"),
        })
    @PostMapping(value = {"/reserve"}, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ReserveDataResponse> reserveData(
        @Parameter(name = "bucket name", example = "bucket", required = true) @PathVariable String bucketName,
        @Parameter(name = "limit", example = "1") @RequestParam(required = false, defaultValue = "1") Integer limit,
        @Parameter(name = "sort", example = "random") @RequestParam(required = false, defaultValue = "id") String sort,
        @Parameter(name = "payload - rules (required)", required = true) @RequestBody DataReserveDTO dataReserveDTO)
        throws BucketNotFoundException, NoAccessToBucketException, UnknownColumnException, ConditionNotAllowedException, UnexpectedException, InvalidObjectException {

        Bucket bucket = bucketService.getBucket(bucketName);
        if (bucket == null) {
            throw new BucketNotFoundException(bucketName);
        }

        if (limit < 1) {
            throw new ValidationException("The limit must be greater than 0!");
        }

        ReserveDataResponse response = new ReserveDataResponse();
        response.setLimit(limit);
        response.setSort(sort);

        User user = userService.getCurrentUser();
        if (bucketService.hasUserAccessToBucket(bucket, user)) {
            String targetOwnerUsername = user.getUsername();
            if (user.isAdminUser() && dataReserveDTO.getTargetOwnerUsername() != null) {
                targetOwnerUsername = dataReserveDTO.getTargetOwnerUsername();
            }

            QueryRule queryRule = new QueryRule(user.getUsername(), dataReserveDTO);
            List<Long> dataIds = dataService.reserveData(user, bucket, queryRule, limit, sort, targetOwnerUsername);
            if (dataIds != null && !dataIds.isEmpty()) {
                response.setReserved(dataIds.size());
            } else {
                response.setReserved(0);
            }

            queryRule.getConditions().add(new Condition(COL.RESERVED, Operator.notEqual, true));
            Map<ResultField, Object> getDataResult = dataService.getData(user, bucket, Optional.empty(), queryRule,
                0, 0, "id");
            response.setAvailable((Long) getDataResult.get(ResultField.TOTAL));

            if (dataIds != null && !dataIds.isEmpty()) {
                List<DataDTO> dataDTOList = dataService.getData(user, bucket, dataIds);
                response.setData(dataDTOList);
            }

            return new ResponseEntity<>(response, HttpStatus.OK);
        } else {
            throw new NoAccessToBucketException(bucketName);
        }
    }


    @PreAuthorize("hasAnyRole('MEMBER', 'ROBOT')")
    @Operation(summary = "Remove data by rules", description = "Removes data based on data rules.",
        responses = {
            @ApiResponse(responseCode = "200", description = "OK", useReturnTypeSchema = true),
            @ApiResponse(responseCode = "404", description = "Not found - the user doesn't have access to the bucket or the bucket was not found"),
            @ApiResponse(responseCode = "406", description = "Not acceptable - rules contain not exising property"),
            @ApiResponse(responseCode = "500", description = "Internal server error"),
        })
    @DeleteMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<MessageResponse> deleteData(
        @Parameter(name = "bucket name", required = true) @PathVariable String bucketName,
        @Parameter(name = "payload - rules (required)", required = true) @RequestBody DataRemoveDTO dataRemoveDTO)
        throws BucketNotFoundException, InvalidObjectException, NoAccessToBucketException, UnknownColumnException, ConditionNotAllowedException {

        if ((dataRemoveDTO.getConditions() == null || dataRemoveDTO.getConditions().size() == 0)
            && (dataRemoveDTO.getRules() == null || dataRemoveDTO.getRules().size() == 0)
            && dataRemoveDTO.getLogic() == null) {
            return new ResponseEntity<>(new MessageResponse("Can not remove data without any rules!"),
                HttpStatus.NOT_ACCEPTABLE);
        }

        Bucket bucket = bucketService.getBucket(bucketName);
        if (bucket == null) {
            throw new BucketNotFoundException(bucketName);
        }

        User user = userService.getCurrentUser();
        if (bucketService.hasUserAccessToBucket(bucket, user)) {
            int count = dataService.deleteDataByRules(user, bucket, new QueryRule(user.getUsername(), dataRemoveDTO));
            return new ResponseEntity<>(new MessageResponse("Removed " + count + " data row(s)"), HttpStatus.OK);
        } else {
            throw new NoAccessToBucketException(bucketName);
        }
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleError(Exception ex) {
        return exceptionFormatter.defaultException(ex);
    }

    @ExceptionHandler({NoAccessToBucketException.class, BucketNotFoundException.class})
    public ResponseEntity<Map<String, Object>> handleNotFoundError(Exception ex) {
        return exceptionFormatter.customException(ex, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, Object>> handleError(DataIntegrityViolationException e) {
        return exceptionFormatter.customException(e, HttpStatus.NOT_ACCEPTABLE);
    }

    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<Map<String, Object>> handleError(ValidationException e) {
        return exceptionFormatter.customException(e, HttpStatus.NOT_ACCEPTABLE);
    }

}
