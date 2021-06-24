package pl.databucket.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import pl.databucket.entity.Bucket;
import pl.databucket.entity.User;
import pl.databucket.exception.BucketNotFoundException;
import pl.databucket.exception.ExceptionFormatter;
import pl.databucket.exception.NoAccessToBucketException;
import pl.databucket.service.BucketService;
import pl.databucket.service.UserService;
import pl.databucket.service.data.DataService;
import java.util.List;

@CrossOrigin(origins = "*", allowedHeaders = "*")
@PreAuthorize("hasAnyRole('MEMBER', 'ADMIN')")
@RequestMapping("/api")
@RestController
public class DataHistoryController {

    private final ExceptionFormatter exceptionFormatter;
    private final DataService service;

    @Autowired
    private BucketService bucketService;

    @Autowired
    private UserService userService;

    public DataHistoryController(DataService service) {
        this.service = service;
        this.exceptionFormatter = new ExceptionFormatter(DataHistoryController.class);
    }

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
                return new ResponseEntity<>(service.getDataHistory(bucket, id), HttpStatus.OK);
            } else
                return exceptionFormatter.customException(new NoAccessToBucketException(bucketName), HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            return exceptionFormatter.defaultException(e);
        }
    }

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
                return new ResponseEntity<>(service.getDataHistoryProperties(bucket, id, ids), HttpStatus.OK);
            } else
                return exceptionFormatter.customException(new NoAccessToBucketException(bucketName), HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            return exceptionFormatter.defaultException(e);
        }
    }
}
