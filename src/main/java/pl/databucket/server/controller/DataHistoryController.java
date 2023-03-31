package pl.databucket.server.controller;

import io.swagger.annotations.ApiParam;
import java.io.InvalidObjectException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pl.databucket.server.dto.DataRemoveDTO;
import pl.databucket.server.entity.Bucket;
import pl.databucket.server.entity.User;
import pl.databucket.server.exception.BucketNotFoundException;
import pl.databucket.server.exception.ConditionNotAllowedException;
import pl.databucket.server.exception.ExceptionFormatter;
import pl.databucket.server.exception.NoAccessToBucketException;
import pl.databucket.server.exception.UnexpectedException;
import pl.databucket.server.exception.UnknownColumnException;
import pl.databucket.server.response.MessageResponse;
import pl.databucket.server.service.BucketService;
import pl.databucket.server.service.UserService;
import pl.databucket.server.service.data.DataService;
import pl.databucket.server.service.data.QueryRule;

@PreAuthorize("hasAnyRole('MEMBER', 'ADMIN')")
@RequestMapping("/api")
@RestController
@RequiredArgsConstructor
public class DataHistoryController {

    private final ExceptionFormatter exceptionFormatter = new ExceptionFormatter(DataHistoryController.class);
    private final DataService service;
    private final BucketService bucketService;
    private final UserService userService;
    private final DataService dataService;


    @GetMapping(value = {
        "/bucket/{bucketName}/{id}/history"},
        produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<Map<String, Object>>> getDataHistory(
        @PathVariable String bucketName,
        @PathVariable Long id)
        throws BucketNotFoundException, NoAccessToBucketException, UnknownColumnException, ConditionNotAllowedException {

        Bucket bucket = Optional.ofNullable(bucketService.getBucket(bucketName))
            .orElseThrow(() -> new BucketNotFoundException(bucketName));
        User user = userService.getCurrentUser();
        if (bucketService.hasUserAccessToBucket(bucket, user)) {
            return ResponseEntity.ok(service.getDataHistory(bucket, id));
        } else {
            throw new NoAccessToBucketException(bucketName);
        }
    }

    @GetMapping(value = {
        "/bucket/{bucketName}/{id}/history/{ids}"},
        produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<Map<String, Object>>> getDataHistoryProperties(
        @PathVariable String bucketName,
        @PathVariable Long id,
        @PathVariable List<Long> ids)
        throws BucketNotFoundException, UnknownColumnException, UnexpectedException, ConditionNotAllowedException, NoAccessToBucketException {

        Bucket bucket = Optional.ofNullable(bucketService.getBucket(bucketName))
            .orElseThrow(() -> new BucketNotFoundException(bucketName));

        User user = userService.getCurrentUser();
        if (bucketService.hasUserAccessToBucket(bucket, user)) {
            return ResponseEntity.ok(service.getDataHistoryProperties(bucket, id, ids));
        } else {
            throw new NoAccessToBucketException(bucketName);
        }
    }


    @DeleteMapping(value = {
        "/bucket/{bucketName}/{id}/history/clear"},
        produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<MessageResponse> clearHistory(
        @ApiParam(value = "bucket name", required = true) @PathVariable String bucketName,
        @ApiParam(value = "id", example = "1", required = true) @PathVariable Long id
    ) throws BucketNotFoundException, UnknownColumnException, ConditionNotAllowedException, NoAccessToBucketException {

        Bucket bucket = Optional.ofNullable(bucketService.getBucket(bucketName))
            .orElseThrow(() -> new BucketNotFoundException(bucketName));

        User user = userService.getCurrentUser();
        if (bucketService.hasUserAccessToBucket(bucket, user)) {
            int count = dataService.clearDataHistory(user, bucket, id);
            return ResponseEntity.ok(new MessageResponse("Removed " + count + " data history row(s)"));
        } else {
            throw new NoAccessToBucketException(bucketName);
        }
    }


    @DeleteMapping(value = {
        "/bucket/{bucketName}/history/clear"},
        produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<MessageResponse> clearHistory(
        @ApiParam(value = "bucket name", required = true) @PathVariable String bucketName,
        @ApiParam(value = "payload - rules", required = true) @RequestBody DataRemoveDTO dataRemoveDTO)
        throws BucketNotFoundException, InvalidObjectException, UnknownColumnException, ConditionNotAllowedException, NoAccessToBucketException {

        if ((dataRemoveDTO.getConditions() == null || dataRemoveDTO.getConditions().isEmpty())
            && (dataRemoveDTO.getRules() == null || dataRemoveDTO.getRules().isEmpty())
            && dataRemoveDTO.getLogic() == null) {
            return new ResponseEntity<>(new MessageResponse("Can not modify data history without any rules!"),
                HttpStatus.NOT_ACCEPTABLE);
        }

        Bucket bucket = Optional.ofNullable(bucketService.getBucket(bucketName))
            .orElseThrow(() -> new BucketNotFoundException(bucketName));
        User user = userService.getCurrentUser();
        if (bucketService.hasUserAccessToBucket(bucket, user)) {
            int count = dataService.clearDataHistory(user, bucket, new QueryRule(user.getUsername(), dataRemoveDTO));
            return ResponseEntity.ok(new MessageResponse("Removed history on " + count + " data rows."));
        } else {
            throw new NoAccessToBucketException(bucketName);
        }
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleError(Exception ex) {
        return exceptionFormatter.defaultException(ex);
    }
}
