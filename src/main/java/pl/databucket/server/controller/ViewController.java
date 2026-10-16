package pl.databucket.server.controller;

import io.swagger.v3.oas.annotations.Hidden;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import pl.databucket.server.dto.ViewDto;
import pl.databucket.server.exception.ExceptionFormatter;
import pl.databucket.server.exception.ItemNotFoundException;
import pl.databucket.server.exception.ModifyByNullEntityIdException;
import pl.databucket.server.service.ViewService;

import jakarta.validation.Valid;
import java.util.List;

@Hidden
@CrossOrigin(origins = "*", allowedHeaders = "*")
@PreAuthorize("hasRole('ADMIN')")
@RequestMapping("/api/views")
@RestController
public class ViewController {

  private final ExceptionFormatter exceptionFormatter = new ExceptionFormatter(ViewController.class);

  @Autowired
  private ViewService viewService;

  @PostMapping
  public ResponseEntity<?> createView(@Valid @RequestBody ViewDto viewDto) {
    try {
      viewDto = viewService.createView(viewDto);
      return new ResponseEntity<>(viewDto, HttpStatus.CREATED);
    } catch (ItemNotFoundException e) {
      return exceptionFormatter.customException(e, HttpStatus.NOT_FOUND);
    } catch (Exception e) {
      return exceptionFormatter.defaultException(e);
    }
  }

  @GetMapping
  public ResponseEntity<?> getViews() {
    try {
      List<ViewDto> viewsDto = viewService.getViews();
      return new ResponseEntity<>(viewsDto, HttpStatus.OK);
    } catch (Exception ee) {
      return exceptionFormatter.defaultException(ee);
    }
  }

  @PutMapping
  public ResponseEntity<?> modifyView(@Valid @RequestBody ViewDto viewDto) {
    try {
      viewDto = viewService.modifyView(viewDto);
      return new ResponseEntity<>(viewDto, HttpStatus.OK);
    } catch (ItemNotFoundException | ModifyByNullEntityIdException e) {
      return exceptionFormatter.customException(e, HttpStatus.NOT_FOUND);
    } catch (Exception e) {
      return exceptionFormatter.defaultException(e);
    }
  }

  @DeleteMapping(value = "/{viewId}")
  public ResponseEntity<?> deleteViews(@PathVariable long viewId) {
    try {
      viewService.deleteView(viewId);
      return new ResponseEntity<>(null, HttpStatus.NO_CONTENT);
    } catch (Exception e) {
      return exceptionFormatter.defaultException(e);
    }
  }

}
