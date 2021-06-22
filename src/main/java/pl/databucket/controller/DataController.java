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
import pl.databucket.service.data.*;
import pl.databucket.entity.Bucket;
import pl.databucket.entity.User;
import pl.databucket.exception.*;
import pl.databucket.response.DataResponse;
import pl.databucket.service.BucketService;
import pl.databucket.service.UserService;

import java.util.*;

@Api(tags="(secured)")
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
            @ApiParam(value="Bucket name", example = "bucket") @PathVariable String bucketName,
            @ApiParam(value="Payload") @RequestBody DataCreateDTO dataCreateDTO) {

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
    @ApiOperation(value = "Modify data")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "OK", response = String.class, examples=@Example(
                    value = @ExampleProperty(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            value = "{\n\t\"message\": \"Modified 3 data row(s)\"\n}"
                    )
            ))
    })
    @PutMapping(value = {"", "/{ids}"}, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> modifyData(
            @ApiParam(value="Bucket name", example = "bucket") @PathVariable String bucketName,
            @ApiParam(value="ids", example = "1,2,3") @PathVariable Optional<List<Long>> ids,
            @ApiParam(value="Payload") @RequestBody DataModifyDTO dataModifyDTO) {

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


    @ApiOperation(value = "Get data by ids")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "OK", response = DataDTO.class)
    })
    @PreAuthorize("hasAnyRole('MEMBER', 'ROBOT')")
    @GetMapping(value = {"/{ids}"}, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> getData(
            @ApiParam(value="Bucket name", example = "bucket") @PathVariable String bucketName,
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
            @ApiParam(value="Bucket name", example = "bucket") @PathVariable String bucketName,
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
            @ApiResponse(code = 200, message = "OK", response = DataDTO.class)
    })
    @PostMapping(value = "/get", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> getData(
            @ApiParam(value="Bucket name", example = "bucket") @PathVariable String bucketName,
            @ApiParam(hidden = true) @RequestParam(required = false, defaultValue = "1") Optional<Integer> page,
            @ApiParam(value="limit", example = "1") @RequestParam(required = false, defaultValue = "1") Optional<Integer> limit,
            @ApiParam(hidden = true) @RequestParam(required = false, defaultValue = "id") Optional<String> sort,
            @ApiParam(value="Payload") @RequestBody(required = false) DataGetDTO dataGetDTO) {

        Bucket bucket = bucketService.getBucket(bucketName);
        if (bucket == null)
            return exceptionFormatter.customException(new BucketNotFoundException(bucketName), HttpStatus.NOT_FOUND);

        try {
            DataResponse response = new DataResponse();

            if (page.isPresent() && limit.get() > 0)
                response.setPage(page.get());

            if (limit.isPresent())
                response.setLimit(limit.get());

            if (sort.isPresent() && limit.get() > 0)
                response.setSort(sort.get());

            User user = userService.getCurrentUser();
            if (bucketService.hasUserAccessToBucket(bucket, user)) {
                Map<ResultField, Object> result = dataService.getData(user, bucket, Optional.ofNullable(dataGetDTO.getColumns()), new QueryRule(user.getUsername(), dataGetDTO), page, limit, sort);

                long total = (long) result.get(ResultField.TOTAL);
                response.setTotal(total);

                if (page.isPresent() && limit.isPresent() && limit.get() > 0) {
                    response.setTotalPages((int) Math.ceil(total / (float) limit.get()));
                }

                if (limit.get() > 0)
                    response.setData(result.get(ResultField.DATA));

                if (response.getData() == null && limit.get() > 0)
                    return new ResponseEntity<>(new MessageResponse("No data matches the rules!"), HttpStatus.OK);
                else
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
            @ApiResponse(code = 200, message = "OK", response = DataDTO.class)
    })
    @PostMapping(value = {"/reserve"}, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> reserveData(
            @ApiParam(value="Bucket name", example = "bucket") @PathVariable String bucketName,
            @ApiParam(value="limit", example = "1") @RequestParam(required = false, defaultValue = "1") Integer limit,
            @ApiParam(value="sort", example = "random") @RequestParam(required = false, defaultValue = "id") Optional<String> sort,
            @ApiParam(value="Payload")  @RequestBody(required = false) DataReserveDTO dataReserveDTO) {

        Bucket bucket = bucketService.getBucket(bucketName);
        if (bucket == null)
            return exceptionFormatter.customException(new BucketNotFoundException(bucketName), HttpStatus.NOT_FOUND);

        try {
            User user = userService.getCurrentUser();
            if (bucketService.hasUserAccessToBucket(bucket, user)) {
                String targetOwnerUsername = user.getUsername();
                if (user.isAdminUser() && dataReserveDTO.getTargetOwnerUsername() != null)
                    targetOwnerUsername = dataReserveDTO.getTargetOwnerUsername();

                List<Long> dataIds = dataService.reserveData(user, bucket, new QueryRule(user.getUsername(), dataReserveDTO), Optional.of(limit), sort, targetOwnerUsername);
                if (dataIds != null && dataIds.size() == 1) {
                    DataDTO dataDto = dataService.getData(user, bucket, dataIds.get(0));
                    return new ResponseEntity<>(dataDto, HttpStatus.OK);
                } else if (dataIds != null && dataIds.size() > 1) {
                    return new ResponseEntity<>(dataIds, HttpStatus.OK);
                } else
                    return new ResponseEntity<>(new MessageResponse("No data matches the rules!"), HttpStatus.OK);
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
            @ApiParam(name = "bucketName", example = "bucket", required = true) @PathVariable String bucketName,
            @ApiParam(value="Payload") @RequestBody DataRemoveDTO dataRemoveDTO) {

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
