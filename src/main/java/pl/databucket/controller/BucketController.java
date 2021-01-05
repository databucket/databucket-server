package pl.databucket.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pl.databucket.dto.BucketDto;
import pl.databucket.exception.*;
import pl.databucket.service.BucketService;
import pl.databucket.specification.BucketSpecification;

import javax.validation.Valid;

@CrossOrigin(origins = "*", allowedHeaders = "*")
@RequestMapping("/api/bucket")
@RestController
public class BucketController {

    @Autowired
    private BucketService bucketService;

    private final ExceptionFormatter customExceptionFormatter = new ExceptionFormatter(BucketController.class);

    @PostMapping
    public ResponseEntity<?> createBucket(@Valid @RequestBody BucketDto bucketDto) {
        try {
            return new ResponseEntity<>(bucketService.createBucket(bucketDto), HttpStatus.CREATED);
        } catch (BucketAlreadyExistsException e) {
            return customExceptionFormatter.customException(e, HttpStatus.NOT_ACCEPTABLE);
        } catch (Exception ee) {
            return customExceptionFormatter.defaultException(ee);
        }
    }

    @GetMapping
    public ResponseEntity<?> getBuckets(BucketSpecification specification, Pageable pageable) {
        try {
            return new ResponseEntity<>(bucketService.getBuckets(specification, pageable), HttpStatus.OK);
        } catch (Exception ee) {
            return customExceptionFormatter.defaultException(ee);
        }
    }

    @PutMapping
    public ResponseEntity<?> modifyBucket(@Valid @RequestBody BucketDto bucketDto) {
        try {
            return new ResponseEntity<>(bucketService.modifyBucket(bucketDto), HttpStatus.OK);
        } catch (ItemDoNotExistsException e1) {
            return customExceptionFormatter.customException(e1, HttpStatus.NOT_FOUND);
        } catch (BucketAlreadyExistsException | ExceededMaximumNumberOfCharactersException | IncorrectValueException e2) {
            return customExceptionFormatter.customException(e2, HttpStatus.NOT_ACCEPTABLE);
        } catch (Exception ee) {
            return customExceptionFormatter.defaultException(ee);
        }
    }

    @DeleteMapping(value = "/{bucketId}")
    public ResponseEntity<?> deleteBucket(@PathVariable long bucketId) {
        try {
            bucketService.deleteBucket(bucketId);
            return new ResponseEntity<>(null, HttpStatus.OK);
        } catch (ItemAlreadyUsedException e1) {
            return customExceptionFormatter.customException(e1, HttpStatus.NOT_ACCEPTABLE);
        } catch (ItemDoNotExistsException e2) {
            return customExceptionFormatter.customException(e2, HttpStatus.NOT_FOUND);
        } catch (Exception ee) {
            return customExceptionFormatter.defaultException(ee);
        }
    }
}
