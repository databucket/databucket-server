package pl.databucket.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import pl.databucket.dto.TaskDto;
import pl.databucket.entity.Task;
import pl.databucket.repository.TaskRepository;


@Service
public class TaskService {

    @Autowired
    private TaskRepository taskRepository;

    public Task createTask(TaskDto taskDto) {
        Task task = new Task();
        task.setName(taskDto.getName());
        task.setDescription(taskDto.getDescription());
        task.setConfiguration(taskDto.getConfiguration());
//        task.setBuckets();
//        task.setDataClasses();

        return taskRepository.save(task);
    }

    public Page<Task> getTasks(Specification<Task> specification, Pageable pageable) {
        return taskRepository.findAll(specification, pageable);
    }

    public Task modifyTask(TaskDto taskDto) {
        Task task = taskRepository.getOne(taskDto.getId());
        task.setName(taskDto.getName());
        task.setDescription(taskDto.getDescription());
        task.setConfiguration(taskDto.getConfiguration());
//        task.setBuckets();
//        task.setDataClasses();

        return taskRepository.save(task);
    }

    public void deleteTask(long taskId) {
        Task task = taskRepository.getOne(taskId);
        task.setDeleted(true);
        taskRepository.save(task);
    }

//    private String referencesTaskItem(int taskId) throws UnknownColumnException, ConditionNotAllowedException {
//        final String AS_ID = " as 'id'";
//        final String AS_NAME = " as 'name'";
//        String result = "";
//
//        List<Condition> conditions = new ArrayList<>();
//        conditions.add(new Condition(COL.DELETED, Operator.equal, false));
//        Map<String, Object> paramMap = new HashMap<>();
//
//        Query query = new Query(TAB.EVENT, true)
//                .select(new String[]{COL.EVENT_ID + AS_ID, COL.EVENT_NAME + AS_NAME, COL.TASKS})
//                .from()
//                .where(conditions, paramMap);
//
//        List<Map<String, Object>> eventList = jdbcTemplate.queryForList(query.toString(logger, paramMap), paramMap);
//
//        if (eventList.size() > 0) {
//            ObjectMapper mapper = new ObjectMapper();
//            List<Map<String, Object>> refEventList = new ArrayList<>();
//            for (Map<String, Object> event: eventList) {
//                String eventTasksStr = (String) event.get(COL.TASKS);
//                try {
//                    List<Map<String, Object>> eventTasks = mapper.readValue(eventTasksStr, new TypeReference<List<Map<String, Object>>>() {});
//                    if (eventTasks.size() > 0) {
//                        for (Map<String, Object> eventTask: eventTasks) {
//                            int eventTaskId = Integer.parseInt((String) eventTask.get(COL.TASK_ID));
//                            if (eventTaskId == taskId)
//                                refEventList.add(event);
//                        }
//                    }
//                } catch (Exception e) {
//                    e.printStackTrace();
//                }
//            }
//
//            if (refEventList.size() > 0) {
//                result += serviceUtils.getStringWithItemsNames(C.EVENTS, refEventList);
//            }
//        }
//
//        conditions = new ArrayList<>();
//        conditions.add(new Condition(COL.TASK_ID, Operator.equal, taskId));
//        query = new Query(TAB.EVENT_LOG, false)
//                .select(COL.COUNT)
//                .from()
//                .where(conditions, paramMap);
//        int count = jdbcTemplate.queryForObject(query.toString(logger, paramMap), paramMap, Integer.class);
//        if (count > 1)
//            result += "\nLOGS";
//
//        if (result.length() > 0)
//            return result;
//        else
//            return null;
//    }

}
