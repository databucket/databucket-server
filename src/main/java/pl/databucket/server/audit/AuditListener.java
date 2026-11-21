package pl.databucket.server.audit;

import org.hibernate.action.spi.BeforeTransactionCompletionProcess;
import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.engine.spi.CollectionEntry;
import org.hibernate.persister.collection.AbstractCollectionPersister;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.persister.entity.AbstractEntityPersister;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.event.spi.*;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.type.Type;
import org.springframework.beans.BeanWrapperImpl;
import pl.databucket.server.entity.*;

import java.sql.PreparedStatement;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.Instant;
import java.util.*;

/**
 * Records the changes of the settings and management objects (audit_events): created, changed and deleted (soft
 * deletes too: deleted false -> true), with the properties that changed, and the changes of their links to other
 * objects (many-to-many, e.g. the users of a team, the roles of a user) as the lists of names before and after. All
 * changes of one object in a transaction become one event. The events of a transaction are written with plain SQL on
 * its connection just before it commits, so a change and its record are committed together (and a rolled back change
 * leaves none). Changes made with plain SQL (bucket data, the scheduler's run results, clearing a project) are not
 * seen here; some of them are recorded from the operation log (AuditActionRecorder).
 */
public class AuditListener implements PostInsertEventListener, PostUpdateEventListener, PostDeleteEventListener,
        PostCollectionRecreateEventListener, PostCollectionUpdateEventListener, PostCollectionRemoveEventListener,
        PreCollectionRemoveEventListener {

    /** The audited entities and their names in the log. */
    static final Map<Class<?>, String> TYPES = Map.ofEntries(
            Map.entry(Bucket.class, "Bucket"), Map.entry(DataClass.class, "Class"),
            Map.entry(DataColumns.class, "Columns"), Map.entry(DataEnum.class, "Enum"),
            Map.entry(DataFilter.class, "Filter"), Map.entry(Group.class, "Group"), Map.entry(Svg.class, "Icon"),
            Map.entry(Tag.class, "Tag"), Map.entry(Task.class, "Task"), Map.entry(TaskSchedule.class, "Schedule"),
            Map.entry(Team.class, "Team"), Map.entry(View.class, "View"), Map.entry(Project.class, "Project"),
            Map.entry(User.class, "User"));

    private static final List<String> NAME_PROPERTIES = List.of("name", "username");

    /** One object's event of a transaction, filled by the entity and collection events of that object. */
    private static final class Event {
        final Instant at = Instant.now();
        final String entityType;
        final String entityId;
        final String actor = AuditContext.actor();
        final String role = AuditContext.role();
        final String source = AuditContext.source();
        final String requestId = AuditContext.requestId();
        Integer projectId;
        String entityName;
        String action;
        final Map<String, Object> changes = new LinkedHashMap<>();

        Event(String entityType, String entityId) {
            this.entityType = entityType;
            this.entityId = entityId;
        }
    }

    /** The events of a session's transaction, by object; written just before the commit. */
    private static final class Batch implements BeforeTransactionCompletionProcess {
        final Map<String, Event> events = new LinkedHashMap<>();

        @Override
        public void doBeforeTransactionCompletion(org.hibernate.engine.spi.SessionImplementor session) {
            BATCHES.remove(session);
            for (Event event : events.values())
                if (!("UPDATE".equals(event.action) && event.changes.isEmpty()))
                    write(session, event);
        }
    }

    // the batch of each open session with audited changes (removed when its transaction completes)
    private static final Map<SharedSessionContractImplementor, Batch> BATCHES =
            Collections.synchronizedMap(new WeakHashMap<>());

    // ------------------------------------------------------------------ entity events

    @Override
    public void onPostInsert(PostInsertEvent event) {
        String type = TYPES.get(event.getEntity().getClass());
        if (type == null)
            return;
        Object[] state = event.getState();
        Event audit = event(event.getSession(), type, event.getId());
        audit.action = "CREATE";
        fill(audit, event.getEntity(), event.getPersister(), event.getId(), state);
        merge(audit, AuditChanges.of(event.getEntity().getClass().getSimpleName(), properties(event.getPersister(), null, state)));
    }

    @Override
    public void onPostUpdate(PostUpdateEvent event) {
        String type = TYPES.get(event.getEntity().getClass());
        if (type == null)
            return;
        EntityPersister persister = event.getPersister();
        Object[] before = event.getOldState();
        Object[] after = event.getState();
        Event audit = event(event.getSession(), type, event.getId());
        fill(audit, event.getEntity(), persister, event.getId(), after);
        int deleted = index(persister, "deleted");
        if (deleted >= 0 && before != null && !Boolean.TRUE.equals(before[deleted]) && Boolean.TRUE.equals(after[deleted])) {
            audit.action = "DELETE";
            audit.changes.clear();
            return;
        }
        if (audit.action == null)
            audit.action = "UPDATE";
        if (!"DELETE".equals(audit.action))
            merge(audit, AuditChanges.of(event.getEntity().getClass().getSimpleName(), properties(persister, before, after)));
    }

    @Override
    public void onPostDelete(PostDeleteEvent event) {
        String type = TYPES.get(event.getEntity().getClass());
        if (type == null)
            return;
        Event audit = event(event.getSession(), type, event.getId());
        fill(audit, event.getEntity(), event.getPersister(), event.getId(), event.getDeletedState());
        audit.action = "DELETE";
        audit.changes.clear();
    }

    @Override
    public boolean requiresPostCommitHandling(EntityPersister persister) {
        return false;
    }

    // ------------------------------------------------------------------ collection events (links)

    @Override
    public void onPostRecreateCollection(PostCollectionRecreateEvent event) {
        collection(event, List.of(), elements(event.getCollection()));
    }

    @Override
    public void onPostUpdateCollection(PostCollectionUpdateEvent event) {
        collection(event, snapshot(event.getCollection()), elements(event.getCollection()));
    }

    @Override
    public void onPostRemoveCollection(PostCollectionRemoveEvent event) {
        collection(event, snapshot(event.getCollection()), List.of());
    }

    // A collection replaced without being read (e.g. setProjects(new HashSet<>(...))) is removed and recreated, and
    // Hibernate doesn't know what it held: read its links from the join table while they are still there.
    @Override
    public void onPreRemoveCollection(PreCollectionRemoveEvent event) {
        if (event.getCollection().wasInitialized())
            return;
        CollectionEntry entry = event.getSession().getPersistenceContextInternal().getCollectionEntry(event.getCollection());
        Object owner = event.getAffectedOwnerOrNull();
        if (entry == null || owner == null || TYPES.get(owner.getClass()) == null
                || !(entry.getLoadedPersister() instanceof AbstractCollectionPersister persister)
                || !persister.isManyToMany() || entry.getLoadedKey() == null)
            return;
        List<String> before = linked(event.getSession(), persister, entry.getLoadedKey());
        if (before != null)
            collection(event, before, List.of());
    }

    // the names of the objects linked in the join table (id, name), or null when they can't be read
    private static List<String> linked(EventSource session, AbstractCollectionPersister persister, Object key) {
        if (!(persister.getElementPersister() instanceof AbstractEntityPersister target))
            return null;
        String nameColumn = null;
        for (String property : NAME_PROPERTIES)
            if (Arrays.asList(target.getPropertyNames()).contains(property)) {
                nameColumn = target.getPropertyColumnNames(property)[0];
                break;
            }
        String sql = "SELECT t." + target.getIdentifierColumnNames()[0] + (nameColumn != null ? ", t." + nameColumn : "")
                + " FROM " + persister.getTableName() + " j JOIN " + target.getTableName() + " t ON t."
                + target.getIdentifierColumnNames()[0] + " = j." + persister.getElementColumnNames()[0]
                + " WHERE j." + persister.getKeyColumnNames()[0] + " = ?";
        List<Object[]> rows = new ArrayList<>();
        boolean withName = nameColumn != null;
        try {
            session.doWork(connection -> {
                try (PreparedStatement statement = connection.prepareStatement(sql)) {
                    statement.setObject(1, key);
                    try (java.sql.ResultSet result = statement.executeQuery()) {
                        while (result.next())
                            rows.add(new Object[]{result.getObject(1), withName ? result.getString(2) : null});
                    }
                }
            });
        } catch (RuntimeException e) {
            return null;
        }
        return labelsOf(rows);
    }

    private void collection(AbstractCollectionEvent event, List<String> before, List<String> after) {
        Object owner = event.getAffectedOwnerOrNull();
        String type = owner != null ? TYPES.get(owner.getClass()) : null;
        if (type == null || event.getAffectedOwnerIdOrNull() == null)
            return;
        String role = role(event);
        if (role == null)
            return;
        String property = role.substring(role.lastIndexOf('.') + 1);
        Event audit = event(event.getSession(), type, event.getAffectedOwnerIdOrNull());
        if (audit.action == null)
            audit.action = "UPDATE";
        if (audit.entityName == null)
            audit.entityName = nameOf(owner);
        if (audit.projectId == null)
            audit.projectId = projectOf(owner, event.getAffectedOwnerIdOrNull());
        if ("DELETE".equals(audit.action))
            return;
        merge(audit, Map.of(property, Arrays.asList(before, after)));
    }

    // the collection's role (e.g. pl.databucket.server.entity.Team.users): a recreated collection doesn't know it
    // itself, its entry in the session does
    private static String role(AbstractCollectionEvent event) {
        if (event.getCollection().getRole() != null)
            return event.getCollection().getRole();
        CollectionEntry entry = event.getSession().getPersistenceContextInternal().getCollectionEntry(event.getCollection());
        if (entry == null)
            return null;
        if (entry.getRole() != null)
            return entry.getRole();
        CollectionPersister persister = entry.getCurrentPersister() != null ? entry.getCurrentPersister() : entry.getLoadedPersister();
        return persister != null ? persister.getRole() : null;
    }

    // the names of the linked objects, sorted
    private static List<String> elements(PersistentCollection<?> collection) {
        if (!collection.wasInitialized())
            return null; // not read, so not changed by this transaction's code
        List<Object> items = new ArrayList<>();
        Iterator<?> iterator = collection.entries(null);
        while (iterator.hasNext())
            items.add(iterator.next());
        return labels(items);
    }

    // the links as they were read (null when not known)
    private static List<String> snapshot(PersistentCollection<?> collection) {
        Object snapshot = collection.getStoredSnapshot();
        Collection<?> items = snapshot instanceof Map<?, ?> map ? map.keySet()
                : snapshot instanceof Collection<?> list ? list : null;
        return items != null ? labels(items) : null;
    }

    // names, sorted; a name that more objects have (e.g. teams of different projects) with the id
    static List<String> labels(Collection<?> entities) {
        List<Object[]> pairs = new ArrayList<>();
        for (Object entity : entities)
            pairs.add(new Object[]{entity != null ? idOf(entity) : null, entity != null ? nameOf(entity) : null});
        return labelsOf(pairs);
    }

    /** Labels of [id, name] pairs: the name, with " #id" when more have it; "#id" without a name. Sorted. */
    static List<String> labelsOf(List<Object[]> pairs) {
        Map<String, Integer> counts = new HashMap<>();
        for (Object[] pair : pairs)
            if (pair[1] != null)
                counts.merge(String.valueOf(pair[1]), 1, Integer::sum);
        List<String> labels = new ArrayList<>();
        for (Object[] pair : pairs) {
            String name = pair[1] != null ? String.valueOf(pair[1]) : null;
            labels.add(name == null ? "#" + pair[0] : counts.get(name) > 1 ? name + " #" + pair[0] : name);
        }
        Collections.sort(labels);
        return labels;
    }

    private static Object idOf(Object entity) {
        BeanWrapperImpl wrapper = new BeanWrapperImpl(entity);
        return wrapper.isReadableProperty("id") ? wrapper.getPropertyValue("id") : "?";
    }

    // ------------------------------------------------------------------ batching

    private static Event event(EventSource session, String type, Object id) {
        Batch batch;
        synchronized (BATCHES) {
            batch = BATCHES.get(session);
            if (batch == null) {
                batch = new Batch();
                BATCHES.put(session, batch);
                session.getActionQueue().registerProcess(batch);
            }
        }
        return batch.events.computeIfAbsent(type + "#" + id, key -> new Event(type, String.valueOf(id)));
    }

    // the changes of a later event of the same object: the first "before", the last "after"
    @SuppressWarnings("unchecked")
    private static void merge(Event audit, Map<String, Object> changes) {
        for (Map.Entry<String, Object> change : changes.entrySet()) {
            List<Object> next = (List<Object>) change.getValue();
            Object existing = audit.changes.get(change.getKey());
            Object before = existing instanceof List<?> previous ? previous.get(0) : next.get(0);
            Object after = next.get(1);
            if (Objects.equals(before, after) || (before == null && after instanceof Collection<?> c && c.isEmpty()))
                audit.changes.remove(change.getKey());
            else
                audit.changes.put(change.getKey(), Arrays.asList(before, after));
        }
    }

    private static void fill(Event audit, Object entity, EntityPersister persister, Object id, Object[] state) {
        String name = name(persister, state);
        if (name != null)
            audit.entityName = name;
        if (audit.projectId == null)
            audit.projectId = projectId(entity, persister, id, state);
    }

    // ------------------------------------------------------------------ writing

    private static void write(SharedSessionContractImplementor session, Event event) {
        session.doWork(connection -> {
            try (PreparedStatement statement = connection.prepareStatement("INSERT INTO audit_events (event_at,"
                    + " project_id, actor, actor_role, source, action, entity_type, entity_id, entity_name, changes,"
                    + " request_id) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, CAST(? AS jsonb), ?)")) {
                statement.setTimestamp(1, Timestamp.from(event.at));
                if (event.projectId != null)
                    statement.setInt(2, event.projectId);
                else
                    statement.setNull(2, Types.INTEGER);
                statement.setString(3, AuditContext.truncate(event.actor, 50));
                statement.setString(4, event.role);
                statement.setString(5, event.source);
                statement.setString(6, event.action);
                statement.setString(7, event.entityType);
                statement.setString(8, AuditContext.truncate(event.entityId, 40));
                statement.setString(9, AuditContext.truncate(event.entityName, 100));
                statement.setString(10, event.changes.isEmpty() ? null : AuditChanges.toJson(event.changes));
                statement.setString(11, AuditContext.truncate(event.requestId, 50));
                statement.executeUpdate();
            }
        });
    }

    // ------------------------------------------------------------------ properties

    // the properties of the object, before and after; collections are recorded by the collection events
    private static List<AuditChanges.Property> properties(EntityPersister persister, Object[] before, Object[] after) {
        String[] names = persister.getPropertyNames();
        Type[] types = persister.getPropertyTypes();
        List<AuditChanges.Property> properties = new ArrayList<>();
        for (int i = 0; i < names.length; i++) {
            if (types[i].isCollectionType())
                continue;
            boolean entity = types[i].isEntityType();
            Object old = before != null ? before[i] : null;
            Object now = after != null ? after[i] : null;
            properties.add(new AuditChanges.Property(names[i], entity ? reference(old) : old, entity ? reference(now) : now));
        }
        return properties;
    }

    // a reference to another object as {id, name}
    private static Object reference(Object entity) {
        if (entity == null)
            return null;
        BeanWrapperImpl wrapper = new BeanWrapperImpl(entity);
        Map<String, Object> reference = new LinkedHashMap<>();
        if (wrapper.isReadableProperty("id"))
            reference.put("id", wrapper.getPropertyValue("id"));
        String name = nameOf(entity);
        if (name != null)
            reference.put("name", name);
        return reference;
    }

    private static String nameOf(Object entity) {
        BeanWrapperImpl wrapper = new BeanWrapperImpl(entity);
        for (String property : NAME_PROPERTIES)
            if (wrapper.isReadableProperty(property) && wrapper.getPropertyValue(property) != null)
                return String.valueOf(wrapper.getPropertyValue(property));
        return null;
    }

    private static Integer projectOf(Object entity, Object id) {
        if (entity instanceof Project)
            return ((Number) id).intValue();
        BeanWrapperImpl wrapper = new BeanWrapperImpl(entity);
        return wrapper.isReadableProperty("projectId") && wrapper.getPropertyValue("projectId") instanceof Number n
                ? n.intValue() : null;
    }

    private static int index(EntityPersister persister, String property) {
        String[] names = persister.getPropertyNames();
        for (int i = 0; i < names.length; i++)
            if (names[i].equals(property))
                return i;
        return -1;
    }

    private static String name(EntityPersister persister, Object[] state) {
        if (state == null)
            return null;
        for (String property : NAME_PROPERTIES) {
            int i = index(persister, property);
            if (i >= 0 && state[i] != null)
                return String.valueOf(state[i]);
        }
        return null;
    }

    // the project of the object: its project_id (tenant), the project itself, null for users
    private static Integer projectId(Object entity, EntityPersister persister, Object id, Object[] state) {
        if (entity instanceof Project)
            return ((Number) id).intValue();
        int i = index(persister, "projectId");
        if (i >= 0 && state != null && state[i] instanceof Number n)
            return n.intValue();
        return null;
    }
}
