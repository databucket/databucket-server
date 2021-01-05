package pl.databucket.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pl.databucket.dto.ViewDto;
import pl.databucket.exception.ExceptionFormatter;
import pl.databucket.service.ViewService;
import pl.databucket.specification.ViewSpecification;
import javax.validation.Valid;

@CrossOrigin(origins = "*", allowedHeaders = "*")
@RequestMapping("/api/view")
@RestController
public class ViewController {

  private final ExceptionFormatter exceptionFormatter = new ExceptionFormatter(ViewController.class);

  @Autowired
  private ViewService viewService;

  @PostMapping
  public ResponseEntity<?> createView(@Valid @RequestBody ViewDto viewDto) {
    try {
      return new ResponseEntity<>(viewService.createView(viewDto), HttpStatus.CREATED);
    } catch (Exception e) {
      return exceptionFormatter.defaultException(e);
    }
  }

  @GetMapping
  public ResponseEntity<?> getViews(ViewSpecification specification, Pageable pageable) {
    try {
      return new ResponseEntity<>(viewService.getViews(specification, pageable), HttpStatus.OK);
    } catch (Exception ee) {
      return exceptionFormatter.defaultException(ee);
    }
  }

  @PutMapping
  public ResponseEntity<?> modifyView(@Valid @RequestBody ViewDto viewDto) {
    try {
      return new ResponseEntity<>(viewService.modifyView(viewDto), HttpStatus.OK);
    } catch (Exception e) {
      return exceptionFormatter.defaultException(e);
    }
  }

  @DeleteMapping(value = "/{viewId}")
  public ResponseEntity<?> deleteViews(@PathVariable long viewId) {
    try {
      viewService.deleteView(viewId);
      return new ResponseEntity<>(null, HttpStatus.OK);
    } catch (Exception e) {
      return exceptionFormatter.defaultException(e);
    }
  }

}
