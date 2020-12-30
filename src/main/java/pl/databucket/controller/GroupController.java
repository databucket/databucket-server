package pl.databucket.controller;

import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pl.databucket.database.Condition;
import pl.databucket.database.FieldValidator;
import pl.databucket.database.ResultField;
import pl.databucket.exception.*;
import pl.databucket.model.beans.GroupBean;
import pl.databucket.model.entity.Group;
import pl.databucket.response.BaseResponse;
import pl.databucket.response.GroupResponse;
import pl.databucket.service.GroupService;

import java.util.*;

@CrossOrigin(origins = "*", allowedHeaders = "*")
@RequestMapping("/api/group")
@RestController
public class GroupController {

  private final CustomExceptionFormatter customExceptionFormatter = new CustomExceptionFormatter(LoggerFactory.getLogger(GroupController.class));

  @Autowired
  private GroupService groupService;

  @PostMapping(produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<?> createGroup(@RequestBody GroupBean groupBean) {
    try {
      Group newGroup = groupService.createGroup(groupBean);
      return new ResponseEntity<>(newGroup, HttpStatus.CREATED);
    } catch (GroupAlreadyExistsException e1) {
      return customExceptionFormatter.customException(new BaseResponse(), e1, HttpStatus.NOT_ACCEPTABLE);
    } catch (Exception e2) {
      return customExceptionFormatter.defaultException(new BaseResponse(), e2);
    }
  }

  @DeleteMapping(value = "/{groupId}", produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<BaseResponse> deleteGroup(@PathVariable("groupId") Integer groupId, @RequestParam String userName) {
    GroupResponse rb = new GroupResponse();
    try {
      groupService.deleteGroup(groupId, userName);
      return new ResponseEntity<>(rb, HttpStatus.OK);
//    } catch (ItemDoNotExistsException e) {
//      return customExceptionFormatter.customException(rb, e, HttpStatus.NOT_FOUND);
    } catch (Exception ee) {
      return customExceptionFormatter.defaultException(rb, ee);
    }
  }

  @GetMapping(value = {"", "/{groupId}"}, produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<BaseResponse> getGroups(
      @PathVariable Optional<Integer> groupId,
      @RequestParam(required = false) Optional<Integer> page,
      @RequestParam(required = false) Optional<Integer> limit,
      @RequestParam(required = false) Optional<String> sort,
      @RequestParam(required = false) Optional<String> filter) {
    GroupResponse rb = new GroupResponse();

    try {
      if (page.isPresent()) {
        FieldValidator.mustBeGraterThen0("page", page.get());
        rb.setPage(page.get());
      }

      if (limit.isPresent()) {
        FieldValidator.mustBeGraterOrEqual0("limit", limit.get());
        rb.setLimit(limit.get());
      }

      if (sort.isPresent()) {
        FieldValidator.validateSort(sort.get());
        rb.setSort(sort.get());
      }

      List<Condition> urlConditions = null;
      if (filter.isPresent()) {
        urlConditions = FieldValidator.validateFilter(filter.get());
      }

      Map<ResultField, Object> result = groupService.getGroups(groupId, page, limit, sort, urlConditions);

      long total = (long) result.get(ResultField.TOTAL);
      rb.setTotal(total);

      if (page.isPresent() && limit.isPresent()) {
        rb.setTotalPages((int) Math.ceil(total / (float) limit.get()));
      }

      rb.setGroups((List<Map<String, Object>>) result.get(ResultField.DATA));

      return new ResponseEntity<>(rb, HttpStatus.OK);
    } catch (IncorrectValueException e) {
      return customExceptionFormatter.customException(rb, e, HttpStatus.NOT_ACCEPTABLE);
    } catch (Exception ee) {
      return customExceptionFormatter.defaultException(rb, ee);
    }
  }

  @PutMapping(value = "/{groupId}", produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<BaseResponse> modifyGroup(@PathVariable("groupId") Integer groupId, @RequestParam String userName, @RequestBody LinkedHashMap<String, Object> body) {

    GroupResponse rb = new GroupResponse();

    try {
      groupService.modifyGroup(userName, groupId, body);
      rb.setMessage("Group '" + groupId + "' has been successfully modified.");
      return new ResponseEntity<>(rb, HttpStatus.OK);
    } catch (ItemDoNotExistsException e1) {
      return customExceptionFormatter.customException(rb, e1, HttpStatus.NOT_FOUND);
    } catch (GroupAlreadyExistsException e2) {
      return customExceptionFormatter.customException(rb, e2, HttpStatus.NOT_ACCEPTABLE);
    } catch (Exception ee) {
      return customExceptionFormatter.defaultException(rb, ee);
    }
  }
}
