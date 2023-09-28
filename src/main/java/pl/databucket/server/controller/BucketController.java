package pl.databucket.server.controller;

import java.util.List;
import java.util.Map;
import javax.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pl.databucket.server.dto.BucketDto;
import pl.databucket.server.entity.Bucket;
import pl.databucket.server.exception.ExceptionFormatter;
import pl.databucket.server.exception.ItemAlreadyExistsException;
import pl.databucket.server.exception.ItemNotFoundException;
import pl.databucket.server.exception.ModifyByNullEntityIdException;
import pl.databucket.server.service.BucketService;

@PreAuthorize("hasRole('ADMIN')")
@RequestMapping("/api/buckets")
@RestController
@RequiredArgsConstructor
public class BucketController {

    private final BucketService bucketService;
    private final ModelMapper modelMapper;

    private final ExceptionFormatter exceptionFormatter = new ExceptionFormatter(BucketController.class);

    @PostMapping
    public ResponseEntity<BucketDto> createBucket(@Valid @RequestBody BucketDto bucketDto)
        throws ItemAlreadyExistsException, ItemNotFoundException {
        Bucket bucket = bucketService.createBucket(bucketDto);
        modelMapper.map(bucket, bucketDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(bucketDto);
    }

    @GetMapping
    public ResponseEntity<List<BucketDto>> getBuckets() {
        List<Bucket> buckets = bucketService.getBuckets();
        List<BucketDto> bucketsDto = buckets.stream().map(item -> modelMapper.map(item, BucketDto.class))
            .toList();
        return ResponseEntity.ok(bucketsDto);
    }

    @PutMapping
    public ResponseEntity<BucketDto> modifyBucket(@Valid @RequestBody BucketDto bucketDto)
        throws ModifyByNullEntityIdException, ItemAlreadyExistsException, ItemNotFoundException {
        Bucket bucket = bucketService.modifyBucket(bucketDto);
        modelMapper.map(bucket, bucketDto);
        return ResponseEntity.ok(bucketDto);
    }

    @DeleteMapping(value = "/{bucketId}")
    public ResponseEntity<Void> deleteBucket(@PathVariable long bucketId) throws ItemNotFoundException {
        bucketService.deleteBucket(bucketId);
        return ResponseEntity.noContent().build();
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleError(Exception ex) {
        return exceptionFormatter.defaultException(ex);
    }
}
