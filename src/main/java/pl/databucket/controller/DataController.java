package pl.databucket.controller;

import io.swagger.annotations.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import pl.databucket.dto.*;
import pl.databucket.entity.Tag;
import pl.databucket.response.MessageResponse;
import pl.databucket.response.ReserveDataResponse;
import pl.databucket.service.data.*;
import pl.databucket.entity.Bucket;
import pl.databucket.entity.User;
import pl.databucket.exception.*;
import pl.databucket.response.GetDataResponse;
import pl.databucket.service.BucketService;
import pl.databucket.service.UserService;

import java.util.*;

@Api(tags="SECURED")
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
    @ApiOperation(value = "Create data")
    @ApiResponses(value = {
            @ApiResponse(code = 201, message = "OK", response = DataDTO.class)
    })
    @PostMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> createData(
            @ApiParam(value="bucket name", example = "bucket") @PathVariable String bucketName,
            @ApiParam(value="payload") @RequestBody DataCreateDTO dataCreateDTO) {

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
                return exceptionFormatter.customException(new ItemNotFoundException(Tag.class, dataCreateDTO.getTagId()), HttpStatus.NOT_ACCEPTABLE);
            else
                return exceptionFormatter.customException(e, HttpStatus.NOT_ACCEPTABLE);
        } catch (Exception e) {
            return exceptionFormatter.defaultException(e);
        }
    }


    @PreAuthorize("hasAnyRole('MEMBER', 'ROBOT')")
    @ApiOperation(value = "Modify data by ids")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "OK", response = String.class, examples=@Example(
                    value = @ExampleProperty(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            value = "{\n\t\"message\": \"Modified 3 data row(s)\"\n}"
                    )
            ))
    })
    @PutMapping(value = { "/{ids}"}, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> modifyData(
            @ApiParam(value="bucket name", example = "bucket") @PathVariable String bucketName,
            @ApiParam(value="ids", example = "1,2,3") @PathVariable Optional<List<Long>> ids,
            @ApiParam(value="payload") @RequestBody DataModifyDTO dataModifyDTO) {

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
            if (e.getMessage().contains("is not present in table \"tags\""))
                return exceptionFormatter.customException(new ItemNotFoundException(Tag.class, dataModifyDTO.getTagId()), HttpStatus.NOT_ACCEPTABLE);
            else if (e.getMessage().contains("cannot cast jsonb null"))
                return exceptionFormatter.customException("Failed to operate on an empty property!", HttpStatus.NOT_ACCEPTABLE);
            else
                return exceptionFormatter.customException(e, HttpStatus.NOT_ACCEPTABLE);
        } catch (Exception e) {
            return exceptionFormatter.defaultException(e);
        }
    }

    @PreAuthorize("hasAnyRole('MEMBER', 'ROBOT')")
    @ApiOperation(value = "Modify data by conditions")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "OK", response = String.class, examples=@Example(
                    value = @ExampleProperty(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            value = "{\n\t\"message\": \"Modified 3 data row(s)\"\n}"
                    )
            ))
    })
    @PutMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> modifyData(
            @ApiParam(value="bucket name", example = "bucket") @PathVariable String bucketName,
            @ApiParam(value="payload") @RequestBody DataModifyDTO dataModifyDTO) {

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
            if (e.getMessage().contains("is not present in table \"tags\""))
                return exceptionFormatter.customException(new ItemNotFoundException(Tag.class, dataModifyDTO.getTagId()), HttpStatus.NOT_ACCEPTABLE);
            else if (e.getMessage().contains("cannot cast jsonb null"))
                return exceptionFormatter.customException("Failed to operate on an empty property!", HttpStatus.NOT_ACCEPTABLE);
            else
                return exceptionFormatter.customException(e, HttpStatus.NOT_ACCEPTABLE);
        } catch (Exception e) {
            return exceptionFormatter.defaultException(e);
        }
    }


    @ApiOperation(value = "Get data by ids")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "OK", response = DataDTO.class)
    })
    @PreAuthorize("hasAnyRole('MEMBER', 'ROBOT')")
    @GetMapping(value = {"/{ids}"}, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> getData(
            @ApiParam(value="bucket name", example = "bucket") @PathVariable String bucketName,
            @ApiParam(value="ids", example = "1,2,3") @PathVariable List<Long> ids) {

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


    @ApiOperation(value = "Remove data by ids")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "OK", response = String.class, examples=@Example(
                    value = @ExampleProperty(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            value = "{\n\t\"message\": \"Removed 3 data row(s)\"\n}"
                    )
            ))
    })
    @PreAuthorize("hasAnyRole('MEMBER', 'ROBOT')")
    @DeleteMapping(value = {"/{ids}"}, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> deleteData(
            @ApiParam(value="bucket name", example = "bucket") @PathVariable String bucketName,
            @ApiParam(value="ids", example = "1,2,3") @PathVariable List<Long> ids) {

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
    @ApiOperation(value = "Get data by conditions")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "OK", response = GetDataResponse.class)
    })
    @PostMapping(value = "/get", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> getData(
            @ApiParam(value="bucket name", example = "bucket") @PathVariable String bucketName,
            @ApiParam(value="page", example="1") @RequestParam(required = false, defaultValue = "1") Integer page,
            @ApiParam(value="limit", example = "1") @RequestParam(required = false, defaultValue = "1") Integer limit,
            @ApiParam(value="sort", example = "id") @RequestParam(required = false, defaultValue = "id") String sort,
            @ApiParam(value="payload") @RequestBody(required = false) DataGetDTO dataGetDTO) {

        Bucket bucket = bucketService.getBucket(bucketName);
        if (bucket == null)
            return exceptionFormatter.customException(new BucketNotFoundException(bucketName), HttpStatus.NOT_FOUND);

        try {
            GetDataResponse response = new GetDataResponse();
            response.setPage(page);
            response.setLimit(limit);
            response.setSort(sort);

            User user = userService.getCurrentUser();
            if (bucketService.hasUserAccessToBucket(bucket, user)) {
                Map<ResultField, Object> result = dataService.getData(user, bucket, Optional.ofNullable(dataGetDTO.getColumns()), new QueryRule(user.getUsername(), dataGetDTO), page, limit, sort);

                long total = (long) result.get(ResultField.TOTAL);
                response.setTotal(total);
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
            if (e.getMessage().contains("cannot cast jsonb null"))
                return exceptionFormatter.customException("Failed to operate on an empty property!", HttpStatus.NOT_ACCEPTABLE);
            else
                return exceptionFormatter.customException(e, HttpStatus.NOT_ACCEPTABLE);
        } catch (Exception ee) {
            return exceptionFormatter.defaultException(ee);
        }
    }

    @PreAuthorize("hasAnyRole('MEMBER', 'ROBOT')")
    @ApiOperation(value = "Reserve data by conditions")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "OK", response = ReserveDataResponse.class)
    })
    @PostMapping(value = {"/reserve"}, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> reserveData(
            @ApiParam(value="bucket name", example = "bucket") @PathVariable String bucketName,
            @ApiParam(value="limit", example = "1") @RequestParam(required = false, defaultValue = "1") Integer limit,
            @ApiParam(value="sort", example = "random") @RequestParam(required = false, defaultValue = "id") String sort,
            @ApiParam(value="payload")  @RequestBody(required = false) DataReserveDTO dataReserveDTO) {

        Bucket bucket = bucketService.getBucket(bucketName);
        if (bucket == null)
            return exceptionFormatter.customException(new BucketNotFoundException(bucketName), HttpStatus.NOT_FOUND);

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
            if (e.getMessage().contains("cannot cast jsonb null"))
                return exceptionFormatter.customException("Failed to operate on an empty property!", HttpStatus.NOT_ACCEPTABLE);
            else
                return exceptionFormatter.customException(e, HttpStatus.NOT_ACCEPTABLE);
        } catch (Exception ee) {
            return exceptionFormatter.defaultException(ee);
        }
    }


    @PreAuthorize("hasAnyRole('MEMBER', 'ROBOT')")
    @ApiOperation(value = "Remove data by conditions")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "OK", response = String.class, examples=@Example(
                    value = @ExampleProperty(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            value = "{\n\t\"message\": \"Removed 3 data row(s)\"\n}"
                    )
            ))
    })
    @DeleteMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> deleteData(
            @ApiParam(name = "bucket name", example = "bucket", required = true) @PathVariable String bucketName,
            @ApiParam(value="payload") @RequestBody DataRemoveDTO dataRemoveDTO) {

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
        } catch (Exception ee) {
            return exceptionFormatter.defaultException(ee);
        }
    }
}
