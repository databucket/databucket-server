package pl.databucket.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import pl.databucket.dto.EventDto;
import pl.databucket.entity.Event;
import pl.databucket.exception.*;
import pl.databucket.repository.EventRepository;


@Service
public class EventService {

    @Autowired
    private EventRepository eventRepository;

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final ServiceUtils serviceUtils;
    Logger logger = LoggerFactory.getLogger(EventService.class);

    public EventService(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        this.serviceUtils = new ServiceUtils(jdbcTemplate, logger);
    }

    public Event createEvent(EventDto eventDto) throws ItemAlreadyExistsException {
        if (eventRepository.existsByNameAndDeleted(eventDto.getName(), false))
            throw new ItemAlreadyExistsException("Event", eventDto.getName());

        // check an event exist in the database events
        if (serviceUtils.isDatabaseEventExist(eventDto.getName()))
            throw new ItemAlreadyExistsException("Event " + eventDto.getName() + " already exist in the database!");

        Event event = new Event();
        event.setName(eventDto.getName());
        event.setDescription(eventDto.getDescription());
        event.setActive(eventDto.isActive());
//        event.setBucket(eventDto.getBucketId()); TODO
//        event.setDataClass(eventDto.getClassId()); TODO

        event.setSchedule(eventDto.getSchedule());
        event.setTasks(eventDto.getTasks());

        event = eventRepository.save(event);

//        if (eventDto.isActive()) {
//            List<Map<String, Object>> buckets = serviceUtils.getBuckets(bucketId, classId);
//            Map<String, Object> event = getEvent(eventId);
//            String eName = (String) event.get(COL.EVENT_NAME);
//            createDatabaseEvent(eventId, eName, buckets, schedule, tasks);
//        }

        return event;
    }

    public Page<Event> getEvents(Specification<Event> specification, Pageable pageable) {
        return eventRepository.findAll(specification, pageable);
    }

    public Event modifyEvent(EventDto eventDto) throws ItemAlreadyExistsException {

        if (eventRepository.existsByNameAndDeleted(eventDto.getName(), false))
            throw new ItemAlreadyExistsException("Event", eventDto.getName());

        if (serviceUtils.isDatabaseEventExist(eventDto.getName()))
            throw new ItemAlreadyExistsException("Event " + eventDto.getName() + " already exist in the database!");

        Event event = eventRepository.getOne(eventDto.getId());
        event.setName(eventDto.getName());
        event.setDescription(eventDto.getDescription());
//        event.setBucket(eventDto.getBucketId()); TODO
//        event.setDataClass(eventDto.getDataClassId()); TODO
        event.setSchedule(eventDto.getSchedule());
        event.setTasks(eventDto.getTasks());

        event = eventRepository.save(event);

        if (event.isActive())
            createDatabaseEvent(event);

        return event;
    }

//    private void deactivateEvents(List<Integer> eventsIds) throws UnknownColumnException, ConditionNotAllowedException {
//        Condition condition = new Condition(COL.EVENT_ID, Operator.in, eventsIds);
//        Map<String, Object> paramMap = new HashMap<>();
//        paramMap.put(COL.UPDATED_BY, "server");
//        paramMap.put(COL.ACTIVE, false);
//
//        Query query = new Query(TAB.EVENT, true)
//                .update()
//                .set(paramMap)
//                .where(condition, paramMap);
//
//        this.jdbcTemplate.update(query.toString(logger, paramMap), paramMap);
//    }

    public void deleteEvent(Long eventId) {
        Event event = eventRepository.getOne(eventId);
        removeDatabaseEvent(event.getName());

        event.setDeleted(true);
        eventRepository.save(event);
    }

    private void createDatabaseEvent(Event event) {

    }

//    private void createDatabaseEvent(int eventId, String eventName, List<Map<String, Object>> buckets, Map<String, Object> schedule, List<Map<String, Object>> tasks) throws UnknownColumnException, UnexpectedException, EmptyInputValueException, ConditionNotAllowedException {
//        String eventQuery = "";
//
//        eventQuery += "CREATE EVENT \"" + eventName + "\"\n";
//        eventQuery += "\tON SCHEDULE\n";
//
//        // Schedule
//        boolean periodically = (boolean) schedule.get(C.PERIODICALLY);
//        if (periodically) {
//            String starts = serviceUtils.getFormattedDate((String) schedule.get(C.STARTS));
//            Map<String, Object> interval = (Map<String, Object>) schedule.get(C.INTERVAL);
//            int amount = (int) interval.get(C.AMOUNT);
//            String unit = serviceUtils.getUnit((int) interval.get(C.UNIT));
//            eventQuery += "\tEVERY " + amount + " " + unit + "\n";
//            eventQuery += "\tSTARTS '" + starts + "'\n";
//
//            boolean enabled_end = (boolean) schedule.get(C.ENABLE_ENDS);
//            if (enabled_end) {
//                String ends = serviceUtils.getFormattedDate((String) schedule.get(C.ENDS));
//                eventQuery += "\tENDS '" + ends + "'\n";
//            }
//        } else {
//            String at = serviceUtils.getFormattedDate((String) schedule.get(C.STARTS));
//            eventQuery += "\tAT '" + at + "'\n";
//        }
//
//        eventQuery += "DO\n";
//        eventQuery += "\tBEGIN\n";
//
//        // Tasks
//        for (Map<String, Object> tableTask : tasks) {
//            int taskId = Integer.parseInt((String) tableTask.get("task_id"));
//            Map<String, Object> task = serviceUtils.getTask(taskId);
//            String taskName = (String) task.get(COL.TASK_NAME);
//            Map<String, Object> configuration = (Map<String, Object>) task.get(C.CONFIGURATION);
//            List<Condition> conditions = FieldValidator.validateListOfConditions(configuration, false);
//            Map<String, Object> actions = (Map<String, Object>) configuration.get(C.ACTIONS);
//            String actionType = (String) actions.get(C.TYPE);
//            if (actionType.equals(C.MODIFY)) {
//                for (Map<String, Object> bucket : buckets) {
//                    int bucketId = (int) bucket.get(COL.BUCKET_ID);
//                    String bucketName = (String) bucket.get(COL.BUCKET_NAME);
//                    String modifyQuery = serviceUtils.getTaskModifyQuery(eventName, bucketName, conditions, actions);
//                    eventQuery += "\t\t/* Task: '" + taskName + "' for bucket '" + bucketName + "' */\n";
//                    eventQuery += "\t\t" + modifyQuery + ";\n";
//                    eventQuery += "\t\tSELECT row_count() INTO @affected_rows;\n";
//                    eventQuery += "\t\tINSERT INTO \"_event_log\" (\"event_id\", \"task_id\", \"bucket_id\", \"affected\") VALUES (" + eventId + ", " + taskId + ", " + bucketId + ", @affected_rows);\n\n";
//                }
//            } else if (actionType.equals(C.REMOVE)) {
//                for (Map<String, Object> bucket : buckets) {
//                    int bucketId = (int) bucket.get(COL.BUCKET_ID);
//                    String bucketName = (String) bucket.get(COL.BUCKET_NAME);
//                    String removeQuery = serviceUtils.getTaskRemoveQuery(bucketName, conditions);
//                    eventQuery += "\t\t/* Task: '" + taskName + "' for bucket '" + bucketName + "' */\n";
//                    eventQuery += "\t\t" + removeQuery + ";\n";
//                    eventQuery += "\t\tSELECT row_count() INTO @affected_rows;\n";
//                    eventQuery += "\t\tINSERT INTO \"_event_log\" (\"event_id\", \"task_id\", \"bucket_id\", \"affected\") VALUES (" + eventId + ", " + taskId + ", " + bucketId + ", @affected_rows);\n\n";
//                }
//            } else
//                throw new UnexpectedException("Undefined action type. Expected one of the [modify, remove]. Given " + actionType);
//        }
//
//        eventQuery += "\tEND\n";
//
//        logger.debug(eventQuery);
//        jdbcTemplate.getJdbcTemplate().execute(eventQuery);
//
////		enableEventScheduler();
//    }

    private void removeDatabaseEvent(String eventName) throws DataAccessException {
        String query = "DROP EVENT IF EXISTS \"" + eventName + "\"";
        jdbcTemplate.getJdbcTemplate().execute(query);
        logger.debug(query);
    }

}
