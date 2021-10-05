package pl.databucket.server.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import pl.databucket.server.service.data.Query;
import pl.databucket.server.dto.BucketDto;
import pl.databucket.server.entity.*;
import pl.databucket.server.exception.ItemAlreadyExistsException;
import pl.databucket.server.exception.ItemNotFoundException;
import pl.databucket.server.exception.ModifyByNullEntityIdException;
import pl.databucket.server.repository.*;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class BucketService {

    @Autowired
    private BucketRepository bucketRepository;

    @Autowired
    private GroupRepository groupRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TeamRepository teamRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private DataClassRepository dataClassRepository;

    @Autowired
    private TagRepository tagRepository;

    @Autowired
    private ViewRepository viewRepository;

    @Autowired
    private NamedParameterJdbcTemplate jdbcTemplate;

    Logger logger = LoggerFactory.getLogger(BucketService.class);

    public Bucket createBucket(BucketDto bucketDto) throws ItemAlreadyExistsException, ItemNotFoundException {
        if (bucketRepository.existsByNameAndDeleted(bucketDto.getName(), false))
            throw new ItemAlreadyExistsException(Bucket.class, bucketDto.getName());

        Bucket bucket = new Bucket();
        bucket.setName(bucketDto.getName());
        bucket.setDescription(bucketDto.getDescription());
        bucket.setHistory(bucketDto.isHistory());
        bucket.setProtectedData(bucketDto.isProtectedData());
        bucket.setIconName(bucketDto.getIconName());
        bucket = bucketRepository.saveAndFlush(bucket);

        if (bucketDto.getRoleId() != null) {
            Role role = roleRepository.getOne(bucketDto.getRoleId());
            bucket.setRole(role);
        }

        if (bucketDto.getClassId() != null) {
            DataClass dataClass = dataClassRepository.findByIdAndDeleted(bucketDto.getClassId(), false);
            if (dataClass != null)
                bucket.setDataClass(dataClass);
            else
                throw new ItemNotFoundException(DataClass.class, bucketDto.getClassId());
        }

        if (bucketDto.getGroupsIds() != null && bucketDto.getGroupsIds().size() > 0) {
            List<Group> groups = groupRepository.findAllByDeletedAndIdIn(false, bucketDto.getGroupsIds());
            if (groups.size() > 0)
                for (Group group : groups) {
                    group.getBuckets().add(bucket);
                    groupRepository.save(group);
                }
            bucket.setGroups(new HashSet<>(groups));
        }

        if (bucketDto.getUsersIds() != null && bucketDto.getUsersIds().size() > 0) {
            List<User> users = userRepository.findAllByIdIn(bucketDto.getUsersIds());
            bucket.setUsers(new HashSet<>(users));
        }

        if (bucketDto.getTeamsIds() != null && bucketDto.getTeamsIds().size() > 0) {
            List<Team> teams = teamRepository.findAllByIdIn(bucketDto.getTeamsIds());
            bucket.setTeams(new HashSet<>(teams));
        }

        bucket = bucketRepository.save(bucket);

        // Create table for data
        String sql = "CREATE TABLE public.\"" + bucket.getTableName() + "\" ("
                + "data_id bigserial NOT NULL,"
                + "tag_id bigint NULL,"
                + "reserved boolean NOT NULL DEFAULT false,"
                + "reserved_by character varying(50) DEFAULT NULL,"
                + "properties jsonb NOT NULL DEFAULT '{}'::jsonb,"
                + "created_by character varying(50) NOT NULL,"
                + "created_at timestamp without time zone NOT NULL DEFAULT current_timestamp,"
                + "modified_by character varying(50),"
                + "modified_at timestamp without time zone NOT NULL DEFAULT current_timestamp,"
                + "PRIMARY KEY (data_id),"
                + "CONSTRAINT fk_tag_id FOREIGN KEY(tag_id) REFERENCES tags(tag_id))";

        logger.debug(sql);
        jdbcTemplate.getJdbcTemplate().execute(sql);

        // Create table for history
        sql = "CREATE TABLE \"" + bucket.getTableHistoryName() + "\" ("
                + "id bigserial NOT NULL,"
                + "data_id bigint NOT NULL,"
                + "tag_id smallint DEFAULT NULL,"
                + "reserved boolean DEFAULT NULL,"
                + "properties jsonb DEFAULT NULL,"
                + "modified_at timestamp without time zone NOT NULL DEFAULT current_timestamp,"
                + "modified_by character varying(50),"
                + "PRIMARY KEY (id),"
                + "CONSTRAINT fk_tag_h_id FOREIGN KEY(data_id) REFERENCES \"" + bucket.getTableName() + "\"(data_id))";

        logger.debug(sql);
        jdbcTemplate.getJdbcTemplate().execute(sql);

        createBeforeDeleteTrigger(bucket);

        // Create after insert and after update triggers if history is enabled
        if (bucket.isHistory()) {
            createAfterInsertTrigger(bucket);
            createAfterUpdateTrigger(bucket);
        }

        return bucket;
    }

    public Page<Bucket> getBuckets(Specification<Bucket> specification, Pageable pageable) {
        return bucketRepository.findAll(specification, pageable);
    }

    public Bucket getBucket(String bucketName) {
        return bucketRepository.findByNameAndDeleted(bucketName, false);
    }

    public List<Bucket> getBuckets() {
        return bucketRepository.findAllByDeletedOrderById(false);
    }

    public List<Bucket> getBuckets(List<Long> ids) {
        return bucketRepository.findAllByDeletedAndIdIn(false, ids);
    }

    public Bucket modifyBucket(BucketDto bucketDto) throws ItemAlreadyExistsException, ItemNotFoundException, ModifyByNullEntityIdException {
        if (bucketDto.getId() == null)
            throw new ModifyByNullEntityIdException(Bucket.class);

        Bucket bucket = bucketRepository.findByIdAndDeleted(bucketDto.getId(), false);

        if (bucket == null)
            throw new ItemNotFoundException(Bucket.class, bucketDto.getId());

        if (!bucket.getName().equals(bucketDto.getName()))
            if (bucketRepository.existsByNameAndDeleted(bucketDto.getName(), false))
                throw new ItemAlreadyExistsException(Bucket.class, bucketDto.getName());


        bucket.setName(bucketDto.getName());
        bucket.setDescription(bucketDto.getDescription());
        bucket.setIconName(bucketDto.getIconName());

        if (bucketDto.getClassId() != null) {
            DataClass dataClass = dataClassRepository.findByIdAndDeleted(bucketDto.getClassId(), false);
            bucket.setDataClass(dataClass);
        } else
            bucket.setDataClass(null);

        // Modify groups
        Set<Long> currentGroupsIds = bucket.getGroups().stream().map(Group::getId).collect(Collectors.toSet());
        Set<Long> newGroupsIds = bucketDto.getGroupsIds();
        if (!(currentGroupsIds.containsAll(newGroupsIds) && newGroupsIds.size() == currentGroupsIds.size())) {
            Set<Long> toAdd = newGroupsIds.stream().filter(element -> !currentGroupsIds.contains(element)).collect(Collectors.toSet());
            if (toAdd.size() > 0) {
                List<Group> groups = groupRepository.findAllByDeletedAndIdIn(false, toAdd);
                for (Group group : groups) {
                    group.getBuckets().add(bucket);
                    groupRepository.save(group);
                }
            }

            Set<Long> toRemove = currentGroupsIds.stream().filter(element -> !newGroupsIds.contains(element)).collect(Collectors.toSet());
            if (toRemove.size() > 0) {
                List<Group> groups = groupRepository.findAllByDeletedAndIdIn(false, toRemove);
                for (Group group : groups) {
                    group.getBuckets().remove(bucket);
                    groupRepository.save(group);
                }
            }

            // Just to send proper response
            List<Group> groups = groupRepository.findAllByDeletedAndIdIn(false, bucketDto.getGroupsIds());
            bucket.setGroups(new HashSet<>(groups));
        }

        if (bucketDto.getUsersIds() != null && bucketDto.getUsersIds().size() > 0) {
            List<User> users = userRepository.findAllByIdIn(bucketDto.getUsersIds());
            bucket.setUsers(new HashSet<>(users));
        } else
            bucket.setUsers(null);

        if (bucketDto.getTeamsIds() != null && bucketDto.getTeamsIds().size() > 0) {
            List<Team> teams = teamRepository.findAllByIdIn(bucketDto.getTeamsIds());
            bucket.setTeams(new HashSet<>(teams));
        } else
            bucket.setTeams(null);

        if (bucket.isHistory() != bucketDto.isHistory()) {
            if (bucketDto.isHistory()) {
                createAfterInsertTrigger(bucket);
                createAfterUpdateTrigger(bucket);
                bucket.setHistory(true);
            } else {
                removeAfterInsertTrigger(bucket);
                removeAfterUpdateTrigger(bucket);
                bucket.setHistory(false);
            }
        }

        bucket.setProtectedData(bucketDto.isProtectedData());

        if (bucketDto.getRoleId() != null) {
            Role role = roleRepository.getOne(bucketDto.getRoleId());
            bucket.setRole(role);
        } else
            bucket.setRole(null);

        return bucketRepository.save(bucket);
    }

    public void deleteBucket(long bucketId) throws ItemNotFoundException {
        Bucket bucket = bucketRepository.findByIdAndDeleted(bucketId, false);

        if (bucket == null)
            throw new ItemNotFoundException(Bucket.class, bucketId);

        for (Group group : bucket.getGroups()) {
            group.getBuckets().remove(bucket);
            groupRepository.save(group);
        }

        bucket.setUsers(null);
        bucket.setTeams(null);

        for (Tag tag : bucket.getTags()) {
            tag.getBuckets().remove(bucket);
            tagRepository.save(tag);
        }

        for (View view : bucket.getViews()) {
            view.getBuckets().remove(bucket);
            viewRepository.save(view);
        }

        bucket.setDeleted(true);
        bucketRepository.save(bucket);

        // Drop bucket history table
        Query query = new Query(bucket.getTableHistoryName()).dropTable();
        jdbcTemplate.getJdbcTemplate().execute(query.toString(logger, null));

        // Drop bucket table
        query = new Query(bucket.getTableName()).dropTable();
        jdbcTemplate.getJdbcTemplate().execute(query.toString(logger, null));
    }

    private void createBeforeDeleteTrigger(Bucket bucket) {
        removeBeforeDeleteTrigger(bucket);

        String sql = "CREATE TRIGGER trigger_before_delete\n" +
                "BEFORE DELETE\n" +
                "ON \"" + bucket.getTableName() + "\"" +
                "FOR EACH ROW\n" +
                "EXECUTE PROCEDURE before_delete()";

        logger.debug(sql);
        jdbcTemplate.getJdbcTemplate().execute(sql);
    }

    private void createAfterInsertTrigger(Bucket bucket) {
        removeAfterInsertTrigger(bucket);

        String sql = "CREATE TRIGGER trigger_after_insert\n" +
                "AFTER INSERT\n" +
                "ON \"" + bucket.getTableName() + "\"\n" +
                "FOR EACH ROW\n" +
                "EXECUTE PROCEDURE after_insert()";

        logger.debug(sql);
        jdbcTemplate.getJdbcTemplate().execute(sql);
    }

    private void createAfterUpdateTrigger(Bucket bucket) {
        removeAfterUpdateTrigger(bucket);

        String sql = "CREATE TRIGGER trigger_after_update\n" +
                "AFTER UPDATE\n" +
                "ON \"" + bucket.getTableName() + "\"\n" +
                "FOR EACH ROW\n" +
                "EXECUTE PROCEDURE after_update()";

        logger.debug(sql);
        jdbcTemplate.getJdbcTemplate().execute(sql);
    }

    private void removeBeforeDeleteTrigger(Bucket bucket) {
        String sql = "DROP TRIGGER IF EXISTS trigger_before_delete on \"" + bucket.getTableName() + "\"";
        logger.debug(sql);
        jdbcTemplate.getJdbcTemplate().execute(sql);
    }

    private void removeAfterInsertTrigger(Bucket bucket) {
        String sql = "DROP TRIGGER IF EXISTS trigger_after_insert on \"" + bucket.getTableName() + "\"";
        logger.debug(sql);
        jdbcTemplate.getJdbcTemplate().execute(sql);
    }

    private void removeAfterUpdateTrigger(Bucket bucket) {
        String sql = "DROP TRIGGER IF EXISTS trigger_after_update on \"" + bucket.getTableName() + "\"";
        logger.debug(sql);
        jdbcTemplate.getJdbcTemplate().execute(sql);
    }

    public List<Bucket> getAccessTreeBuckets(User user) {
        return bucketRepository.findAllByDeletedOrderById(false).stream().filter(bucket -> hasUserAccessToBucket(bucket, user)).collect(Collectors.toList());
    }

    public boolean hasUserAccessToBucket(Bucket bucket, User user) {
        boolean accessForUser = bucket.getUsers().size() > 0 && bucket.getUsers().contains(user);

        if (accessForUser)
            return true;
        else {
            boolean accessByRole = bucket.getRole() != null ? user.getRoles().contains(bucket.getRole()) : bucket.getTeams().size() > 0;
            boolean accessByTeam = bucket.getTeams().size() > 0 ? !Collections.disjoint(bucket.getTeams(), user.getTeams()) : bucket.getRole() != null;
            return accessByRole && accessByTeam;
        }
    }

}
