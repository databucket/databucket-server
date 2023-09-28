package pl.databucket.server.controller;

import java.util.List;
import java.util.Map;
import javax.validation.Valid;
import lombok.RequiredArgsConstructor;
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
import pl.databucket.server.dto.TagDto;
import pl.databucket.server.exception.ExceptionFormatter;
import pl.databucket.server.exception.ItemAlreadyExistsException;
import pl.databucket.server.exception.ItemNotFoundException;
import pl.databucket.server.exception.ModifyByNullEntityIdException;
import pl.databucket.server.service.TagService;

@RequestMapping("/api/tags")
@RestController
@RequiredArgsConstructor
public class TagController {

    private final ExceptionFormatter exceptionFormatter = new ExceptionFormatter(TagController.class);

    private final TagService tagService;

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ResponseEntity<TagDto> createTag(@Valid @RequestBody TagDto tagDto) throws ItemAlreadyExistsException {
        return ResponseEntity.status(HttpStatus.CREATED).body(tagService.createTag(tagDto));
    }

    @GetMapping
    public ResponseEntity<List<TagDto>> getTags() {
        return ResponseEntity.ok(tagService.getTags());
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping
    public ResponseEntity<?> modifyTag(@Valid @RequestBody TagDto tagDto) throws ItemAlreadyExistsException {
        try {
            return ResponseEntity.ok(tagService.modifyTag(tagDto));
        } catch (ItemNotFoundException | ModifyByNullEntityIdException e) {
            return exceptionFormatter.customException(e, HttpStatus.NOT_FOUND);
        }
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping(value = "/{tagId}")
    public ResponseEntity<Void> deleteTags(@PathVariable long tagId) throws ItemNotFoundException {
        tagService.deleteTag(tagId);
        return ResponseEntity.noContent().build();
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleError(Exception ex) {
        return exceptionFormatter.defaultException(ex);
    }
}
