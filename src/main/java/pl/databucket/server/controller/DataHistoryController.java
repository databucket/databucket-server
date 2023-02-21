package pl.databucket.server.controller;

import io.swagger.v3.oas.annotations.Parameter;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pl.databucket.server.dto.DataRemoveDTO;
import pl.databucket.server.entity.Bucket;
import pl.databucket.server.entity.User;
import pl.databucket.server.exception.BucketNotFoundException;
import pl.databucket.server.exception.ExceptionFormatter;
import pl.databucket.server.exception.NoAccessToBucketException;
import pl.databucket.server.response.MessageResponse;
import pl.databucket.server.service.BucketService;
import pl.databucket.server.service.UserService;
import pl.databucket.server.service.data.DataService;
import pl.databucket.server.service.data.QueryRule;

@CrossOrigin(origins = "*", allowedHeaders = "*")
@PreAuthorize("hasAnyRole('MEMBER', 'ADMIN')")
@RequestMapping("/api")
@RestController
@RequiredArgsConstructor
public class DataHistoryController {

    private final ExceptionFormatter exceptionFormatter = new ExceptionFormatter(DataHistoryController.class);
    private final DataService dataService;
    private final BucketService bucketService;
    private final UserService userService;


    @GetMapping(value = {
        "/bucket/{bucketName}/{id}/history"},
        produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> getDataHistory(
        @PathVariable String bucketName,
        @PathVariable Long id) {

        Bucket bucket = bucketService.getBucket(bucketName);
        if (bucket == null) {
            return exceptionFormatter.customException(new BucketNotFoundException(bucketName), HttpStatus.NOT_FOUND);
        }

        try {
            User user = userService.getCurrentUser();
            if (bucketService.hasUserAccessToBucket(bucket, user)) {
                return new ResponseEntity<>(dataService.getDataHistory(bucket, id), HttpStatus.OK);
            } else {
                return exceptionFormatter.customException(new NoAccessToBucketException(bucketName),
                    HttpStatus.NOT_FOUND);
            }
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
        if (bucket == null) {
            return exceptionFormatter.customException(new BucketNotFoundException(bucketName), HttpStatus.NOT_FOUND);
        }

        try {
            User user = userService.getCurrentUser();
            if (bucketService.hasUserAccessToBucket(bucket, user)) {
                return new ResponseEntity<>(dataService.getDataHistoryProperties(bucket, id, ids), HttpStatus.OK);
            } else {
                return exceptionFormatter.customException(new NoAccessToBucketException(bucketName),
                    HttpStatus.NOT_FOUND);
            }
        } catch (Exception e) {
            return exceptionFormatter.defaultException(e);
        }
    }


    @DeleteMapping(value = {
        "/bucket/{bucketName}/{id}/history/clear"},
        produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> clearHistory(
        @Parameter(name = "bucket name", required = true) @PathVariable String bucketName,
        @Parameter(name = "id", example = "1", required = true) @PathVariable Long id
    ) {

        Bucket bucket = bucketService.getBucket(bucketName);
        if (bucket == null) {
            return exceptionFormatter.customException(new BucketNotFoundException(bucketName), HttpStatus.NOT_FOUND);
        }

        try {
            User user = userService.getCurrentUser();
            if (bucketService.hasUserAccessToBucket(bucket, user)) {
                int count = dataService.clearDataHistory(user, bucket, id);
                return new ResponseEntity<>(new MessageResponse("Removed " + count + " data history row(s)"),
                    HttpStatus.OK);
            } else {
                return exceptionFormatter.customException(new NoAccessToBucketException(bucketName),
                    HttpStatus.NOT_FOUND);
            }
        } catch (Exception e) {
            return exceptionFormatter.defaultException(e);
        }
    }


    @DeleteMapping(value = {
        "/bucket/{bucketName}/history/clear"},
        produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> clearHistory(
        @Parameter(name = "bucket name", required = true) @PathVariable String bucketName,
        @Parameter(name = "payload - rules", required = true) @RequestBody DataRemoveDTO dataRemoveDTO) {

        if ((dataRemoveDTO.getConditions() == null || dataRemoveDTO.getConditions().size() == 0)
            && (dataRemoveDTO.getRules() == null || dataRemoveDTO.getRules().size() == 0)
            && dataRemoveDTO.getLogic() == null) {
            return new ResponseEntity<>(new MessageResponse("Can not modify data history without any rules!"),
                HttpStatus.NOT_ACCEPTABLE);
        }

        Bucket bucket = bucketService.getBucket(bucketName);
        if (bucket == null) {
            return exceptionFormatter.customException(new BucketNotFoundException(bucketName), HttpStatus.NOT_FOUND);
        }
        try {
            User user = userService.getCurrentUser();
            if (bucketService.hasUserAccessToBucket(bucket, user)) {
                int count = dataService.clearDataHistory(user, bucket,
                    new QueryRule(user.getUsername(), dataRemoveDTO));
                return new ResponseEntity<>(new MessageResponse("Removed history on " + count + " data rows."),
                    HttpStatus.OK);
            } else {
                return exceptionFormatter.customException(new NoAccessToBucketException(bucketName),
                    HttpStatus.NOT_FOUND);
            }
        } catch (Exception e) {
            return exceptionFormatter.defaultException(e);
        }
    }

}
