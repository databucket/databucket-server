package pl.databucket.web.controller;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import pl.databucket.exception.BucketAlreadyExistsException;
import pl.databucket.exception.ItemDoNotExistsException;
import pl.databucket.exception.ClassAlreadyExistsException;
import pl.databucket.exception.ColumnsAlreadyExistsException;
import pl.databucket.exception.EmptyInputValueException;
import pl.databucket.exception.ExceededMaximumNumberOfCharactersException;
import pl.databucket.exception.FilterAlreadyExistsException;
import pl.databucket.exception.GroupAlreadyExistsException;
import pl.databucket.exception.IncorrectValueException;
import pl.databucket.exception.TagAlreadyExistsException;
import pl.databucket.exception.UnknownColumnException;
import pl.databucket.exception.ViewAlreadyExistsException;
import pl.databucket.web.database.C;
import pl.databucket.web.database.COL;
import pl.databucket.web.database.Condition;
import pl.databucket.web.database.FieldValidator;
import pl.databucket.web.service.DatabucketService;
import pl.databucket.web.service.ResponseBody;
import pl.databucket.web.service.ResponseStatus;

@CrossOrigin(origins = "*", allowedHeaders = "*")
@RequestMapping("/api")
@RestController
public class ApiController {

	@Autowired
	private DatabucketService databucketService;
	
	@Value("${databucket.title}")
	private String title;
	
//	@Autowired
//	private BuildProperties buildProperties;
	
	
	Logger logger = LoggerFactory.getLogger(ApiController.class);
	
	//--------------------------------------- C O N F I G U R A T I O N ---------------------------------------------
	
	@GetMapping(value = "/title", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ResponseBody> getTitle() {
		ResponseBody rb = new ResponseBody();

		try {
			rb.setTitle(this.title);
			rb.setStatus(ResponseStatus.OK);
			return new ResponseEntity<ResponseBody>(rb, HttpStatus.OK); 
		} catch (Exception ee) {
			return defaultException(rb, ee);
		}			
	}
	
	@GetMapping(value = "/version", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ResponseBody> getVersion() {
		ResponseBody rb = new ResponseBody();

		try {
//			rb.setVersion(this.buildProperties.getVersion());
			rb.setVersion("2.2.5");
			rb.setStatus(ResponseStatus.OK);
			return new ResponseEntity<ResponseBody>(rb, HttpStatus.OK); 
		} catch (Exception ee) {
			return defaultException(rb, ee);
		}			
	}
	
	//----------------------------------------------- G R O U P S ---------------------------------------------------

	@PostMapping(value = "/groups", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ResponseBody> createGroup(
			@RequestParam(required = true) String userName,
			@RequestBody LinkedHashMap<String, Object> body) {
		ResponseBody rb = new ResponseBody();
		
		String groupName = null;
		String description = null;
		ArrayList<Integer> buckets = null;
		
		try {			
			groupName = FieldValidator.validateGroupName(body, true);
			description = FieldValidator.validateDescription(body, false);
			buckets = FieldValidator.validateBuckets(body, false);
			
			int groupId = databucketService.createGroup(userName, groupName, description, buckets);
			rb.setStatus(ResponseStatus.OK);
			rb.setGroupId(groupId);
			rb.setMessage("The group '" + groupName + "' has been successfully created.");
			return new ResponseEntity<ResponseBody>(rb, HttpStatus.CREATED);
		} catch (GroupAlreadyExistsException | ExceededMaximumNumberOfCharactersException | EmptyInputValueException e) {
			return customException(rb, e, HttpStatus.NOT_ACCEPTABLE);
		} catch (Exception ee) {
			return defaultException(rb, ee);
		}
	}
	
	@DeleteMapping(value = "/groups/{groupId}", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ResponseBody> deleteGroup(
			@PathVariable("groupId") Integer groupId,
			@RequestParam(required = true) String userName) {
		ResponseBody rb = new ResponseBody();
		try {
			databucketService.deleteGroup(groupId, userName);
			rb.setStatus(ResponseStatus.OK);
			return new ResponseEntity<ResponseBody>(rb, HttpStatus.OK);
		} catch (ItemDoNotExistsException e) {
			return customException(rb, e, HttpStatus.NOT_FOUND);
		} catch (Exception ee) {
			return defaultException(rb, ee);
		}		
	}
	
	@SuppressWarnings("unchecked")
	@GetMapping(value = {"/groups", "/groups/{groupId}"}, produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ResponseBody> getGroups(
			@PathVariable Optional<Integer> groupId,
			@RequestParam(required = false) Optional<Integer> page,
			@RequestParam(required = false) Optional<Integer> limit,
			@RequestParam(required = false) Optional<String> sort,
			@RequestParam(required = false) Optional<String> filter) {
		ResponseBody rb = new ResponseBody();
					
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
			
			Map<String, Object> result = databucketService.getGroups(groupId, page, limit, sort, urlConditions);
				
			long total = (long) result.get(C.TOTAL);
			rb.setTotal(total);
						
			if (page.isPresent() && limit.isPresent())
				rb.setTotalPages((int) Math.ceil(total/(float) limit.get()));
				
			rb.setGroups((List<Map<String, Object>>) result.get(C.GROUPS));
			
			return new ResponseEntity<ResponseBody>(rb, HttpStatus.OK); 
		} catch (IncorrectValueException e) {
			return customException(rb, e, HttpStatus.NOT_ACCEPTABLE);
		} catch (Exception ee) {
			return defaultException(rb, ee);
		}
	}
	
	@PutMapping(value = "/groups/{groupId}", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ResponseBody> modifyGroup(
			@PathVariable("groupId") Integer groupId,
			@RequestParam(required = true) String userName,
			@RequestBody LinkedHashMap<String, Object> body) {
		
		ResponseBody rb = new ResponseBody();
		
		try {
			databucketService.modifyGroup(userName, groupId, body);
			rb.setStatus(ResponseStatus.OK);
			rb.setMessage("Group '" + groupId + "' has been successfully modified.");
			return new ResponseEntity<ResponseBody>(rb, HttpStatus.OK);
		} catch (ItemDoNotExistsException e1) {
			return customException(rb, e1, HttpStatus.NOT_FOUND);
		} catch (GroupAlreadyExistsException | ExceededMaximumNumberOfCharactersException | IncorrectValueException e2) {
			return customException(rb, e2, HttpStatus.NOT_ACCEPTABLE);
		} catch (Exception ee) {
			return defaultException(rb, ee);
		}
	}
	
	//------------------------------------------- D A T A   C L A S S -----------------------------------------------

	@PostMapping(value = "/classes", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ResponseBody> createClass(
			@RequestParam(required = true) String userName,
			@RequestBody LinkedHashMap<String, Object> body) {
		ResponseBody rb = new ResponseBody();
		
		String className = null;
		String description = null;
		
		try {			
			className = FieldValidator.validateClassName(body, true);
			description = FieldValidator.validateDescription(body, false);
			
			int classId = databucketService.createClass(userName, className, description);
			rb.setStatus(ResponseStatus.OK);
			rb.setClassId(classId);
			rb.setMessage("The class '" + className + "' has been successfully created.");
			return new ResponseEntity<ResponseBody>(rb, HttpStatus.CREATED);
		} catch (ClassAlreadyExistsException | ExceededMaximumNumberOfCharactersException | EmptyInputValueException e) {
			return customException(rb, e, HttpStatus.NOT_ACCEPTABLE);
		} catch (Exception ee) {
			return defaultException(rb, ee);
		}
	}
	
	@DeleteMapping(value = "/classes/{classId}", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ResponseBody> deleteClass(
			@PathVariable("classId") Integer classId,
			@RequestParam(required = true) String userName) {
		ResponseBody rb = new ResponseBody();
		try {
			databucketService.deleteClass(classId, userName);
			rb.setStatus(ResponseStatus.OK);
			return new ResponseEntity<ResponseBody>(rb, HttpStatus.OK);
		} catch (ItemDoNotExistsException e) {
			return customException(rb, e, HttpStatus.NOT_FOUND);
		} catch (Exception ee) {
			return defaultException(rb, ee);
		}		
	}
	
	@SuppressWarnings("unchecked")
	@GetMapping(value = {"/classes", "/classes/{classId}"}, produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ResponseBody> getClasses(
			@PathVariable Optional<Integer> classId,
			@RequestParam(required = false) Optional<Integer> page,
			@RequestParam(required = false) Optional<Integer> limit,
			@RequestParam(required = false) Optional<String> sort,
			@RequestParam(required = false) Optional<String> filter) {
		ResponseBody rb = new ResponseBody();
					
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
			
			Map<String, Object> result = databucketService.getClasses(classId, page, limit, sort, urlConditions);
				
			long total = (long) result.get(C.TOTAL);
			rb.setTotal(total);
						
			if (page.isPresent() && limit.isPresent())
				rb.setTotalPages((int) Math.ceil(total/(float) limit.get()));
				
			rb.setClasses((List<Map<String, Object>>) result.get(C.CLASSES));
			
			return new ResponseEntity<ResponseBody>(rb, HttpStatus.OK); 
		} catch (IncorrectValueException e) {
			return customException(rb, e, HttpStatus.NOT_ACCEPTABLE);
		} catch (Exception ee) {
			return defaultException(rb, ee);
		}
	}
	
	@PutMapping(value = "/classes/{classId}", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ResponseBody> modifyClass(
			@PathVariable("classId") Integer classId,
			@RequestParam(required = true) String userName,
			@RequestBody LinkedHashMap<String, Object> body) {
		
		ResponseBody rb = new ResponseBody();
		
		try {
			databucketService.modifyClass(userName, classId, body);
			rb.setStatus(ResponseStatus.OK);
			rb.setMessage("Class '" + classId + "' has been successfully modified.");
			return new ResponseEntity<ResponseBody>(rb, HttpStatus.OK);
		} catch (ItemDoNotExistsException e1) {
			return customException(rb, e1, HttpStatus.NOT_FOUND);
		} catch (ClassAlreadyExistsException | ExceededMaximumNumberOfCharactersException | IncorrectValueException e2) {
			return customException(rb, e2, HttpStatus.NOT_ACCEPTABLE);
		} catch (Exception ee) {
			return defaultException(rb, ee);
		}
	}
	
	//---------------------------------------------- B U C K E T S --------------------------------------------------
	
	@PostMapping(value = "/buckets", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ResponseBody> createBucket(
			@RequestParam(required = true) String userName,
			@RequestBody LinkedHashMap<String, Object> body) {
		ResponseBody rb = new ResponseBody();
		
		try {			
			String bucketName = FieldValidator.validateBucketName(body, true);
			Integer index = FieldValidator.validateIndex(body, false);
			String description = FieldValidator.validateDescription(body, false);
			String iconName = FieldValidator.validateIcon(body, false);
			Integer classId = FieldValidator.validateNullableId(body, COL.CLASS_ID, false);
			
			boolean history = false;
			if (body.containsKey(COL.HISTORY))
				history = (boolean) body.get(COL.HISTORY);
			
			int bucketId = databucketService.createBucket(userName, bucketName, index, description, iconName, history, classId);
			rb.setStatus(ResponseStatus.OK);
			rb.setBucketId(bucketId);
			rb.setMessage("The bucket '" + bucketName + "' has been successfully created.");
			return new ResponseEntity<ResponseBody>(rb, HttpStatus.CREATED);
		} catch (BucketAlreadyExistsException | ExceededMaximumNumberOfCharactersException | EmptyInputValueException e) {
			return customException(rb, e, HttpStatus.NOT_ACCEPTABLE);
		} catch (Exception ee) {
			return defaultException(rb, ee);
		}
	}
	
	@DeleteMapping(value = "/buckets/{bucketName}", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ResponseBody> deleteBucket(
			@PathVariable("bucketName") String bucketName,
			@RequestParam(required = true) String userName) {
		ResponseBody rb = new ResponseBody();
		try {
			databucketService.deleteBucket(bucketName, userName);
			rb.setStatus(ResponseStatus.OK);
			return new ResponseEntity<ResponseBody>(rb, HttpStatus.OK);
		} catch (ItemDoNotExistsException e) {
			return customException(rb, e, HttpStatus.NOT_FOUND);
		} catch (Exception ee) {
			return defaultException(rb, ee);
		}		
	}
	
	@SuppressWarnings("unchecked")
	@GetMapping(value = {"/buckets", "/buckets/{bucketName}"}, produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ResponseBody> getBuckets(
			@PathVariable Optional<String> bucketName,
			@RequestParam(required = false) Optional<Integer> page,
			@RequestParam(required = false) Optional<Integer> limit,
			@RequestParam(required = false) Optional<String> sort,
			@RequestParam(required = false) Optional<String> filter) {
		ResponseBody rb = new ResponseBody();
					
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
			
			Map<String, Object> result = databucketService.getBuckets(bucketName, page, limit, sort, urlConditions);
				
			long total = (long) result.get(C.TOTAL);
			rb.setTotal(total);
						
			if (page.isPresent() && limit.isPresent())
				rb.setTotalPages((int) Math.ceil(total/(float) limit.get()));
				
			rb.setBuckets((List<Map<String, Object>>) result.get(C.BUCKETS));
			
			return new ResponseEntity<ResponseBody>(rb, HttpStatus.OK); 
		} catch (IncorrectValueException e) {
			return customException(rb, e, HttpStatus.NOT_ACCEPTABLE);
		} catch (Exception ee) {
			return defaultException(rb, ee);
		}
	}
	
	@GetMapping(value = "/buckets/{bucketName}/info", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ResponseBody> getBucketInfo(@PathVariable String bucketName) {
		ResponseBody rb = new ResponseBody();

		try {
			rb.setStatistic(databucketService.getStatistic(bucketName));
			rb.setStatus(ResponseStatus.OK);
			return new ResponseEntity<ResponseBody>(rb, HttpStatus.OK); 
		} catch (ItemDoNotExistsException e) {
			return customException(rb, e, HttpStatus.NOT_FOUND);
		} catch (Exception ee) {
			return defaultException(rb, ee);
		}			
	}
	
	@PutMapping(value = "/buckets/{bucketName}", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ResponseBody> modifyBucket(
			@PathVariable("bucketName") String bucketName,
			@RequestParam(required = true) String userName,
			@RequestBody LinkedHashMap<String, Object> body) {
		
		ResponseBody rb = new ResponseBody();
		
		try {
			databucketService.modifyBucket(userName, bucketName, body);
			rb.setStatus(ResponseStatus.OK);
			rb.setMessage("Bucket '" + bucketName + "' has been successfully modified.");
			return new ResponseEntity<ResponseBody>(rb, HttpStatus.OK);
		} catch (ItemDoNotExistsException e1) {
			return customException(rb, e1, HttpStatus.NOT_FOUND);
		} catch (BucketAlreadyExistsException | ExceededMaximumNumberOfCharactersException | IncorrectValueException e2) {
			return customException(rb, e2, HttpStatus.NOT_ACCEPTABLE);
		} catch (Exception ee) {
			return defaultException(rb, ee);
		}
	}
	
	//------------------------------------------------- T A G S -----------------------------------------------------
	
	@PostMapping(value = "/tags", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ResponseBody> createTag(
			@RequestParam(required = true) String userName,
			@RequestBody LinkedHashMap<String, Object> body) {
		
		ResponseBody rb = new ResponseBody();
		
		try {
			Integer bucketId = FieldValidator.validateNullableId(body, COL.BUCKET_ID, false);
			String bucketName = FieldValidator.validateBucketName(body, false);
			String tagName = FieldValidator.validateTagName(body, true);
			String description = FieldValidator.validateDescription(body, false);
			String iconName = FieldValidator.validateIcon(body, false);
			Integer classId = FieldValidator.validateNullableId(body, COL.CLASS_ID, false);
			
			Integer tagId = databucketService.createTag(userName, tagName, bucketId, bucketName, iconName, description, classId);
			rb.setStatus(ResponseStatus.OK);
			rb.setTagId(tagId);
			rb.setMessage("The new tag has been successfully created.");
			return new ResponseEntity<ResponseBody>(rb, HttpStatus.CREATED);
		} catch (ItemDoNotExistsException e1) {
			return customException(rb, e1, HttpStatus.NOT_FOUND);
		} catch (EmptyInputValueException | ExceededMaximumNumberOfCharactersException | IncorrectValueException | TagAlreadyExistsException e2) {
			return customException(rb, e2, HttpStatus.NOT_ACCEPTABLE);
		} catch (Exception ee) {
			return defaultException(rb, ee);
		}
	}
	
	@DeleteMapping(value = "/tags/{tagId}", produces = MediaType.APPLICATION_JSON_VALUE)	
	public ResponseEntity<ResponseBody> deleteTags(
			@PathVariable Integer tagId,
			@RequestParam(required = true) String userName) {
		
		ResponseBody rb = new ResponseBody();
		
		try {
			databucketService.deleteTag(userName, tagId);
			rb.setStatus(ResponseStatus.OK);
			rb.setMessage("The tag has been removed.");
			return new ResponseEntity<ResponseBody>(rb, HttpStatus.OK);
		} catch (Exception ee) {
			return defaultException(rb, ee);
		}		
	}
	
	@SuppressWarnings("unchecked")
	@GetMapping(value = {
			"/tags", 
			"/tags/{tagId}", 
			"/tags/buckets/{bucketName}"}, produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ResponseBody> getTags(
			@PathVariable Optional<String> bucketName, 
			@PathVariable Optional<Integer> tagId,
			@RequestParam(required = false) Optional<Integer> page,
			@RequestParam(required = false) Optional<Integer> limit,
			@RequestParam(required = false) Optional<String> sort,
			@RequestParam(required = false) Optional<String> filter) {
		
		ResponseBody rb = new ResponseBody();
		
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
			
			Map<String, Object> result = databucketService.getTags(bucketName, tagId, page, limit, sort, urlConditions);
			
			long total = (long) result.get(C.TOTAL);
			rb.setTotal(total);
						
			if (page.isPresent() && limit.isPresent())
				rb.setTotalPages((int) Math.ceil(total/(float) limit.get()));
			
			rb.setTags((List<Map<String, Object>>) result.get(C.TAGS));
						
			return new ResponseEntity<ResponseBody>(rb, HttpStatus.OK);
		} catch (ItemDoNotExistsException e1) {
			return customException(rb, e1, HttpStatus.NOT_FOUND);
		} catch (IncorrectValueException e2) {
			return customException(rb, e2, HttpStatus.NOT_ACCEPTABLE);
		} catch (Exception ee) {
			return defaultException(rb, ee);
		}
	}
	
	@PutMapping(value = "/tags/{tagId}", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ResponseBody> modifyTag(
			@PathVariable("tagId") Integer tagId,
			@RequestParam(required = true) String userName,
			@RequestBody LinkedHashMap<String, Object> body) {
		
		ResponseBody rb = new ResponseBody();
		
		try {
			Integer bucketId = FieldValidator.validateNullableId(body, COL.BUCKET_ID, false);
			Integer classId = FieldValidator.validateNullableId(body, COL.CLASS_ID, false);
			String tagName = FieldValidator.validateTagName(body, false);
			String description = FieldValidator.validateDescription(body, false);
			
			databucketService.modifyTag(userName, tagId, tagName, bucketId, classId, description);
			rb.setStatus(ResponseStatus.OK);
			rb.setMessage("Tag with id '" + tagId + "' has been successfully modified.");
			return new ResponseEntity<ResponseBody>(rb, HttpStatus.OK);
		} catch (ItemDoNotExistsException e1) {
			return customException(rb, e1, HttpStatus.NOT_FOUND);
		} catch (TagAlreadyExistsException | IncorrectValueException | ExceededMaximumNumberOfCharactersException e2) {
			return customException(rb, e2, HttpStatus.NOT_ACCEPTABLE);
		} catch (Exception ee) {
			return defaultException(rb, ee);
		}
	}
	
	//---------------------------------------------- C O L U M N S --------------------------------------------------
	
	@PostMapping(value = "/columns", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ResponseBody> createColumns(
			@RequestParam(required = true) String userName,
			@RequestBody Map<String, Object> body) {
		ResponseBody rb = new ResponseBody();
		
		try {
			String columnsName = FieldValidator.validateColumnsName(body, true);
			Integer bucketId = FieldValidator.validateNullableId(body, COL.BUCKET_ID, false);			
			List<Map<String, Object>> columns = FieldValidator.validateColumns(body, true);
			String description = FieldValidator.validateDescription(body, false);
			Integer classId = FieldValidator.validateNullableId(body, COL.CLASS_ID, false);
			
			int columnsId = databucketService.createColumns(columnsName, bucketId, userName, columns, description, classId);
			rb.setStatus(ResponseStatus.OK);
			rb.setColumnsId(columnsId);
			rb.setMessage("The new columns definition has been successfully created.");
			return new ResponseEntity<ResponseBody>(rb, HttpStatus.CREATED);
		} catch (ItemDoNotExistsException e) {
			return customException(rb, e, HttpStatus.NOT_FOUND);
		} catch (EmptyInputValueException | ColumnsAlreadyExistsException e1) {
			return customException(rb, e1, HttpStatus.NOT_ACCEPTABLE);			
		} catch (Exception ee) {
			return defaultException(rb, ee);
		}
	}
	
	@DeleteMapping(value = "/columns/{columnsId}", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ResponseBody> deleteColumns(
			@PathVariable Integer columnsId,
			@RequestParam(required = true) String userName) {
		ResponseBody rb = new ResponseBody();
		try {
			databucketService.deleteColumns(userName, columnsId);
			rb.setStatus(ResponseStatus.OK);
			rb.setMessage("The columns definition has been removed.");
			return new ResponseEntity<ResponseBody>(rb, HttpStatus.OK);
		} catch (Exception ee) {
			return defaultException(rb, ee);
		}		
	}
	
	@SuppressWarnings("unchecked")
	@GetMapping(value = {
			"/columns", 
			"/columns/{columnsId}", 
			"/columns/buckets/{bucketName}"}, produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ResponseBody> getColumns(
			@PathVariable Optional<String> bucketName,
			@PathVariable Optional<Integer> columnsId,
			@RequestParam(required = false) Optional<Integer> page,
			@RequestParam(required = false) Optional<Integer> limit,
			@RequestParam(required = false) Optional<String> sort,
			@RequestParam(required = false) Optional<String> filter) {
		
		ResponseBody rb = new ResponseBody();
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
			
			Map<String, Object> result = databucketService.getColumns(bucketName, columnsId, page, limit, sort, urlConditions);
				
			long total = (long) result.get(C.TOTAL);
			rb.setTotal(total);
						
			if (page.isPresent() && limit.isPresent())
				rb.setTotalPages((int) Math.ceil(total/(float) limit.get()));
				
			rb.setColumns((List<Map<String, Object>>) result.get(C.COLUMNS));
			
			return new ResponseEntity<ResponseBody>(rb, HttpStatus.OK);
		} catch (ItemDoNotExistsException e) {
			return customException(rb, e, HttpStatus.NOT_FOUND);
		} catch (IncorrectValueException e2) {
			return customException(rb, e2, HttpStatus.NOT_ACCEPTABLE);
		} catch (Exception ee) {
			return defaultException(rb, ee);
		}
	}
	
	@PutMapping(value = "/columns/{columnsId}", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ResponseBody> modifyColumns(
			@PathVariable("columnsId") Integer columnsId,
			@RequestParam(required = true) String userName,
			@RequestBody LinkedHashMap<String, Object> body) {
		ResponseBody rb = new ResponseBody();
		try {
			Integer bucketId = FieldValidator.validateNullableId(body, COL.BUCKET_ID, false);
			Integer classId = FieldValidator.validateNullableId(body, COL.CLASS_ID, false);
			String columnsName = FieldValidator.validateColumnsName(body, false);
			String description = FieldValidator.validateDescription(body, false);
			List<Map<String, Object>> columns = FieldValidator.validateColumns(body, false);
			
			databucketService.modifyColumns(userName, columnsId, columnsName, bucketId, classId, description, columns);
			rb.setStatus(ResponseStatus.OK);
			rb.setMessage("Columns with id '" + columnsId + "' have been successfully modified.");
			return new ResponseEntity<ResponseBody>(rb, HttpStatus.OK);
		} catch (ItemDoNotExistsException e1) {
			return customException(rb, e1, HttpStatus.NOT_FOUND);
		} catch (EmptyInputValueException | ExceededMaximumNumberOfCharactersException e2) {
			return customException(rb, e2, HttpStatus.NOT_ACCEPTABLE);
		} catch (Exception ee) {
			return defaultException(rb, ee);
		}
	}
	
	
	//---------------------------------------------- F I L T E R S --------------------------------------------------
	
	@PostMapping(value = "/filters", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ResponseBody> createFilter(
			@RequestParam(required = true) String userName,
			@RequestBody Map<String, Object> body) {
		ResponseBody rb = new ResponseBody();
		
		try {
			String filterName = FieldValidator.validateFilterName(body, true);
			Integer bucketId = null;
			if (body.containsKey(COL.BUCKET_ID))
				bucketId = (Integer) body.get(COL.BUCKET_ID);
			
			List<Map<String, Object>> conditions = FieldValidator.validateConditions(body, true);
			String description = FieldValidator.validateDescription(body, false);
			Integer classId = FieldValidator.validateNullableId(body, COL.CLASS_ID, false);		
			
			int filterId = databucketService.createFilter(filterName, bucketId, userName, conditions, description, classId);
			rb.setStatus(ResponseStatus.OK);
			rb.setFilterId(filterId);
			rb.setMessage("The new filter has been successfully created.");
			return new ResponseEntity<ResponseBody>(rb, HttpStatus.CREATED);
		} catch (ItemDoNotExistsException e) {
			return customException(rb, e, HttpStatus.NOT_FOUND);
		} catch (EmptyInputValueException | FilterAlreadyExistsException e1) {
			return customException(rb, e1, HttpStatus.NOT_ACCEPTABLE);			
		} catch (Exception ee) {
			return defaultException(rb, ee);
		}
	}
	
	@DeleteMapping(value = "/filters/{filterId}", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ResponseBody> deleteFilters(
			@PathVariable Integer filterId,
			@RequestParam(required = true) String userName) {
		ResponseBody rb = new ResponseBody();
		try {
			databucketService.deleteFilter(userName, filterId);
			rb.setStatus(ResponseStatus.OK);
			rb.setMessage("The filter has been removed.");
			return new ResponseEntity<ResponseBody>(rb, HttpStatus.OK);
		} catch (Exception ee) {
			return defaultException(rb, ee);
		}		
	}
	
	@SuppressWarnings("unchecked")
	@GetMapping(value = {
			"/filters", 
			"/filters/{filterId}", 
			"/filters/buckets/{bucketName}"}, produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ResponseBody> getFilters(
			@PathVariable Optional<String> bucketName,
			@PathVariable Optional<Integer> filterId,
			@RequestParam(required = false) Optional<Integer> page,
			@RequestParam(required = false) Optional<Integer> limit,
			@RequestParam(required = false) Optional<String> sort,
			@RequestParam(required = false) Optional<String> filter) {
		
		ResponseBody rb = new ResponseBody();
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
			
			Map<String, Object> result = databucketService.getFilters(bucketName, filterId, page, limit, sort, urlConditions);
				
			long total = (long) result.get(C.TOTAL);
			rb.setTotal(total);
						
			if (page.isPresent() && limit.isPresent())
				rb.setTotalPages((int) Math.ceil(total/(float) limit.get()));
				
			rb.setFilters((List<Map<String, Object>>) result.get(C.FILTERS));
			
			return new ResponseEntity<ResponseBody>(rb, HttpStatus.OK);
		} catch (ItemDoNotExistsException e) {
			return customException(rb, e, HttpStatus.NOT_FOUND);
		} catch (IncorrectValueException e2) {
			return customException(rb, e2, HttpStatus.NOT_ACCEPTABLE);
		} catch (Exception ee) {
			return defaultException(rb, ee);
		}
	}
	
	@PutMapping(value = "/filters/{filterId}", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ResponseBody> modifyFilter(
			@PathVariable("filterId") Integer filterId,
			@RequestParam(required = true) String userName,
			@RequestBody LinkedHashMap<String, Object> body) {
		ResponseBody rb = new ResponseBody();
		try {
			Integer bucketId = FieldValidator.validateNullableId(body, COL.BUCKET_ID, false);
			Integer classId = FieldValidator.validateNullableId(body, COL.CLASS_ID, false);
			String filterName = FieldValidator.validateFilterName(body, false);
			String description = FieldValidator.validateDescription(body, false);
			List<Map<String, Object>> conditions = FieldValidator.validateConditions(body, false);
			
			databucketService.modifyFilter(userName, filterId, filterName, bucketId, classId, description, conditions);
			rb.setStatus(ResponseStatus.OK);
			rb.setMessage("Filter with id '" + filterId + "' has been successfully modified.");
			return new ResponseEntity<ResponseBody>(rb, HttpStatus.OK);
		} catch (ItemDoNotExistsException e1) {
			return customException(rb, e1, HttpStatus.NOT_FOUND);
		} catch (EmptyInputValueException | ExceededMaximumNumberOfCharactersException e2) {
			return customException(rb, e2, HttpStatus.NOT_ACCEPTABLE);
		} catch (Exception ee) {
			return defaultException(rb, ee);
		}
	}
	
	//---------------------------------------------- B U N D L E S --------------------------------------------------
	
	@SuppressWarnings("unchecked")
	@PostMapping(value = "/buckets/{bucketName}/bundles/custom", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ResponseBody> getBundlesCustom(
			@PathVariable(required = true) String bucketName,
			@RequestParam(required = false) Optional<Integer> page,
			@RequestParam(required = false) Optional<Integer> limit,
			@RequestParam(required = false) Optional<String> sort,
			@RequestBody LinkedHashMap<String, Object> body) {
		
		ResponseBody rb = new ResponseBody();
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
			
			List<Map<String, Object>> columns = FieldValidator.validateColumns(body, false);
			List<Condition> conditions = FieldValidator.validateListOfConditions(body, false);
						
			Map<String, Object> result = databucketService.getBundles(bucketName, Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.ofNullable(columns), Optional.ofNullable(conditions), page, limit, sort);
			
			long total = (long) result.get("total");
			rb.setTotal(total);
			
			if (page.isPresent() && limit.isPresent())
				rb.setTotalPages((int) Math.ceil(total/(float) limit.get()));			
			
			rb.setBundles((List<Map<String, Object>>) result.get("bundles"));
			
			if (rb.getBundles().size() > 0)
				return new ResponseEntity<ResponseBody>(rb, HttpStatus.OK);
			else {
				if (limit.get() > 0) {
					rb.setMessage("No bundle meets the given requirements!");
					rb.setStatus(ResponseStatus.NO_DATA);
				}
				return new ResponseEntity<ResponseBody>(rb, HttpStatus.OK);
			}
		} catch (ItemDoNotExistsException e1) {
			return customException(rb, e1, HttpStatus.NOT_FOUND);
		} catch (IncorrectValueException | UnknownColumnException e2) {
			return customException(rb, e2, HttpStatus.NOT_ACCEPTABLE);
		} catch (Exception ee) {
			return defaultException(rb, ee);
		}					
	}
	
	@SuppressWarnings("unchecked")
	@GetMapping(value = {
			"/buckets/{bucketName}/bundles", 
			"/buckets/{bucketName}/bundles/{bundleId}",
			"/buckets/{bucketName}/bundles/tags/{tagId}",
			"/buckets/{bucketName}/bundles/filters/{filterId}",
			"/buckets/{bucketName}/bundles/views/{viewId}"}, 
			produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ResponseBody> getBundles(
			@PathVariable(required = true) String bucketName,
			@PathVariable(required = false) Optional<Integer[]> bundleId,
			@PathVariable(required = false) Optional<Integer[]> tagId,
			@PathVariable(required = false) Optional<Integer> filterId,
			@PathVariable(required = false) Optional<Integer> viewId,
			@RequestParam(required = false) Optional<Integer> page,
			@RequestParam(required = false) Optional<Integer> limit,
			@RequestParam(required = false) Optional<String> sort) {
		
		ResponseBody rb = new ResponseBody();
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
			
			Map<String, Object> result = databucketService.getBundles(bucketName, bundleId, tagId, filterId, viewId, Optional.empty(), Optional.empty(), page, limit, sort);
			
			long total = (long) result.get("total");
			rb.setTotal(total);
			
			if (page.isPresent() && limit.isPresent())
				rb.setTotalPages((int) Math.ceil(total/(float) limit.get()));			
			
			rb.setBundles((List<Map<String, Object>>) result.get("bundles"));
			
			if (rb.getBundles().size() > 0)
				return new ResponseEntity<ResponseBody>(rb, HttpStatus.OK);
			else {
				rb.setMessage("No bundle meets the given requirements!");
				rb.setStatus(ResponseStatus.NO_DATA);
				return new ResponseEntity<ResponseBody>(rb, HttpStatus.OK);
			}				
		} catch (ItemDoNotExistsException e1) {
			return customException(rb, e1, HttpStatus.NOT_FOUND);
		} catch (IncorrectValueException | UnknownColumnException e2) {
			return customException(rb, e2, HttpStatus.NOT_ACCEPTABLE);
		} catch (Exception ee) {
			return defaultException(rb, ee);
		}			
	}
	
	@GetMapping(value = {
			"/buckets/{bucketName}/bundles/lock", 
			"/buckets/{bucketName}/bundles/tags/{tagId}/lock",
			"/buckets/{bucketName}/bundles/filters/{filterId}/lock"}, produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ResponseBody> lockBundles(
			@PathVariable(required = true) String bucketName,
			@PathVariable(required = false) Optional<Integer[]> tagId,
			@PathVariable(required = false) Optional<Integer> filterId,
			@RequestParam(required = true) String userName,
			@RequestParam(required = false) Optional<Integer> page,
			@RequestParam(required = false) Optional<Integer> limit,
			@RequestParam(required = false) Optional<String> sort) {
		
		ResponseBody rb = new ResponseBody();
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
			
			List<Integer> bundlesIdsList = databucketService.lockBundles(bucketName, userName, tagId, filterId, Optional.empty(), page, limit, sort);
			
			if (bundlesIdsList != null && bundlesIdsList.size() > 0) {
				Integer[] bundlesIds = bundlesIdsList.toArray(new Integer[bundlesIdsList.size()]);
				return getBundles(bucketName, Optional.of(bundlesIds), Optional.empty(), Optional.empty(), Optional.empty(), page, limit, sort);
			} else {
				rb.setMessage("No bundle meets the given requirements!");
				rb.setBundlesIds(null);
				rb.setStatus(ResponseStatus.NO_DATA);
				return new ResponseEntity<ResponseBody>(rb, HttpStatus.OK);
			}			
		} catch (ItemDoNotExistsException e) {
			return customException(rb, e, HttpStatus.NOT_FOUND);
		} catch (IncorrectValueException | UnknownColumnException e2) {
			return customException(rb, e2, HttpStatus.NOT_ACCEPTABLE);
		} catch (Exception ee) {
			return defaultException(rb, ee);
		}			
	}
	
	// Lock bundles with given conditions
	@PostMapping(value = {"/buckets/{bucketName}/bundles/custom/lock"}, produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ResponseBody> lockBundles(
			@PathVariable("bucketName") String bucketName,
			@RequestParam(required = true) String userName,
			@RequestParam(required = false) Optional<Integer> page,
			@RequestParam(required = false) Optional<Integer> limit,
			@RequestParam(required = false) Optional<String> sort,
			@RequestBody LinkedHashMap<String, Object> body) {
		
		ResponseBody rb = new ResponseBody();
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
			
			List<Condition> conditions = FieldValidator.validateListOfConditions(body, true);
			
			List<Integer> bundlesIdsList = databucketService.lockBundles(bucketName, userName, Optional.empty(), Optional.empty(), Optional.of(conditions), page, limit, sort);
			if (bundlesIdsList != null && bundlesIdsList.size() > 0) {
				Integer[] bundlesIds = bundlesIdsList.toArray(new Integer[bundlesIdsList.size()]);
				return getBundles(bucketName, Optional.of(bundlesIds), Optional.empty(), Optional.empty(), Optional.empty(), page, limit, sort);
			} else {
				rb.setMessage("No bundle meets the given requirements!");
				rb.setBundlesIds(null);
				rb.setStatus(ResponseStatus.NO_DATA);
				return new ResponseEntity<ResponseBody>(rb, HttpStatus.OK);
			}				
		} catch (ItemDoNotExistsException e) {
			return customException(rb, e, HttpStatus.NOT_FOUND);
		} catch (IncorrectValueException | UnknownColumnException e2) {
			return customException(rb, e2, HttpStatus.NOT_ACCEPTABLE);
		} catch (Exception ee) {
			return defaultException(rb, ee);
		}			
	}
	
	@PostMapping(value = "/buckets/{bucketName}/bundles", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ResponseBody> createBundle(
			@PathVariable("bucketName") String bucketName,
			@RequestParam(required = true) String userName,
			@RequestBody Map<String, Object> body) {
		
		ResponseBody rb = new ResponseBody();
		try {
			Integer bundleId = databucketService.createBundle(userName, bucketName, body);
			rb.setStatus(ResponseStatus.OK);
			rb.setBundleId(bundleId);
			rb.setMessage("The new bundle has been successfully created.");
			return new ResponseEntity<ResponseBody>(rb, HttpStatus.CREATED);
		} catch (ItemDoNotExistsException e1) {
			return customException(rb, e1, HttpStatus.NOT_FOUND);
		} catch (Exception ee) {
			return defaultException(rb, ee);
		}		
	}
	
	@PutMapping(value = {
			"/buckets/{bucketName}/bundles",
			"/buckets/{bucketName}/bundles/{bundlesIds}", 
			"/buckets/{bucketName}/bundles/filters/{filterId}", 
			"/buckets/{bucketName}/bundles/tags/{tagsIds}"}, produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ResponseBody> modifyBundles(
			@PathVariable String bucketName,
			@PathVariable Optional<Integer[]> bundlesIds,
			@PathVariable Optional<Integer> filterId,
			@PathVariable Optional<Integer[]> tagsIds,
			@RequestParam(required = true) String userName,
			@RequestBody (required = true) LinkedHashMap<String, Object> body) {
		ResponseBody rb = new ResponseBody();
		try {
			int count = databucketService.modifyBundles(userName, bucketName, bundlesIds, filterId, tagsIds, body);		
			rb.setStatus(ResponseStatus.OK);
			rb.setMessage("Number of modified bundles: " + count);
			return new ResponseEntity<ResponseBody>(rb, HttpStatus.OK);
		} catch (ItemDoNotExistsException e1) {
			return customException(rb, e1, HttpStatus.NOT_FOUND);
		} catch (Exception ee) {
			return defaultException(rb, ee);
		}
	}
	
	@PutMapping(value = "/buckets/{bucketName}/bundles/custom", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ResponseBody> modifyBundlesCustom(
			@PathVariable String bucketName,
			@RequestParam(required = true) String userName,
			@RequestBody (required = true) LinkedHashMap<String, Object> body) {
		ResponseBody rb = new ResponseBody();
		try {
			int count = databucketService.modifyBundles(userName, bucketName, Optional.empty(), Optional.empty(), Optional.empty(), body);	
			rb.setStatus(ResponseStatus.OK);
			rb.setMessage("Number of modified bundles: " + count);
			return new ResponseEntity<ResponseBody>(rb, HttpStatus.OK);
		} catch (ItemDoNotExistsException e1) {
			return customException(rb, e1, HttpStatus.NOT_FOUND);
		} catch (Exception ee) {
			return defaultException(rb, ee);
		}
	}
	
	@DeleteMapping(value = {
			"/buckets/{bucketName}/bundles",
			"/buckets/{bucketName}/bundles/{bundlesIds}", 
			"/buckets/{bucketName}/bundles/filters/{filterId}", 
			"/buckets/{bucketName}/bundles/tags/{tagsIds}"}, produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ResponseBody> deleteBundles(
			@PathVariable String bucketName,
			@PathVariable Optional<Integer[]> bundlesIds,
			@PathVariable Optional<Integer> filterId,
			@PathVariable Optional<Integer[]> tagsIds) {
		
		ResponseBody rb = new ResponseBody();
		try {
			int count = databucketService.deleteBundles(bucketName, bundlesIds, filterId, tagsIds);
			rb.setStatus(ResponseStatus.OK);
			rb.setMessage("Number of removed bundles: " + count);
			return new ResponseEntity<ResponseBody>(rb, HttpStatus.OK);
		} catch (ItemDoNotExistsException e) {
			return customException(rb, e, HttpStatus.NOT_FOUND);
		} catch (Exception ee) {
			return defaultException(rb, ee);
		}		
	}
	
	@DeleteMapping(value = {"/buckets/{bucketName}/bundles/custom"}, produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ResponseBody> deleteBundlesCustom(
			@PathVariable String bucketName,
			@RequestBody LinkedHashMap<String, Object> body) {
		
		ResponseBody rb = new ResponseBody();
		try {
			List<Condition> conditions = FieldValidator.validateListOfConditions(body, false);
			int count = databucketService.deleteBundles(bucketName, conditions);
			rb.setStatus(ResponseStatus.OK);
			rb.setMessage("Number of removed bundles: " + count);
			return new ResponseEntity<ResponseBody>(rb, HttpStatus.OK);
		} catch (ItemDoNotExistsException e) {
			return customException(rb, e, HttpStatus.NOT_FOUND);
		} catch (Exception ee) {
			return defaultException(rb, ee);
		}		
	}
	
	//------------------------------------- B U N D L E S  H I S T O R Y --------------------------------------------
	
	// Get bundle history
	@GetMapping(value = {"/buckets/{bucketName}/bundles/{bundleId}/history"}, produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ResponseBody> getBundleHistory(
			@PathVariable("bucketName") String bucketName,
			@PathVariable("bundleId") Integer bundleId) {
		
		ResponseBody rb = new ResponseBody();
		try {
			rb.setHistory(databucketService.getBundleHistory(bucketName, bundleId));			
			rb.setStatus(ResponseStatus.OK);
			return new ResponseEntity<ResponseBody>(rb, HttpStatus.OK);
		} catch (ItemDoNotExistsException e) {
			return customException(rb, e, HttpStatus.NOT_FOUND);
		} catch (Exception ee) {
			return defaultException(rb, ee);
		}			
	}
	
	// Get bundle history parameters
	@GetMapping(value = {"/buckets/{bucketName}/bundles/{bundleId}/history/properties/{ids}"}, produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ResponseBody> getBundleHistoryProperties(
			@PathVariable String bucketName,
			@PathVariable Integer bundleId,
			@PathVariable Integer[] ids) {
		
		ResponseBody rb = new ResponseBody();
		try {
			rb.setHistory(databucketService.getBundleHistoryProperties(bucketName, bundleId, ids));			
			rb.setStatus(ResponseStatus.OK);
			return new ResponseEntity<ResponseBody>(rb, HttpStatus.OK);
		} catch (ItemDoNotExistsException e) {
			return customException(rb, e, HttpStatus.NOT_FOUND);
		} catch (Exception ee) {
			return defaultException(rb, ee);
		}			
	}
	
	//------------------------------------------------ V I E W S ----------------------------------------------------

	@PostMapping(value = "/views", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ResponseBody> createView(
			@RequestParam(required = true) String userName,
			@RequestBody HashMap<String, Object> body) {
		ResponseBody rb = new ResponseBody();
		
		try {
			String viewName = FieldValidator.validateViewName(body, true);			
			Integer bucketId = FieldValidator.validateNullableId(body, COL.BUCKET_ID, false);			
			Integer classId = FieldValidator.validateNullableId(body, COL.CLASS_ID, false);
			Integer columnsId = FieldValidator.validateNullableId(body, COL.COLUMNS_ID, false);
			Integer filterId = FieldValidator.validateNullableId(body, COL.FILTER_ID, false);
			String description = FieldValidator.validateDescription(body, false);
			
			Integer viewId = databucketService.createView(userName, viewName, description, bucketId, classId, columnsId, filterId);
			
			rb.setViewId(viewId);
			rb.setMessage("The new view has been successfully created.");
			rb.setStatus(ResponseStatus.OK);
			return new ResponseEntity<ResponseBody>(rb, HttpStatus.CREATED);
		} catch (ViewAlreadyExistsException | EmptyInputValueException | ExceededMaximumNumberOfCharactersException e2) {
			return customException(rb, e2, HttpStatus.NOT_ACCEPTABLE);
		} catch (Exception ee) {
			return defaultException(rb, ee);
		}
	}
	
	@DeleteMapping(value = "/views/{viewId}", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ResponseBody> deleteViews(
			@PathVariable Integer viewId,
			@RequestParam(required = true) String userName) {
		ResponseBody rb = new ResponseBody();
		try {
			databucketService.deleteView(userName, viewId);
			rb.setStatus(ResponseStatus.OK);
			rb.setMessage("The view has been removed.");
			return new ResponseEntity<ResponseBody>(rb, HttpStatus.OK);
		} catch (Exception ee) {
			return defaultException(rb, ee);
		}		
	}
	
	@PutMapping(value = "/views/{viewId}", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ResponseBody> modifyView(
			@PathVariable("viewId") Integer viewId,
			@RequestParam(required = true) String userName,
			@RequestBody LinkedHashMap<String, Object> body) {
		ResponseBody rb = new ResponseBody();
		try {
			Integer bucketId = FieldValidator.validateNullableId(body, COL.BUCKET_ID, false);
			Integer classId = FieldValidator.validateNullableId(body, COL.CLASS_ID, false);
			Integer columnsId = FieldValidator.validateNullableId(body, COL.COLUMNS_ID, false);
			Integer filterId = FieldValidator.validateNullableId(body, COL.FILTER_ID, false);
			String description = FieldValidator.validateDescription(body, false);
			String viewName = FieldValidator.validateViewName(body, false);
			
			databucketService.modifyView(userName, viewId, viewName, bucketId, classId, description, columnsId, filterId);
			rb.setStatus(ResponseStatus.OK);
			rb.setMessage("View with id '" + viewId + "' has been successfully modified.");
			return new ResponseEntity<ResponseBody>(rb, HttpStatus.OK);
		} catch (ItemDoNotExistsException e) {
			return customException(rb, e, HttpStatus.NOT_FOUND);
		} catch (Exception ee) {
			return defaultException(rb, ee);
		}
	}
	
	@SuppressWarnings("unchecked")
	@GetMapping(value = {"/views", "/views/{viewId}", "/buckets/{bucketName}/views"}, produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ResponseBody> getViews(
			@PathVariable Optional<String> bucketName,
			@PathVariable Optional<Integer> viewId,
			@RequestParam(required = false) Optional<Integer> page,
			@RequestParam(required = false) Optional<Integer> limit,
			@RequestParam(required = false) Optional<String> sort,
			@RequestParam(required = false) Optional<String> filter) {
		
		ResponseBody rb = new ResponseBody();
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
			
			Map<String, Object> result = databucketService.getViews(bucketName, viewId, page, limit, sort, urlConditions);
				
			long total = (long) result.get(C.TOTAL);
			rb.setTotal(total);
						
			if (page.isPresent() && limit.isPresent())
				rb.setTotalPages((int) Math.ceil(total/(float) limit.get()));
				
			rb.setViews((List<Map<String, Object>>) result.get(C.VIEWS));
			
			return new ResponseEntity<ResponseBody>(rb, HttpStatus.OK);
		} catch (ItemDoNotExistsException e) {
			return customException(rb, e, HttpStatus.NOT_FOUND);
		} catch (IncorrectValueException e1) {
			return customException(rb, e1, HttpStatus.NOT_ACCEPTABLE);
		} catch (Exception ee) {
			return defaultException(rb, ee);
		}
	}
	
	
	//----------------------------------------------- T A S K S -----------------------------------------------------
	
	@PostMapping(value = "/tasks", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ResponseBody> createTask(
			@RequestParam(required = true) String userName,
			@RequestBody Map<String, Object> body) {
		ResponseBody rb = new ResponseBody();
		
		try {
			String taskName = FieldValidator.validateTaskName(body, true);
			Map<String, Object> configuration = FieldValidator.validateTaskConfiguration(body, true);
			String description = FieldValidator.validateDescription(body, false);
			Integer bucketId = FieldValidator.validateNullableId(body, COL.BUCKET_ID, false);
			Integer classId = FieldValidator.validateNullableId(body, COL.CLASS_ID, false);			
			
			int taskId = databucketService.createTask(taskName, bucketId, classId, userName, description, configuration);
			rb.setStatus(ResponseStatus.OK);
			rb.setTaskId(taskId);
			rb.setMessage("The new task has been successfully created.");
			return new ResponseEntity<ResponseBody>(rb, HttpStatus.CREATED);
		} catch (ItemDoNotExistsException e) {
			return customException(rb, e, HttpStatus.NOT_FOUND);
		} catch (EmptyInputValueException e1) {
			return customException(rb, e1, HttpStatus.NOT_ACCEPTABLE);			
		} catch (Exception ee) {
			return defaultException(rb, ee);
		}
	}
	
	@DeleteMapping(value = "/tasks/{taskId}", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ResponseBody> deleteTask(
			@PathVariable Integer taskId,
			@RequestParam(required = true) String userName) {
		ResponseBody rb = new ResponseBody();
		try {
			databucketService.deleteTask(userName, taskId);
			rb.setStatus(ResponseStatus.OK);
			rb.setMessage("The task has been removed.");
			return new ResponseEntity<ResponseBody>(rb, HttpStatus.OK);
		} catch (Exception ee) {
			return defaultException(rb, ee);
		}		
	}
	
	@SuppressWarnings("unchecked")
	@GetMapping(value = {
			"/tasks", 
			"/tasks/{taskId}", 
			"/tasks/buckets/{bucketName}"}, produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ResponseBody> getTasks(
			@PathVariable Optional<String> bucketName,
			@PathVariable Optional<Integer> taskId,
			@RequestParam(required = false) Optional<Integer> page,
			@RequestParam(required = false) Optional<Integer> limit,
			@RequestParam(required = false) Optional<String> sort,
			@RequestParam(required = false) Optional<String> filter) {
		
		ResponseBody rb = new ResponseBody();
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
			
			Map<String, Object> result = databucketService.getTasks(bucketName, taskId, page, limit, sort, urlConditions);
				
			long total = (long) result.get(C.TOTAL);
			rb.setTotal(total);
						
			if (page.isPresent() && limit.isPresent())
				rb.setTotalPages((int) Math.ceil(total/(float) limit.get()));
				
			rb.setTasks((List<Map<String, Object>>) result.get(C.TASKS));
			
			return new ResponseEntity<ResponseBody>(rb, HttpStatus.OK);
		} catch (ItemDoNotExistsException e) {
			return customException(rb, e, HttpStatus.NOT_FOUND);
		} catch (IncorrectValueException e2) {
			return customException(rb, e2, HttpStatus.NOT_ACCEPTABLE);
		} catch (Exception ee) {
			return defaultException(rb, ee);
		}
	}
	
	@PutMapping(value = "/tasks/{taskId}", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ResponseBody> modifyTask(
			@PathVariable("taskId") Integer taskId,
			@RequestParam(required = true) String userName,
			@RequestBody LinkedHashMap<String, Object> body) {
		ResponseBody rb = new ResponseBody();
		try {
			Integer bucketId = FieldValidator.validateNullableId(body, COL.BUCKET_ID, false);
			Integer classId = FieldValidator.validateNullableId(body, COL.CLASS_ID, false);
			String taskName = FieldValidator.validateTaskName(body, false);
			String description = FieldValidator.validateDescription(body, false);
			LinkedHashMap<String, Object> configuration = FieldValidator.validateTaskConfiguration(body, false);
			
			databucketService.modifyTask(userName, taskId, taskName, bucketId, classId, description, configuration);
			rb.setStatus(ResponseStatus.OK);
			rb.setMessage("Task with id '" + taskId + "' has been successfully modified.");
			return new ResponseEntity<ResponseBody>(rb, HttpStatus.OK);
		} catch (ItemDoNotExistsException e1) {
			return customException(rb, e1, HttpStatus.NOT_FOUND);
		} catch (EmptyInputValueException | ExceededMaximumNumberOfCharactersException e2) {
			return customException(rb, e2, HttpStatus.NOT_ACCEPTABLE);
		} catch (Exception ee) {
			return defaultException(rb, ee);
		}
	}
	
	//---------------------------------------------- E V E N T S ----------------------------------------------------
	
	@PostMapping(value = "/events", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ResponseBody> createEvent(
			@RequestParam(required = true) String userName,
			@RequestBody Map<String, Object> body) {
		ResponseBody rb = new ResponseBody();
		
		try {
			String eventName = FieldValidator.validateEventName(body, true);	
			Boolean active = FieldValidator.validateEventStatus(body, false);
			Map<String, Object> schedule = FieldValidator.validateEventSchedule(body, true, active);
			List<Map<String, Object>> tasks = FieldValidator.validateEventTasks(body, true);			
			String description = FieldValidator.validateDescription(body, false);
			Integer bucketId = FieldValidator.validateNullableId(body, COL.BUCKET_ID, false);
			Integer classId = FieldValidator.validateNullableId(body, COL.CLASS_ID, false);			
			
			int eventId = databucketService.createEvent(eventName, bucketId, classId, userName, description, schedule, tasks, active);
			rb.setStatus(ResponseStatus.OK);
			rb.setEventId(eventId);
			rb.setMessage("The new event has been successfully created.");
			return new ResponseEntity<ResponseBody>(rb, HttpStatus.CREATED);
		} catch (ItemDoNotExistsException e) {
			return customException(rb, e, HttpStatus.NOT_FOUND);
		} catch (EmptyInputValueException e1) {
			return customException(rb, e1, HttpStatus.NOT_ACCEPTABLE);			
		} catch (Exception ee) {
			return defaultException(rb, ee);
		}
	}
	
	@DeleteMapping(value = "/events/{eventId}", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ResponseBody> deleteEvent(
			@PathVariable Integer eventId,
			@RequestParam(required = true) String userName) {
		ResponseBody rb = new ResponseBody();
		try {
			databucketService.deleteEvent(userName, eventId);
			rb.setStatus(ResponseStatus.OK);
			rb.setMessage("The event has been removed.");
			return new ResponseEntity<ResponseBody>(rb, HttpStatus.OK);
		} catch (Exception ee) {
			return defaultException(rb, ee);
		}		
	}
	
	@SuppressWarnings("unchecked")
	@GetMapping(value = {
			"/events", 
			"/events/{eventId}", 
			"/events/buckets/{bucketName}"}, produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ResponseBody> getEvents(
			@PathVariable Optional<String> bucketName,
			@PathVariable Optional<Integer> eventId,
			@RequestParam(required = false) Optional<Integer> page,
			@RequestParam(required = false) Optional<Integer> limit,
			@RequestParam(required = false) Optional<String> sort,
			@RequestParam(required = false) Optional<String> filter) {
		
		ResponseBody rb = new ResponseBody();
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
			
			Map<String, Object> result = databucketService.getEvents(bucketName, eventId, page, limit, sort, urlConditions);
				
			long total = (long) result.get(C.TOTAL);
			rb.setTotal(total);
						
			if (page.isPresent() && limit.isPresent())
				rb.setTotalPages((int) Math.ceil(total/(float) limit.get()));
				
			rb.setEvents((List<Map<String, Object>>) result.get(C.EVENTS));
			
			return new ResponseEntity<ResponseBody>(rb, HttpStatus.OK);
		} catch (ItemDoNotExistsException e) {
			return customException(rb, e, HttpStatus.NOT_FOUND);
		} catch (IncorrectValueException e2) {
			return customException(rb, e2, HttpStatus.NOT_ACCEPTABLE);
		} catch (Exception ee) {
			return defaultException(rb, ee);
		}
	}
	
	@PutMapping(value = "/events/{eventId}", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ResponseBody> modifyEvent(
			@PathVariable("eventId") Integer eventId,
			@RequestParam(required = true) String userName,
			@RequestBody LinkedHashMap<String, Object> body) {
		ResponseBody rb = new ResponseBody();
		try {
			Integer bucketId = FieldValidator.validateNullableId(body, COL.BUCKET_ID, false);
			Integer classId = FieldValidator.validateNullableId(body, COL.CLASS_ID, false);
			String eventName = FieldValidator.validateEventName(body, false);
			String description = FieldValidator.validateDescription(body, false);
			List<Map<String, Object>> tasks = FieldValidator.validateEventTasks(body, false);	
			Boolean active = FieldValidator.validateEventStatus(body, false);
			Map<String, Object> schedule = FieldValidator.validateEventSchedule(body, false, active);
			
			databucketService.modifyEvent(userName, eventId, eventName, bucketId, classId, description, schedule, tasks, active);
			rb.setStatus(ResponseStatus.OK);
			rb.setMessage("Event with id '" + eventId + "' has been successfully modified.");
			return new ResponseEntity<ResponseBody>(rb, HttpStatus.OK);
		} catch (ItemDoNotExistsException e1) {
			return customException(rb, e1, HttpStatus.NOT_FOUND);
		} catch (EmptyInputValueException | IncorrectValueException | ExceededMaximumNumberOfCharactersException e2) {
			return customException(rb, e2, HttpStatus.NOT_ACCEPTABLE);
		} catch (Exception ee) {
			return defaultException(rb, ee);
		}
	}
	
	//------------------------------------------ E V E N T S   L O G ------------------------------------------------
	
	@SuppressWarnings("unchecked")
	@GetMapping(value = "/events/log", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ResponseBody> getEventsLog(
			@RequestParam(required = false) Optional<Integer> page,
			@RequestParam(required = false) Optional<Integer> limit,
			@RequestParam(required = false) Optional<String> sort,
			@RequestParam(required = false) Optional<String> filter) {
		
		ResponseBody rb = new ResponseBody();
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
			
			Map<String, Object> result = databucketService.getEventsLog(page, limit, sort, urlConditions);
				
			long total = (long) result.get(C.TOTAL);
			rb.setTotal(total);
						
			if (page.isPresent() && limit.isPresent())
				rb.setTotalPages((int) Math.ceil(total/(float) limit.get()));
				
			rb.setEventsLog((List<Map<String, Object>>) result.get(C.EVENTS_LOG));
			
			return new ResponseEntity<ResponseBody>(rb, HttpStatus.OK);

		} catch (Exception ee) {
			return defaultException(rb, ee);
		}
	}
	
	@DeleteMapping(value = "/events/log", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ResponseBody> clearEventsLog() {
		ResponseBody rb = new ResponseBody();
		
		try {
			databucketService.clearEventsLog();
			rb.setStatus(ResponseStatus.OK);
			rb.setMessage("The events log has been cleaned.");
			return new ResponseEntity<ResponseBody>(rb, HttpStatus.OK);
		} catch (Exception ee) {
			return defaultException(rb, ee);
		}		
	}
	
	
	//------------------------------------------ E X C E P T I O N S ------------------------------------------------
	
	private ResponseEntity<ResponseBody> customException(ResponseBody rb, Exception e, HttpStatus status) {
		logger.warn(e.getMessage());
		rb.setStatus(ResponseStatus.FAILED);
		rb.setMessage(e.getMessage());
		return new ResponseEntity<ResponseBody>(rb, status);
	}
	
	private ResponseEntity<ResponseBody> defaultException(ResponseBody rb, Exception e) {
		logger.error("ERROR:", e);
		rb.setStatus(ResponseStatus.FAILED);
		rb.setMessage(e.getMessage());
		return new ResponseEntity<ResponseBody>(rb, HttpStatus.INTERNAL_SERVER_ERROR);
	}
}
