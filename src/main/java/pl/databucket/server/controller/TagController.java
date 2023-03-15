package pl.databucket.server.controller;

import java.util.List;
import javax.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pl.databucket.server.dto.TagDto;
import pl.databucket.server.entity.Tag;
import pl.databucket.server.exception.ExceptionFormatter;
import pl.databucket.server.exception.ItemAlreadyExistsException;
import pl.databucket.server.exception.ItemNotFoundException;
import pl.databucket.server.exception.ModifyByNullEntityIdException;
import pl.databucket.server.service.TagService;

@CrossOrigin(origins = "*", allowedHeaders = "*")
@RequestMapping("/api/tags")
@RestController
@RequiredArgsConstructor
public class TagController {

    private final ExceptionFormatter exceptionFormatter = new ExceptionFormatter(TagController.class);

    private final TagService tagService;
    private final ModelMapper modelMapper;


    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ResponseEntity<?> createTag(@Valid @RequestBody TagDto tagDto) {
        try {
            Tag tag = tagService.createTag(tagDto);
            modelMapper.map(tag, tagDto);
            return ResponseEntity.status(HttpStatus.CREATED).body(tagDto);
        } catch (ItemAlreadyExistsException e) {
            return exceptionFormatter.customException(e, HttpStatus.NOT_ACCEPTABLE);
        } catch (Exception e) {
            return exceptionFormatter.defaultException(e);
        }
    }

    @GetMapping
    public ResponseEntity<?> getTags() {
        try {
            List<Tag> tags = tagService.getTags();
            List<TagDto> tagsDto = tags.stream().map(item -> modelMapper.map(item, TagDto.class)).toList();
            return ResponseEntity.ok(tagsDto);
        } catch (Exception ee) {
            return exceptionFormatter.defaultException(ee);
        }
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping
    public ResponseEntity<?> modifyTag(@Valid @RequestBody TagDto tagDto) {
        try {
            Tag tag = tagService.modifyTag(tagDto);
            modelMapper.map(tag, tagDto);
            return ResponseEntity.ok(tagDto);
        } catch (ItemAlreadyExistsException e) {
            return exceptionFormatter.customException(e, HttpStatus.NOT_ACCEPTABLE);
        } catch (ItemNotFoundException | ModifyByNullEntityIdException e) {
            return exceptionFormatter.customException(e, HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            return exceptionFormatter.defaultException(e);
        }
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping(value = "/{tagId}")
    public ResponseEntity<?> deleteTags(@PathVariable long tagId) {
        try {
            tagService.deleteTag(tagId);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            return exceptionFormatter.defaultException(e);
        }
    }
}
