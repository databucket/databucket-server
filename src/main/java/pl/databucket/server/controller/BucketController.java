package pl.databucket.server.controller;

import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import pl.databucket.server.dto.BucketDto;
import pl.databucket.server.entity.Bucket;
import pl.databucket.server.exception.ExceptionFormatter;
import pl.databucket.server.exception.ItemAlreadyExistsException;
import pl.databucket.server.exception.ItemNotFoundException;
import pl.databucket.server.exception.ModifyByNullEntityIdException;
import pl.databucket.server.service.BucketService;

import javax.validation.Valid;
import java.util.List;
import java.util.stream.Collectors;

@CrossOrigin(origins = "*", allowedHeaders = "*")
@PreAuthorize("hasRole('ADMIN')")
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
    public ResponseEntity<?> getBuckets() {
        try {
            List<Bucket> buckets = bucketService.getBuckets();
            List<BucketDto> bucketsDto = buckets.stream().map(item -> modelMapper.map(item, BucketDto.class)).collect(Collectors.toList());
            return new ResponseEntity<>(bucketsDto, HttpStatus.OK);
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
            return new ResponseEntity<>(null, HttpStatus.NO_CONTENT);
        } catch (ItemNotFoundException e) {
            return exceptionFormatter.customException(e, HttpStatus.NOT_FOUND);
        } catch (Exception ee) {
            return exceptionFormatter.defaultException(ee);
        }
    }
}
