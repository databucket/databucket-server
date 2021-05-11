package pl.databucket.controller;

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
import pl.databucket.service.data.Condition;
import pl.databucket.service.data.FieldValidator;
import pl.databucket.entity.Bucket;
import pl.databucket.entity.User;
import pl.databucket.exception.*;
import pl.databucket.response.DataResponse;
import pl.databucket.service.BucketService;
import pl.databucket.service.data.DataService;
import pl.databucket.service.UserService;
import pl.databucket.service.data.ResultField;

import java.util.*;

@CrossOrigin(origins = "*", allowedHeaders = "*")
@RequestMapping("/api/bucket/{bucketName}/data")
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
    @PostMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> createData(
            @PathVariable("bucketName") String bucketName,
            @RequestBody DataCreateDto dataCreateDto) {

        Bucket bucket = bucketService.getBucket(bucketName);
        if (bucket == null)
            return exceptionFormatter.customException(new BucketNotFoundException(bucketName), HttpStatus.NOT_FOUND);

        try {
            User user = userService.getCurrentUser();
            if (bucketService.hasUserAccessToBucket(bucket, user)) {
                DataDto dataDto = dataService.createData(user, bucket, dataCreateDto);
                return new ResponseEntity<>(dataDto, HttpStatus.CREATED);
            } else
                return exceptionFormatter.customException(new NoAccessToBucketException(bucketName), HttpStatus.NOT_FOUND);
        } catch (DataIntegrityViolationException e) {
            if (e.getMessage().contains("is not present in table \"tags\""))
                return exceptionFormatter.customException(new ItemNotFoundException(Tag.class, dataCreateDto.getTagId()), HttpStatus.NOT_ACCEPTABLE);
            else
                return exceptionFormatter.customException(e, HttpStatus.NOT_ACCEPTABLE);
        } catch (Exception e) {
            return exceptionFormatter.defaultException(e);
        }
    }


    @PreAuthorize("hasAnyRole('MEMBER', 'ROBOT')")
    @PutMapping(value = "/{ids}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> modifyData(
            @PathVariable String bucketName,
            @PathVariable Optional<List<Long>> ids,
            @RequestBody DataModifyDto dataModifyDto) {

        Bucket bucket = bucketService.getBucket(bucketName);
        if (bucket == null)
            return exceptionFormatter.customException(new BucketNotFoundException(bucketName), HttpStatus.NOT_FOUND);

        try {
            User user = userService.getCurrentUser();
            if (bucketService.hasUserAccessToBucket(bucket, user)) {
                int count = dataService.modifyData(user, bucket, ids, dataModifyDto);

                return new ResponseEntity<>(new MessageResponse("Number of modified data: " + count), HttpStatus.OK);
            } else
                return exceptionFormatter.customException(new NoAccessToBucketException(bucketName), HttpStatus.NOT_FOUND);
        } catch (DataIntegrityViolationException e) {
            if (e.getMessage().contains("is not present in table \"tags\""))
                return exceptionFormatter.customException(new ItemNotFoundException(Tag.class, dataModifyDto.getTagId()), HttpStatus.NOT_ACCEPTABLE);
            else
                return exceptionFormatter.customException(e, HttpStatus.NOT_ACCEPTABLE);
        } catch (Exception e) {
            return exceptionFormatter.defaultException(e);
        }
    }


    @PreAuthorize("hasAnyRole('MEMBER', 'ROBOT')")
    @GetMapping(value = {"/{ids}"}, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> getData(
            @PathVariable String bucketName,
            @PathVariable List<Long> ids) {

        Bucket bucket = bucketService.getBucket(bucketName);
        if (bucket == null)
            return exceptionFormatter.customException(new BucketNotFoundException(bucketName), HttpStatus.NOT_FOUND);

        try {
            User user = userService.getCurrentUser();
            if (bucketService.hasUserAccessToBucket(bucket, user)) {
                List<DataDto> dataDtoList = dataService.getData(user, bucket, ids);
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


    @PreAuthorize("hasAnyRole('MEMBER', 'ROBOT')")
    @DeleteMapping(value = {"/{ids}"}, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> deleteData(
            @PathVariable String bucketName,
            @PathVariable List<Long> ids) {

        Bucket bucket = bucketService.getBucket(bucketName);
        if (bucket == null)
            return exceptionFormatter.customException(new BucketNotFoundException(bucketName), HttpStatus.NOT_FOUND);

        try {
            User user = userService.getCurrentUser();
            if (bucketService.hasUserAccessToBucket(bucket, user)) {
                int count = dataService.deleteDataByIds(user, bucket, ids);
                return new ResponseEntity<>(new MessageResponse("Number of removed data: " + count), HttpStatus.OK);
            } else
                return exceptionFormatter.customException(new NoAccessToBucketException(bucketName), HttpStatus.NOT_FOUND);
        } catch (Exception ee) {
            return exceptionFormatter.defaultException(ee);
        }
    }


    @PreAuthorize("hasAnyRole('MEMBER', 'ROBOT')")
    @PostMapping(value = "/get", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> getData(
            @PathVariable String bucketName,
            @RequestParam(required = false, defaultValue = "1") Optional<Integer> page,
            @RequestParam(required = false, defaultValue = "1") Optional<Integer> limit,
            @RequestParam(required = false, defaultValue = "data_id") Optional<String> sort,
            @RequestBody(required = false) DataGetDto dataGetDto) {

        Bucket bucket = bucketService.getBucket(bucketName);
        if (bucket == null)
            return exceptionFormatter.customException(new BucketNotFoundException(bucketName), HttpStatus.NOT_FOUND);

        try {
            DataResponse response = new DataResponse();

            if (page.isPresent()) {
                FieldValidator.mustBeGraterThen0("page", page.get());
                response.setPage(page.get());
            }

            if (limit.isPresent()) {
                FieldValidator.mustBeGraterOrEqual0("limit", limit.get());
                response.setLimit(limit.get());
            }

            if (sort.isPresent()) {
                FieldValidator.validateSort(sort.get());
                response.setSort(sort.get());
            }

            User user = userService.getCurrentUser();
            if (bucketService.hasUserAccessToBucket(bucket, user)) {
                List<Condition> conditions = null;
                if (dataGetDto != null && dataGetDto.getConditions() != null)
                    conditions = FieldValidator.validateListOfConditions(dataGetDto.getConditions(), true);

//                List<Map<String, Object>> columns = null;
//                if (dataGetDto != null && dataGetDto.getColumns() != null)
//                    columns = FieldValidator.validateListOfColumns(dataGetDto.getColumns());

                Map<ResultField, Object> result = dataService.getData(user, bucket, Optional.ofNullable(dataGetDto.getColumns()), Optional.ofNullable(conditions), page, limit, sort);

                long total = (long) result.get(ResultField.TOTAL);
                response.setTotal(total);

                if (page.isPresent() && limit.isPresent()) {
                    response.setTotalPages((int) Math.ceil(total / (float) limit.get()));
                }

                response.setData(result.get(ResultField.DATA));

                if (response.getData() == null && limit.get() > 0)
                    return new ResponseEntity<>(new MessageResponse("No data meets the given requirements!"), HttpStatus.OK);
                else
                    return new ResponseEntity<>(response, HttpStatus.OK);
            } else
                return exceptionFormatter.customException(new NoAccessToBucketException(bucketName), HttpStatus.NOT_FOUND);
        } catch (Exception ee) {
            return exceptionFormatter.defaultException(ee);
        }
    }


    @PreAuthorize("hasAnyRole('MEMBER', 'ROBOT')")
    @PostMapping(value = {"/reserve"}, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> reserveData(
            @PathVariable("bucketName") String bucketName,
            @RequestParam(required = false, defaultValue = "1") Integer limit,
            @RequestParam(required = false, defaultValue = "data_id") Optional<String> sort,
            @RequestBody(required = false) DataReserveDto dataReserveDto) {

        Bucket bucket = bucketService.getBucket(bucketName);
        if (bucket == null)
            return exceptionFormatter.customException(new BucketNotFoundException(bucketName), HttpStatus.NOT_FOUND);

        try {
            User user = userService.getCurrentUser();
            if (bucketService.hasUserAccessToBucket(bucket, user)) {
                List<Condition> conditions;
                if (dataReserveDto != null && dataReserveDto.getConditions() != null)
                    conditions = FieldValidator.validateListOfConditions(dataReserveDto.getConditions(), false);
                else
                    conditions = new ArrayList<>();

                String targetOwnerUsername = user.getUsername();
                if (user.isAdminUser())
                    targetOwnerUsername = dataReserveDto.getTargetOwnerUsername();

                List<Long> dataIds = dataService.reserveData(user, bucket, conditions, Optional.of(limit), sort, targetOwnerUsername);
                if (dataIds != null && dataIds.size() == 1) {
                    DataDto dataDto = dataService.getData(user, bucket, dataIds.get(0));
                    return new ResponseEntity<>(dataDto, HttpStatus.OK);
                } else if (dataIds != null && dataIds.size() > 1) {
                    return new ResponseEntity<>(dataIds, HttpStatus.OK);
                } else
                    return new ResponseEntity<>(new MessageResponse("No data meets the given requirements!"), HttpStatus.OK);
            } else
                return exceptionFormatter.customException(new NoAccessToBucketException(bucketName), HttpStatus.NOT_FOUND);
        } catch (Exception ee) {
            return exceptionFormatter.defaultException(ee);
        }
    }


    @PreAuthorize("hasAnyRole('MEMBER', 'ROBOT')")
    @DeleteMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> deleteData(
            @PathVariable String bucketName,
            @RequestBody DataRemoveDto dataRemoveDto) {

        Bucket bucket = bucketService.getBucket(bucketName);
        if (bucket == null)
            return exceptionFormatter.customException(new BucketNotFoundException(bucketName), HttpStatus.NOT_FOUND);

        try {
            User user = userService.getCurrentUser();
            List<Condition> conditions = FieldValidator.validateListOfConditions(dataRemoveDto.getConditions(), false);
            if (bucketService.hasUserAccessToBucket(bucket, user)) {
                int count = dataService.deleteDataByRules(user, bucket, conditions);
                return new ResponseEntity<>(new MessageResponse("Number of removed data: " + count), HttpStatus.OK);
            } else
                return exceptionFormatter.customException(new NoAccessToBucketException(bucketName), HttpStatus.NOT_FOUND);
        } catch (Exception ee) {
            return exceptionFormatter.defaultException(ee);
        }
    }
}
