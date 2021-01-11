package pl.databucket.controller;

import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pl.databucket.dto.BucketDto;
import pl.databucket.entity.Bucket;
import pl.databucket.exception.*;
import pl.databucket.response.BucketPageResponse;
import pl.databucket.service.BucketService;
import pl.databucket.specification.BucketSpecification;
import javax.validation.Valid;

@CrossOrigin(origins = "*", allowedHeaders = "*")
@RequestMapping("/api/buckets")
@RestController
public class BucketController {

    @Autowired
    private BucketService bucketService;

    @Autowired
    private ModelMapper modelMapper;

    private final ExceptionFormatter exceptionFormatter = new ExceptionFormatter(BucketController.class);

    @PostMapping
    public ResponseEntity<?> createBucket(@Valid @RequestBody BucketDto bucketDto) {
        try {
            Bucket bucket = bucketService.createBucket(bucketDto);
            modelMapper.map(bucket, bucketDto);
            return new ResponseEntity<>(bucketDto, HttpStatus.CREATED);
        } catch (ItemAlreadyExistsException e) {
            return exceptionFormatter.customException(e, HttpStatus.NOT_ACCEPTABLE);
        } catch (ItemNotFoundException e) {
            return exceptionFormatter.customException(e, HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            return exceptionFormatter.defaultException(e);
        }
    }

    @GetMapping
    public ResponseEntity<?> getBuckets(BucketSpecification specification, Pageable pageable) {
        try {
            Page<Bucket> bucketPage = bucketService.getBuckets(specification, pageable);
            return new ResponseEntity<>(new BucketPageResponse(bucketPage, modelMapper), HttpStatus.OK);
        } catch (Exception ee) {
            return exceptionFormatter.defaultException(ee);
        }
    }

    @PutMapping
    public ResponseEntity<?> modifyBucket(@Valid @RequestBody BucketDto bucketDto) {
        try {
            Bucket bucket = bucketService.modifyBucket(bucketDto);
            modelMapper.map(bucket, bucketDto);
            return new ResponseEntity<>(bucketDto, HttpStatus.OK);
        } catch (ItemNotFoundException | ModifyByNullEntityIdException e) {
            return exceptionFormatter.customException(e, HttpStatus.NOT_FOUND);
        } catch (ItemAlreadyExistsException e) {
            return exceptionFormatter.customException(e, HttpStatus.NOT_ACCEPTABLE);
        } catch (Exception e) {
            return exceptionFormatter.defaultException(e);
        }
    }

    @DeleteMapping(value = "/{bucketId}")
    public ResponseEntity<?> deleteBucket(@PathVariable long bucketId) {
        try {
            bucketService.deleteBucket(bucketId);
            return new ResponseEntity<>(null, HttpStatus.OK);
        } catch (ItemNotFoundException e) {
            return exceptionFormatter.customException(e, HttpStatus.NOT_FOUND);
        } catch (Exception ee) {
            return exceptionFormatter.defaultException(ee);
        }
    }
}
