package pl.databucket.server.controller;

import io.swagger.v3.oas.annotations.Hidden;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.unit.DataSize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import pl.databucket.server.entity.Bucket;
import pl.databucket.server.exception.BucketNotFoundException;
import pl.databucket.server.exception.ExceptionFormatter;
import pl.databucket.server.exception.ItemNotFoundException;
import pl.databucket.server.response.MessageResponse;
import pl.databucket.server.service.BucketAccessRequestService;
import pl.databucket.server.service.BucketService;
import pl.databucket.server.service.HistoryCleanupService;
import pl.databucket.server.service.OverviewService;

/**
 * The Overview page of the project.
 */
@Hidden
@RequestMapping("/api/overview")
@RestController
public class OverviewController {

    private final ExceptionFormatter exceptionFormatter = new ExceptionFormatter(OverviewController.class);

    private final OverviewService overviewService;
    private final BucketService bucketService;
    private final BucketAccessRequestService requestService;
    // absent when the cleanup is disabled (databucket.history-cleanup.enabled)
    private final ObjectProvider<HistoryCleanupService> historyCleanup;

    public OverviewController(OverviewService overviewService, BucketService bucketService,
                              BucketAccessRequestService requestService, ObjectProvider<HistoryCleanupService> historyCleanup) {
        this.overviewService = overviewService;
        this.bucketService = bucketService;
        this.requestService = requestService;
        this.historyCleanup = historyCleanup;
    }

    @PreAuthorize("hasAnyRole('MEMBER', 'ADMIN')")
    @GetMapping
    public ResponseEntity<?> getOverview() {
        try {
            return new ResponseEntity<>(overviewService.getOverview(), HttpStatus.OK);
        } catch (Exception e) {
            return exceptionFormatter.defaultException(e);
        }
    }

    // the whole history of a bucket: for the admins of the project, as other changes of the buckets
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/bucket/{bucketName}/history")
    public ResponseEntity<?> clearHistory(@PathVariable String bucketName) {
        Bucket bucket = bucketService.getBucket(bucketName);
        if (bucket == null)
            return exceptionFormatter.customException(new BucketNotFoundException(bucketName), HttpStatus.NOT_FOUND);
        try {
            long freed = overviewService.clearHistory(bucket);
            return new ResponseEntity<>(new MessageResponse("History cleared, " + toText(freed) + " freed"), HttpStatus.OK);
        } catch (Exception e) {
            return exceptionFormatter.defaultException(e);
        }
    }

    // the history limit of a bucket applied now instead of at night
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/bucket/{bucketName}/history/limit")
    public ResponseEntity<?> applyHistoryLimit(@PathVariable String bucketName) {
        Bucket bucket = bucketService.getBucket(bucketName);
        if (bucket == null)
            return exceptionFormatter.customException(new BucketNotFoundException(bucketName), HttpStatus.NOT_FOUND);
        if (bucket.getHistoryLimit() == null)
            return exceptionFormatter.customException("The bucket has no history limit", HttpStatus.CONFLICT);
        HistoryCleanupService cleanup = historyCleanup.getIfAvailable();
        if (cleanup == null)
            return exceptionFormatter.customException("The history cleanup is disabled", HttpStatus.CONFLICT);
        try {
            int removed = cleanup.clean(bucket.getId(), bucket.getName(), bucket.getHistoryLimit());
            if (removed < 0)
                return exceptionFormatter.customException("The history of the bucket is being cleaned right now", HttpStatus.CONFLICT);
            return new ResponseEntity<>(new MessageResponse("Removed " + removed + " older history entries"), HttpStatus.OK);
        } catch (Exception e) {
            return exceptionFormatter.defaultException(e);
        }
    }

    // a user without access to a bucket visible on the Overview page asks for it
    @PreAuthorize("hasAnyRole('MEMBER', 'ADMIN')")
    @PostMapping("/bucket/{bucketName}/access-request")
    public ResponseEntity<?> requestAccess(@PathVariable String bucketName) {
        try {
            requestService.request(bucketName);
            return new ResponseEntity<>(new MessageResponse("The admins of the project have been asked for access"), HttpStatus.OK);
        } catch (IllegalStateException e) {
            return exceptionFormatter.customException(e.getMessage(), HttpStatus.CONFLICT);
        } catch (Exception e) {
            return exceptionFormatter.defaultException(e);
        }
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/access-requests")
    public ResponseEntity<?> getAccessRequests() {
        try {
            return new ResponseEntity<>(requestService.getPending(), HttpStatus.OK);
        } catch (Exception e) {
            return exceptionFormatter.defaultException(e);
        }
    }

    // for the counter next to Overview in the side panel
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/access-requests/count")
    public ResponseEntity<?> countAccessRequests() {
        try {
            return new ResponseEntity<>(Map.of("count", requestService.countPending()), HttpStatus.OK);
        } catch (Exception e) {
            return exceptionFormatter.defaultException(e);
        }
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/access-requests/{id}/{decision:approve|reject}")
    public ResponseEntity<?> decideAccessRequest(@PathVariable long id, @PathVariable String decision) {
        try {
            if ("approve".equals(decision)) {
                requestService.approve(id);
                return new ResponseEntity<>(new MessageResponse("Access granted"), HttpStatus.OK);
            }
            requestService.reject(id);
            return new ResponseEntity<>(new MessageResponse("Request rejected"), HttpStatus.OK);
        } catch (ItemNotFoundException e) {
            return exceptionFormatter.customException(e, HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            return exceptionFormatter.defaultException(e);
        }
    }

    private static String toText(long bytes) {
        DataSize size = DataSize.ofBytes(bytes);
        return size.toMegabytes() > 0 ? size.toMegabytes() + " MB" : size.toKilobytes() + " kB";
    }
}
