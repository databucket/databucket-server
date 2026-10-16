package pl.databucket.server.controller;

import io.swagger.v3.oas.annotations.Hidden;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import pl.databucket.server.dto.GroupDto;
import pl.databucket.server.exception.*;
import pl.databucket.server.service.GroupService;

import jakarta.validation.Valid;
import java.util.List;

@Hidden
@CrossOrigin(origins = "*", allowedHeaders = "*")
@PreAuthorize("hasRole('ADMIN')")
@RequestMapping("/api/groups")
@RestController
public class GroupController {

  @Autowired
  private GroupService groupService;

  private final ExceptionFormatter exceptionFormatter = new ExceptionFormatter(GroupController.class);

  @PostMapping
  public ResponseEntity<?> createGroup(@Valid @RequestBody GroupDto groupDto) {
    try {
      groupDto = groupService.createGroup(groupDto);
      return new ResponseEntity<>(groupDto, HttpStatus.CREATED);
    } catch (ItemAlreadyExistsException e) {
      return exceptionFormatter.customException(e, HttpStatus.NOT_ACCEPTABLE);
    } catch (SomeItemsNotFoundException e) {
      return exceptionFormatter.customException(e, HttpStatus.NOT_FOUND);
    } catch (Exception e) {
      return exceptionFormatter.defaultException(e);
    }
  }

  @GetMapping
  public ResponseEntity<?> getGroups() {
    try {
      List<GroupDto> groupsDto = groupService.getGroups();
      return new ResponseEntity<>(groupsDto, HttpStatus.OK);
    } catch (Exception ee) {
      return exceptionFormatter.defaultException(ee);
    }
  }

  @PutMapping
  public ResponseEntity<?> modifyGroup(@Valid @RequestBody GroupDto groupDto) {
    try {
      groupDto = groupService.modifyGroup(groupDto);
      return new ResponseEntity<>(groupDto, HttpStatus.OK);
    } catch (ItemNotFoundException | SomeItemsNotFoundException | ModifyByNullEntityIdException e) {
      return exceptionFormatter.customException(e, HttpStatus.NOT_FOUND);
    } catch (Exception e) {
      return exceptionFormatter.defaultException(e);
    }
  }

  @DeleteMapping(value = "/{groupId}")
  public ResponseEntity<?> deleteGroup(@PathVariable("groupId") Long groupId) {
    try {
      groupService.deleteGroup(groupId);
      return new ResponseEntity<>(null, HttpStatus.NO_CONTENT);
    } catch (ItemNotFoundException e) {
      return exceptionFormatter.customException(e, HttpStatus.NOT_FOUND);
    } catch (Exception e) {
      return exceptionFormatter.defaultException(e);
    }
  }
}
