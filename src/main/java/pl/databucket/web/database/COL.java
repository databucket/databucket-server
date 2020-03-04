package pl.databucket.web.database;

public class COL {
	
	public static final String COUNT = "count(*)";
	
	// Common columns
	public static final String INDEX = "index";
	public static final String DESCRIPTION = "description";
	public static final String ICON_NAME = "icon_name";
	public static final String CREATED_AT = "created_at";
	public static final String CREATED_BY = "created_by";
	public static final String UPDATED_AT = "updated_at";
	public static final String UPDATED_BY = "updated_by";
	
	// Table _group
	public static final String GROUP_ID = "group_id";
	public static final String GROUP_NAME = "group_name";
	public static final String BUCKETS = "buckets";
	
	// Talbe _class
	public static final String CLASS_ID = "class_id";
	public static final String CLASS_NAME = "class_name";
	
	// Table _bucket
	public static final String BUCKET_ID = "bucket_id";
	public static final String BUCKET_NAME = "bucket_name";
	public static final String DELETED = "deleted";
	public static final String HISTORY = "history";
	
	// Table _columns
	public static final String COLUMNS_ID = "columns_id";
	public static final String COLUMNS_NAME = "columns_name";
	public static final String COLUMNS = "columns";
	
	// Table _filter
	public static final String FILTER_ID = "filter_id";
	public static final String FILTER_NAME = "filter_name";
	public static final String CONDITIONS = "conditions";
	
	// Table _tag
	public static final String TAG_ID = "tag_id";
	public static final String TAG_NAME = "tag_name";
	
	// Table _view
	public static final String VIEW_ID = "view_id";
	public static final String VIEW_NAME = "view_name";
	
	// Table bucket
	public static final String BUNDLE_ID = "bundle_id";
	public static final String LOCKED = "locked";
	public static final String LOCKED_BY = "locked_by";
	public static final String PROPERTIES = "properties";
	
	// Table _task
	public static final String TASK_ID = "task_id";
	public static final String TASK_NAME = "task_name";
	public static final String CONFIGURATION = "configuration";
	
	// Table _event
	public static final String EVENT_ID = "event_id";
	public static final String EVENT_NAME = "event_name";
	public static final String SCHEDULE = "schedule";
	public static final String TASKS = "tasks";
	public static final String ACTIVE = "active";
		
	// Table _event_log
	public static final String EVENT_LOG_ID = "event_log_id";
	public static final String AFFECTED = "affected";
	
	// Table history
	public static final String ID = "id";
}
