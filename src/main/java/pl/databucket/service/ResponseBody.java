package pl.databucket.service;

import java.util.Map;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({"status", "message", "page", "limit", "total", "total_pages", "search", "sort", "bundles", "groups", "tasks"})
public class ResponseBody {
	
	private ResponseStatus status = null;
	private String title = null;
	private String version = null;
	private String message = null;
	private Integer page = null;
	private Integer limit = null;
	@JsonProperty("total_pages")
	private Integer totalPages = null;
	private Long total = null;
	private String sort = null;
	private String search = null;
	@JsonProperty("tag_id")
	private Integer tagId = null;
	private List<Map<String, Object>> tags = null;
	@JsonProperty("group_id")
	private Integer groupId = null;
	@JsonProperty("class_id")
	private Integer classId = null;
	@JsonProperty("bucket_id")
	private Integer bucketId = null;
	@JsonProperty("bundle_id")
	private Integer bundleId = null;
	@JsonProperty("bundles_ids")
	private Integer[] bundlesIds = null;
	@JsonProperty("columns_id")
	private Integer columnsId = null;
	@JsonProperty("filter_id")
	private Integer filterId = null;
	@JsonProperty("task_id")
	private Integer taskId = null;
	@JsonProperty("event_id")
	private Integer eventId = null;
	@JsonProperty("view_id")
	private Integer viewId = null;
	
	private List<Map<String, Object>> groups = null;
	private List<Map<String, Object>> classes = null;
	private List<Map<String, Object>> buckets = null;
	private List<Map<String, Object>> tasks = null;
	private List<Map<String, Object>> events = null;
	private List<Map<String, Object>> events_history = null;
	private List<Map<String, Object>> statistic = null;
	private List<Map<String, Object>> columns = null;
	private List<Map<String, Object>> filters = null;	
	private List<Map<String, Object>> views = null;
	private List<Map<String, Object>> history = null;
	private List<Map<String, Object>> bundles = null;
	
	@JsonProperty("events_log")
	private List<Map<String, Object>> eventsLog = null;
	
	public ResponseStatus getStatus() {
		return status;
	}
	public void setStatus(ResponseStatus status) {
		this.status = status;
	}
	public Long getTotal() {
		return total;
	}
	public void setTotal(Long total) {
		this.total = total;
	}
	public Integer getPage() {
		return page;
	}
	public void setPage(Integer page) {
		this.page = page;
	}	
	public String getSort() {
		return sort;
	}
	public void setSort(String sort) {
		this.sort = sort;
	}
	public Integer getLimit() {
		return limit;
	}
	public void setLimit(Integer limit) {
		this.limit = limit;
	}
	public Integer getTotalPages() {
		return totalPages;
	}
	public void setTotalPages(Integer totalPages) {
		this.totalPages = totalPages;
	}
	public String getMessage() {
		return message;
	}
	public void setMessage(String message) {
		this.message = message;
	}
	public List<Map<String, Object>> getBuckets() {
		return buckets;
	}
	public void setBuckets(List<Map<String, Object>> buckets) {
		this.buckets = buckets;
	}
	public List<Map<String, Object>> getStatistic() {
		return statistic;
	}
	public void setStatistic(List<Map<String, Object>> statistic) {
		this.statistic = statistic;
	}
	public List<Map<String, Object>> getTags() {
		return tags;
	}
	public void setTags(List<Map<String, Object>> tags) {
		this.tags = tags;
	}
	public List<Map<String, Object>> getColumns() {
		return columns;
	}
	public void setColumns(List<Map<String, Object>> columns) {
		this.columns = columns;
	}
	public List<Map<String, Object>> getFilters() {
		return filters;
	}
	public void setFilters(List<Map<String, Object>> filters) {
		this.filters = filters;
	}
	public List<Map<String, Object>> getViews() {
		return views;
	}
	public void setViews(List<Map<String, Object>> views) {
		this.views = views;
	}
	public Integer getTagId() {
		return tagId;
	}
	public void setTagId(Integer tagId) {
		this.tagId = tagId;
	}
	public Integer getBucketId() {
		return bucketId;
	}
	public void setBucketId(Integer bucketId) {
		this.bucketId = bucketId;
	}
	public Integer getBundleId() {
		return bundleId;
	}
	public void setBundleId(Integer bundleId) {
		this.bundleId = bundleId;
	}
	public Integer[] getBundlesIds() {
		return bundlesIds;
	}
	public void setBundlesIds(Integer[] bundlesIds) {
		this.bundlesIds = bundlesIds;
	}
	public Integer getColumnsId() {
		return columnsId;
	}
	public void setColumnsId(Integer columnsId) {
		this.columnsId = columnsId;
	}
	public Integer getFilterId() {
		return filterId;
	}
	public void setFilterId(Integer filterId) {
		this.filterId = filterId;
	}
	public Integer getViewId() {
		return viewId;
	}
	public void setViewId(Integer viewId) {
		this.viewId = viewId;
	}
	public List<Map<String, Object>> getHistory() {
		return history;
	}
	public void setHistory(List<Map<String, Object>> history) {
		this.history = history;
	}
	public List<Map<String, Object>> getBundles() {
		return bundles;
	}
	public void setBundles(List<Map<String, Object>> bundles) {
		this.bundles = bundles;
	}
	public String getSearch() {
		return search;
	}
	public void setSearch(String search) {
		this.search = search;
	}
	public String getTitle() {
		return title;
	}
	public void setTitle(String title) {
		this.title = title;
	}
	public List<Map<String, Object>> getGroups() {
		return groups;
	}
	public void setGroups(List<Map<String, Object>> groups) {
		this.groups = groups;
	}
	public Integer getGroupId() {
		return groupId;
	}
	public void setGroupId(Integer groupId) {
		this.groupId = groupId;
	}
	public Integer getClassId() {
		return classId;
	}
	public void setClassId(Integer classId) {
		this.classId = classId;
	}
	public Integer getTaskId() {
		return taskId;
	}
	public void setTaskId(Integer taskId) {
		this.taskId = taskId;
	}
	public List<Map<String, Object>> getTasks() {
		return tasks;
	}
	public void setTasks(List<Map<String, Object>> tasks) {
		this.tasks = tasks;
	}
	public List<Map<String, Object>> getClasses() {
		return classes;
	}
	public void setClasses(List<Map<String, Object>> classes) {
		this.classes = classes;
	}
	public String getVersion() {
		return version;
	}
	public void setVersion(String version) {
		this.version = version;
	}
	public List<Map<String, Object>> getEvents() {
		return events;
	}
	public void setEvents(List<Map<String, Object>> events) {
		this.events = events;
	}
	public List<Map<String, Object>> getEvents_history() {
		return events_history;
	}
	public void setEvents_history(List<Map<String, Object>> events_history) {
		this.events_history = events_history;
	}
	public Integer getEventId() {
		return eventId;
	}
	public void setEventId(Integer eventId) {
		this.eventId = eventId;
	}
	public List<Map<String, Object>> getEventsLog() {
		return eventsLog;
	}
	public void setEventsLog(List<Map<String, Object>> eventsLog) {
		this.eventsLog = eventsLog;
	}
	
	
}
