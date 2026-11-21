package pl.databucket.server.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import pl.databucket.server.configuration.Constants;
import pl.databucket.server.dto.DataClassItemDto;
import pl.databucket.server.dto.TaskRunDTO;
import pl.databucket.server.dto.TaskRunResponse;
import pl.databucket.server.entity.Bucket;
import pl.databucket.server.entity.DataClass;
import pl.databucket.server.entity.DataFilter;
import pl.databucket.server.entity.Task;
import pl.databucket.server.entity.User;
import pl.databucket.server.exception.BucketNotFoundException;
import pl.databucket.server.exception.InvalidRuleException;
import pl.databucket.server.logging.AuditLog;
import pl.databucket.server.repository.TaskRepository;
import pl.databucket.server.service.data.DataService;
import pl.databucket.server.service.data.ResultField;
import pl.databucket.server.service.data.SearchRules;
import pl.databucket.server.service.data.filter.LogicFilter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Runs a saved task on the data of a bucket, on the server: the same as the "Run task" dialog of the web application
 * does with the data API (see {@link TaskAction}). The rows are those matching the filter of the task and the rules of
 * the request together; with neither the task is refused, so it never runs on the whole bucket by mistake.
 */
@Service
public class TaskRunService {

    private final TaskRepository taskRepository;
    private final BucketService bucketService;
    private final UserService userService;
    private final DataService dataService;
    private final TransactionTemplate readOnly;

    public TaskRunService(TaskRepository taskRepository, BucketService bucketService, UserService userService,
                          DataService dataService, PlatformTransactionManager transactionManager) {
        this.taskRepository = taskRepository;
        this.bucketService = bucketService;
        this.userService = userService;
        this.dataService = dataService;
        this.readOnly = new TransactionTemplate(transactionManager);
        this.readOnly.setReadOnly(true);
    }

    /** The action of the task needs a higher role than the user's (removing and clearing history: ROBOT). */
    public static class ForbiddenTaskException extends Exception {
        public ForbiddenTaskException(String message) {
            super(message);
        }
    }

    /** No such task among the tasks of the bucket. */
    public static class TaskNotFoundException extends Exception {
        public TaskNotFoundException(String message) {
            super(message);
        }
    }

    // what is needed to run the task, read in one transaction: the access checks compare entities by identity
    private record Plan(User user, Bucket bucket, long taskId, String taskName, TaskAction action,
                        Map<String, Object> filterLogic) {
    }

    private static class Failure extends RuntimeException {
        Failure(Exception cause) {
            super(cause);
        }
    }

    /**
     * @param taskRef the task id, or (from the MCP server) its name
     * @param rules   rules narrowing the rows (may be null)
     * @param preview true: change nothing, count the rows the task would run on
     */
    public TaskRunResponse run(String bucketName, String taskRef, SearchRules rules, boolean preview) throws Exception {
        Plan plan = plan(bucketName, taskRef);
        TaskAction action = plan.action();
        User user = plan.user();
        Bucket bucket = plan.bucket();

        if ((action.type() == TaskAction.Type.REMOVE || action.type() == TaskAction.Type.CLEAR_HISTORY)
                && !user.hasRoleAtLeast(Constants.ROLE_ROBOT))
            throw new ForbiddenTaskException("The task " + (action.type() == TaskAction.Type.REMOVE
                    ? "removes data rows" : "clears the history") + ", which needs the ROBOT role or higher");
        if (action.type() == TaskAction.Type.CLEAR_HISTORY && !bucket.isHistory())
            throw new TaskAction.InvalidTaskException("The bucket '" + bucket.getName() + "' keeps no history");
        if (filter(user, plan.filterLogic(), rules).isEmpty())
            throw new TaskAction.InvalidTaskException("The task has no filter: give rules selecting the rows");

        String type = action.type().value();
        if (preview) {
            if (action.type() == TaskAction.Type.RESERVE) {
                long available = dataService.countFreeData(bucket, filter(user, plan.filterLogic(), rules));
                return new TaskRunResponse(plan.taskId(), plan.taskName(), type, true, Math.min(available, action.number()),
                        available, "Would reserve " + Math.min(available, action.number()) + " of " + available
                        + " free data row(s)");
            }
            Map<ResultField, Object> result = dataService.getData(user, bucket, Optional.empty(),
                    filter(user, plan.filterLogic(), rules), 1, 0, "id", true);
            long total = ((Number) result.get(ResultField.TOTAL)).longValue();
            return new TaskRunResponse(plan.taskId(), plan.taskName(), type, true, total, null,
                    "The task would run on " + total + " data row(s)");
        }

        TaskRunResponse response = switch (action.type()) {
            case MODIFY -> {
                int count = dataService.modifyData(user, bucket, Optional.empty(), action.changes(),
                        filter(user, plan.filterLogic(), rules));
                yield new TaskRunResponse(plan.taskId(), plan.taskName(), type, false, count, null,
                        "Modified " + count + " data row(s)");
            }
            case REMOVE -> {
                int count = dataService.deleteDataByRules(user, bucket, filter(user, plan.filterLogic(), rules));
                yield new TaskRunResponse(plan.taskId(), plan.taskName(), type, false, count, null,
                        "Removed " + count + " data row(s)");
            }
            case CLEAR_HISTORY -> {
                int count = dataService.clearDataHistory(user, bucket, filter(user, plan.filterLogic(), rules));
                yield new TaskRunResponse(plan.taskId(), plan.taskName(), type, false, count, null,
                        "Cleared the history of " + count + " data row(s)");
            }
            case RESERVE -> {
                // as the reservation of the API: an admin may reserve for someone else
                String owner = user.isAdminUser() && action.username() != null ? action.username() : user.getUsername();
                List<Long> ids = dataService.reserveData(user, bucket, filter(user, plan.filterLogic(), rules),
                        action.number(), action.random() ? "random" : "id", owner);
                long available = dataService.countFreeData(bucket, filter(user, plan.filterLogic(), rules));
                yield new TaskRunResponse(plan.taskId(), plan.taskName(), type, false, ids.size(), available,
                        "Reserved " + ids.size() + " data row(s)" + (owner.equals(user.getUsername()) ? "" : " for " + owner));
            }
        };
        AuditLog.log("task.run", "id={} name={} bucket={} action={} count={}", plan.taskId(), plan.taskName(),
                bucket.getName(), type, response.count());
        return response;
    }

    private Plan plan(String bucketName, String taskRef) throws Exception {
        try {
            return readOnly.execute(status -> {
                try {
                    return readPlan(bucketName, taskRef);
                } catch (Exception e) {
                    throw new Failure(e);
                }
            });
        } catch (Failure e) {
            throw (Exception) e.getCause();
        }
    }

    private Plan readPlan(String bucketName, String taskRef) throws Exception {
        User user = userService.getCurrentUser();
        Bucket bucket = bucketService.getBucket(bucketName);
        if (bucket == null || !bucketService.hasUserAccessToBucket(bucket, user))
            throw new BucketNotFoundException(bucketName);

        Task task = findTask(bucket, taskRef);
        DataClass dataClass = bucket.getDataClass();
        List<DataClassItemDto> classProperties = dataClass != null ? dataClass.getConfiguration() : null;
        DataFilter filter = task.getDataFilter() != null && !Boolean.TRUE.equals(task.getDataFilter().getDeleted()) ? task.getDataFilter() : null;
        TaskAction action = TaskAction.of(task.getConfiguration(), classProperties,
                filter != null && filter.getConfiguration() != null ? filter.getConfiguration().getProperties() : null);
        Map<String, Object> filterLogic = filter != null && filter.getConfiguration() != null
                ? filter.getConfiguration().getLogic() : null;
        return new Plan(user, bucket, task.getId(), task.getName(), action, filterLogic);
    }

    // the task by id (or by name, if unique), one of the tasks of the bucket: those of the bucket or of its class,
    // as in the bucket's data table
    private Task findTask(Bucket bucket, String taskRef) throws TaskNotFoundException, TaskAction.InvalidTaskException {
        List<Task> tasks = tasksOf(bucket);
        if (taskRef != null && taskRef.matches("\\d+")) {
            long id = Long.parseLong(taskRef);
            Optional<Task> task = tasks.stream().filter(t -> t.getId() == id).findFirst();
            if (task.isPresent())
                return task.get();
        }
        List<Task> named = tasks.stream().filter(t -> t.getName() != null && t.getName().equals(taskRef)).toList();
        if (named.size() > 1)
            throw new TaskAction.InvalidTaskException("There are " + named.size() + " tasks named '" + taskRef
                    + "' in the bucket '" + bucket.getName() + "'; give the id of one of them");
        if (named.isEmpty())
            throw new TaskNotFoundException("The bucket '" + bucket.getName() + "' has no task '" + taskRef
                    + "' (tasks are assigned to buckets or classes in Settings > Tasks)");
        return named.get(0);
    }

    /** The tasks shown in the bucket: assigned to it or to its class. In a transaction (identity comparisons). */
    public List<Task> tasksOf(Bucket bucket) {
        DataClass dataClass = bucket.getDataClass();
        List<Task> result = new ArrayList<>();
        for (Task task : taskRepository.findAllByDeletedOrderById(false))
            if ((task.getBuckets() != null && task.getBuckets().stream().anyMatch(b -> b.getId() == bucket.getId()))
                    || (dataClass != null && task.getDataClasses() != null
                    && task.getDataClasses().stream().anyMatch(c -> c.getId() == dataClass.getId())))
                result.add(task);
        return result;
    }

    // a new filter for each query: the data service adds its own conditions to it
    private static LogicFilter filter(User user, Map<String, Object> filterLogic, SearchRules rules) throws InvalidRuleException {
        TaskRunDTO combined = new TaskRunDTO();
        List<Object> logic = new ArrayList<>();
        if (filterLogic != null && !filterLogic.isEmpty())
            logic.add(filterLogic);
        if (rules != null) {
            if (rules.getLogic() != null && !rules.getLogic().isEmpty())
                logic.add(rules.getLogic());
            combined.setRules(rules.getRules());
            combined.setConditions(rules.getConditions());
        }
        if (!logic.isEmpty())
            combined.setLogic(logic.size() == 1 ? castMap(logic.get(0)) : Map.of("and", logic));
        return LogicFilter.of(user.getUsername(), combined);
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> castMap(Object value) {
        return (Map<String, Object>) value;
    }
}
