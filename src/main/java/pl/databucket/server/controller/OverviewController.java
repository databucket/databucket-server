package pl.databucket.server.controller;

import io.swagger.v3.oas.annotations.Hidden;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.unit.DataSize;
import org.springframework.web.bind.annotation.*;
import pl.databucket.server.entity.Bucket;
import pl.databucket.server.exception.BucketNotFoundException;
import pl.databucket.server.exception.ExceptionFormatter;
import pl.databucket.server.response.MessageResponse;
import pl.databucket.server.service.BucketService;
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

    public OverviewController(OverviewService overviewService, BucketService bucketService) {
        this.overviewService = overviewService;
        this.bucketService = bucketService;
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

    private static String toText(long bytes) {
        DataSize size = DataSize.ofBytes(bytes);
        return size.toMegabytes() > 0 ? size.toMegabytes() + " MB" : size.toKilobytes() + " kB";
    }
}
