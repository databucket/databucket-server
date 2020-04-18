package pl.databucket.service;

import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.dao.DataAccessException;
import org.springframework.dao.InvalidDataAccessApiUsageException;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;

import pl.databucket.exception.*;
import pl.databucket.database.Condition;

public interface DatabucketService {
	
	// ---- GROUPS ----
	int createGroup(String userName, String groupName, String description, ArrayList<Integer> buckets) throws GroupAlreadyExistsException, ExceededMaximumNumberOfCharactersException, EmptyInputValueException, Exception;
	int deleteGroup(Integer groupId, String userName) throws ItemDoNotExistsException, UnknownColumnException;
	Map<String, Object> getGroups(Optional<Integer> groupId, Optional<Integer> page, Optional<Integer> limit, Optional<String> sort, List<Condition> urlConditions) throws UnknownColumnException, DataAccessException, UnexpectedException;
	void modifyGroup(String userName, Integer groupId, LinkedHashMap<String, Object> body)  throws ItemDoNotExistsException, GroupAlreadyExistsException, JsonProcessingException, IncorrectValueException, ExceededMaximumNumberOfCharactersException, DataAccessException, UnknownColumnException;
	
	// ---- CLASSES ----
	int createClass(String userName, String className, String description) throws ClassAlreadyExistsException, ExceededMaximumNumberOfCharactersException, EmptyInputValueException, Exception;
	int deleteClass(Integer classId, String userName) throws ItemDoNotExistsException, UnknownColumnException, ItemAlreadyUsedException;
	Map<String, Object> getClasses(Optional<Integer> classId, Optional<Integer> page, Optional<Integer> limit, Optional<String> sort, List<Condition> urlConditions) throws UnknownColumnException, DataAccessException, UnexpectedException;
	void modifyClass(String userName, Integer classId, LinkedHashMap<String, Object> body)  throws ItemDoNotExistsException, ClassAlreadyExistsException, JsonProcessingException, IncorrectValueException, ExceededMaximumNumberOfCharactersException, DataAccessException, UnknownColumnException;
		
	// ---- BUCKETS ----
	Map<String, Object> getBuckets(Optional<String> bucketName, Optional<Integer> page, Optional<Integer> limit, Optional<String> sort, List<Condition> urlConditions) throws UnknownColumnException, DataAccessException;
	List<Map<String, Object>> getStatistic(String bucketName) throws ItemDoNotExistsException, UnexpectedException, UnknownColumnException;
	int createBucket(String createdBy, String bucketName, int index, String description, String icon, boolean history, Integer classId) throws BucketAlreadyExistsException, ExceededMaximumNumberOfCharactersException, EmptyInputValueException, Exception;
	void deleteBucket(String bucketName, String userName) throws ItemDoNotExistsException, UnknownColumnException, ItemAlreadyUsedException;
	void modifyBucket(String updatedBy, String bucketName, Map<String, Object> details) throws ItemDoNotExistsException, BucketAlreadyExistsException, JsonProcessingException, IncorrectValueException, ExceededMaximumNumberOfCharactersException, DataAccessException, UnknownColumnException;
	
	// ---- TAGS ----
	Map<String, Object> getTags(Optional<String> bucketName, Optional<Integer> tagId, Optional<Integer> page, Optional<Integer> limit, Optional<String> sort, List<Condition> urlConditions) throws ItemDoNotExistsException, UnknownColumnException;
	int createTag(String createdBy, String tagName, Integer bucketId, String bucketName, String iconName, String description, Integer classId) throws ItemDoNotExistsException, JsonProcessingException, TagAlreadyExistsException, InvalidDataAccessApiUsageException, DataAccessException, UnknownColumnException;
	int deleteTag(String userName, int tagId) throws UnknownColumnException, ItemDoNotExistsException, ItemAlreadyUsedException;
	void modifyTag(String updatedBy, Integer tagId, String tagName, Integer bucketId, Integer classId, String description) throws TagAlreadyExistsException, JsonProcessingException, ItemDoNotExistsException, IncorrectValueException, ExceededMaximumNumberOfCharactersException, UnknownColumnException;
	
	// ---- COLUMNS ----
	Map<String, Object> getColumns(Optional<String> bucketName, Optional<Integer> columnsId, Optional<Integer> page, Optional<Integer> limit, Optional<String> sort, List<Condition> urlConditions) throws ItemDoNotExistsException, UnexpectedException, UnknownColumnException;
	int createColumns(String columnsName, Integer bucketId, String createdBy, List<Map<String, Object>> columns, String description, Integer classId) throws ItemDoNotExistsException, JsonProcessingException, ColumnsAlreadyExistsException, UnknownColumnException;
	int deleteColumns(String userName, int columnsId) throws ItemDoNotExistsException, UnknownColumnException, ItemAlreadyUsedException;
	void modifyColumns(String updatedBy, Integer columnsId, String columnsName, Integer bucketId, Integer classId, String description, List<Map<String, Object>> columns) throws ItemDoNotExistsException, ExceededMaximumNumberOfCharactersException, UnexpectedException, ColumnsAlreadyExistsException, UnknownColumnException;
	
	// ---- FILTERS ----
	Map<String, Object> getFilters(Optional<String> bucketName, Optional<Integer> filterId, Optional<Integer> page, Optional<Integer> limit, Optional<String> sort, List<Condition> urlConditions) throws ItemDoNotExistsException, UnexpectedException, UnknownColumnException;
	int createFilter(String filterName, Integer bucketId, String createdBy, List<Map<String, Object>> conditions, String description, Integer classId) throws ItemDoNotExistsException, JsonProcessingException, FilterAlreadyExistsException, UnknownColumnException;
	int deleteFilter(String userName, int filterId) throws ItemDoNotExistsException, UnknownColumnException, ItemAlreadyUsedException;
	void modifyFilter(String updatedBy, Integer filterId, String filterName, Integer bucketId, Integer classId, String description, List<Map<String, Object>> conditions) throws ItemDoNotExistsException, ExceededMaximumNumberOfCharactersException, UnexpectedException, FilterAlreadyExistsException, UnknownColumnException;
	
	// ---- BUNDLES ----
	List<Integer> lockBundles(String bucketName, String userName, Optional<Integer[]> tagId, Optional<Integer> filterId, Optional<List<Condition>> conditions, Optional<Integer> page, Optional<Integer> limit, Optional<String> sort) throws JsonParseException, JsonMappingException, IOException, ItemDoNotExistsException, UnexpectedException, UnknownColumnException;
	Map<String, Object> getBundles(String bucketName, Optional<Integer[]> bundleId, Optional<Integer[]> tagId, Optional<Integer> filterId, Optional<Integer> viewId, Optional<List<Map<String, Object>>> inColumns, Optional<List<Condition>> inConditions, Optional<Integer> page, Optional<Integer> limit, Optional<String> sort) throws IncorrectValueException, ItemDoNotExistsException, UnexpectedException, UnknownColumnException, JsonParseException, JsonMappingException, IOException;
	int createBundle(String createdBy, String bucketName, Map<String, Object> details) throws JsonProcessingException, ItemDoNotExistsException, UnexpectedException, ItemDoNotExistsException, UnknownColumnException;
	int deleteBundles(String bucketName, Optional<Integer[]> bundlesIds, Optional<Integer> filterId, Optional<Integer[]> tagsIds) throws JsonParseException, JsonMappingException, IOException, ItemDoNotExistsException, UnexpectedException, UnknownColumnException;
	int deleteBundles(String bucketName, List<Condition> conditions) throws ItemDoNotExistsException, UnknownColumnException, UnexpectedException;
	int modifyBundles(String updatedBy, String bucketName, Optional<Integer[]> bundlesIds, Optional<Integer> filterId, Optional<Integer[]> tagsIds, LinkedHashMap<String, Object> details) throws IOException, ItemDoNotExistsException, UnexpectedException, ItemDoNotExistsException, UnknownColumnException;
	
	// ---- VIEW ----
	int createView(String userName, String viewName, String description, Integer bucketId, Integer classId, Integer columnsId, Integer filterId) throws UnexpectedException, ViewAlreadyExistsException, UnknownColumnException, JsonProcessingException;
	int deleteView(String userName, int viewId) throws UnknownColumnException;
	Map<String, Object> getViews(Optional<String> bucketName, Optional<Integer> viewId, Optional<Integer> page, Optional<Integer> limit, Optional<String> sort, List<Condition> urlConditions) throws JsonParseException, JsonMappingException, IOException, ItemDoNotExistsException, UnexpectedException, UnknownColumnException;
	void modifyView(String updatedBy, Integer viewId, String viewName, Integer bucketId, Integer classId, String description, Integer columnsId, Integer filterId) throws HarmlessException, JsonProcessingException, ItemDoNotExistsException, ItemDoNotExistsException, UnknownColumnException, ItemDoNotExistsException;
	
	List<Map<String, Object>> getBundleHistory(String bucketName, Integer bundleId) throws ItemDoNotExistsException, UnexpectedException, UnknownColumnException;
	List<Map<String, Object>> getBundleHistoryProperties(String bucketName, Integer bundleId, Integer[] ids) throws JsonParseException, JsonMappingException, IOException, ItemDoNotExistsException, UnexpectedException, UnknownColumnException;
		
	// ---- TASK ----
	int createTask(String taskName, Integer bucketId, Integer classId, String userName, String description,	Map<String, Object> configuration) throws ItemDoNotExistsException, JsonProcessingException, UnknownColumnException;
	int deleteTask(String userName, int taskId) throws ItemDoNotExistsException, UnknownColumnException, ItemAlreadyUsedException;
	Map<String, Object> getTasks(Optional<String> bucketName, Optional<Integer> taskId, Optional<Integer> page, Optional<Integer> limit, Optional<String> sort, List<Condition> urlConditions) throws ItemDoNotExistsException, UnexpectedException, UnknownColumnException;
	void modifyTask(String userName, Integer taskId, String taskName, Integer bucketId, Integer classId, String description, LinkedHashMap<String, Object> configuration) throws ItemDoNotExistsException, ExceededMaximumNumberOfCharactersException, UnexpectedException, UnknownColumnException;
	
	// ---- EVENT ----
	int createEvent(String eventName, Integer bucketId, Integer classId, String userName, String description, Map<String, Object> schedule, List<Map<String, Object>> tasks, Boolean active) throws JsonProcessingException, InvalidDataAccessApiUsageException, DataAccessException, UnknownColumnException, ItemAlreadyExistsException, ParseException, UnexpectedException, EmptyInputValueException;
	int deleteEvent(String userName, Integer eventId) throws DataAccessException, UnknownColumnException, ItemDoNotExistsException;
	Map<String, Object> getEvents(Optional<String> bucketName, Optional<Integer> eventId, Optional<Integer> page, Optional<Integer> limit, Optional<String> sort, List<Condition> urlConditions) throws ItemDoNotExistsException, UnknownColumnException, UnexpectedException;
	void modifyEvent(String userName, Integer eventId, String eventName, Integer bucketId, Integer classId,	String description, Map<String, Object> schedule, List<Map<String, Object>> tasks, Boolean active) throws UnexpectedException, UnknownColumnException, ItemDoNotExistsException, ItemAlreadyExistsException, ParseException, EmptyInputValueException;
	
	// ---- EVENT LOG ----
	Map<String, Object> getEventsLog(Optional<Integer> page, Optional<Integer> limit, Optional<String> sort, List<Condition> urlConditions) throws UnknownColumnException;
	void clearEventsLog();
				
}
