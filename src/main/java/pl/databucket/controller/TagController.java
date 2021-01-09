package pl.databucket.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pl.databucket.dto.TagDto;
import pl.databucket.exception.ExceptionFormatter;
import pl.databucket.service.TagService;
import pl.databucket.specification.TagSpecification;

import javax.validation.Valid;

@CrossOrigin(origins = "*", allowedHeaders = "*")
@RequestMapping("/api/tags")
@RestController
public class TagController {

    private final ExceptionFormatter exceptionFormatter = new ExceptionFormatter(TagController.class);

    @Autowired
    private TagService tagService;


    @PostMapping
    public ResponseEntity<?> createTag(@Valid @RequestBody TagDto tagDto) {
        try {
            return new ResponseEntity<>(tagService.createTag(tagDto), HttpStatus.CREATED);
        } catch (Exception e) {
            return exceptionFormatter.defaultException(e);
        }
    }

    @GetMapping
    public ResponseEntity<?> getTags(TagSpecification specification, Pageable pageable) {
        try {
            return new ResponseEntity<>(tagService.getTags(specification, pageable), HttpStatus.OK);
        } catch (Exception ee) {
            return exceptionFormatter.defaultException(ee);
        }
    }

    @PutMapping
    public ResponseEntity<?> modifyTag(@Valid @RequestBody TagDto tagDto) {
        try {
            return new ResponseEntity<>(tagService.modifyTag(tagDto), HttpStatus.OK);
        } catch (Exception e) {
            return exceptionFormatter.defaultException(e);
        }
    }

    @DeleteMapping(value = "/{tagId}")
    public ResponseEntity<?> deleteTags(@PathVariable long tagId) {
        try {
            tagService.deleteTag(tagId);
            return new ResponseEntity<>(null, HttpStatus.OK);
        } catch (Exception e) {
            return exceptionFormatter.defaultException(e);
        }
    }
}
