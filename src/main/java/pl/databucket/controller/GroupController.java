package pl.databucket.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pl.databucket.dto.GroupDto;
import pl.databucket.entity.Group;
import pl.databucket.exception.ExceptionFormatter;
import pl.databucket.exception.GroupAlreadyExistsException;
import pl.databucket.service.GroupService;
import pl.databucket.specification.GroupSpecification;

import javax.validation.Valid;

@CrossOrigin(origins = "*", allowedHeaders = "*")
@RequestMapping("/api/group")
@RestController
public class GroupController {

  @Autowired
  private GroupService groupService;

  private final ExceptionFormatter exceptionFormatter = new ExceptionFormatter(GroupController.class);

  @PostMapping
  public ResponseEntity<?> createGroup(@Valid @RequestBody GroupDto groupDto) {
    try {
      Group newGroup = groupService.createGroup(groupDto);
      return new ResponseEntity<>(newGroup, HttpStatus.CREATED);
    } catch (GroupAlreadyExistsException e1) {
      return exceptionFormatter.customException(e1, HttpStatus.NOT_ACCEPTABLE);
    } catch (Exception e2) {
      return exceptionFormatter.defaultException(e2);
    }
  }

  @GetMapping
  public ResponseEntity<?> getGroups(GroupSpecification customerSpec, Pageable pageable) {
    try {
      return new ResponseEntity<>(groupService.getGroups(customerSpec, pageable), HttpStatus.OK);
    } catch (Exception ee) {
      return exceptionFormatter.defaultException(ee);
    }
  }

  @PutMapping
  public ResponseEntity<?> modifyGroup(@Valid @RequestBody GroupDto groupDto) {
    try {
      Group group = groupService.modifyGroup(groupDto);
      return new ResponseEntity<>(group, HttpStatus.OK);
    } catch (Exception ee) {
      return exceptionFormatter.defaultException(ee);
    }
  }

  @DeleteMapping(value = "/{groupId}")
  public ResponseEntity<?> deleteGroup(@PathVariable("groupId") Long groupId) {
    try {
      groupService.deleteGroup(groupId);
      return new ResponseEntity<>(null, HttpStatus.OK);
    } catch (Exception ee) {
      return exceptionFormatter.defaultException(ee);
    }
  }
}
