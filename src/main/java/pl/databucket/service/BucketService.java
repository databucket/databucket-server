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
import pl.databucket.database.*;
import pl.databucket.entity.User;
import pl.databucket.exception.*;
import pl.databucket.dto.BucketDto;
import pl.databucket.entity.Bucket;
import pl.databucket.repository.BucketRepository;
import pl.databucket.repository.DataClassRepository;
import pl.databucket.repository.UserRepository;

import java.util.HashSet;
import java.util.Set;

@Service
public class BucketService {

    @Autowired
    private BucketRepository bucketRepository;

    @Autowired
    private DataClassRepository dataClassRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private NamedParameterJdbcTemplate jdbcTemplate;

    private final String TABLE_PREFIX = "x";
    Logger logger = LoggerFactory.getLogger(BucketService.class);

    public Bucket createBucket(BucketDto bucketDto) throws BucketAlreadyExistsException, ConditionNotAllowedException, UnknownColumnException {
        if (!bucketRepository.existsByNameAndDeleted(bucketDto.getName(), false)) {
            Bucket bucket = new Bucket();
            bucket.setName(bucketDto.getName());
            bucket.setDescription(bucketDto.getDescription());
            bucket.setHistory(bucketDto.isHistory());
            bucket.setIconName(bucketDto.getIconName());
            if (bucketDto.getDataClassId() != null)
                bucket.setDataClass(dataClassRepository.getOne(bucketDto.getDataClassId()));

            if (bucketDto.getUsers() != null) {
                Set<User> users = new HashSet<>();
                for (long id : bucketDto.getUsers())
                    users.add(userRepository.getOne(id));
                bucket.setUsers(users);
            }

            bucket = bucketRepository.save(bucket);

            String dataTableName = TABLE_PREFIX + '-' + bucket.getId();

            // Create table for data
            String sql = "CREATE TABLE public.\"" + dataTableName + "\" ("
                    + "data_id bigserial NOT NULL,"
                    + "tag_id bigint NULL,"
                    + "reserved boolean NOT NULL DEFAULT false,"
                    + "reserved_by character varying(50) DEFAULT NULL,"
                    + "properties jsonb NOT NULL DEFAULT '{}'::jsonb,"
                    + "created_by character varying(50) NOT NULL,"
                    + "created_date timestamp with time zone NOT NULL DEFAULT current_timestamp,"
                    + "last_updated_by character varying(50),"
                    + "last_updated_date timestamp with time zone NOT NULL DEFAULT current_timestamp,"
                    + "PRIMARY KEY (data_id),"
                    + "CONSTRAINT fk_tag_id FOREIGN KEY(tag_id) REFERENCES tags(tag_id))";

            logger.debug(sql);
            jdbcTemplate.getJdbcTemplate().execute(sql);

            // Create table for history
            sql = "CREATE TABLE \"" + dataTableName + "-h\" ("
                    + "id bigserial NOT NULL,"
                    + "data_id bigint NOT NULL,"
                    + "tag_id smallint DEFAULT NULL,"
                    + "locked boolean DEFAULT NULL,"
                    + "properties jsonb DEFAULT NULL,"
                    + "updated_at timestamp with time zone NOT NULL DEFAULT current_timestamp,"
                    + "updated_by character varying(50),"
                    + "PRIMARY KEY (id),"
                    + "CONSTRAINT fk_tag_h_id FOREIGN KEY(data_id) REFERENCES \"" + dataTableName + "\"(data_id))";

            logger.debug(sql);
            jdbcTemplate.getJdbcTemplate().execute(sql);

            createBeforeDeleteTrigger(dataTableName);

            // Create after insert and after update triggers if history is enabled
            if (bucket.isHistory()) {
                createAfterInsertTrigger(dataTableName);
                createAfterUpdateTrigger(dataTableName);
            }

            return bucket;
        } else {
            throw new BucketAlreadyExistsException(bucketDto.getName());
        }
    }

    public Page<Bucket> getBuckets(Specification<Bucket> specification, Pageable pageable) {
        return bucketRepository.findAll(specification, pageable);
    }

    public void deleteBucket(long bucketId) throws ItemDoNotExistsException, UnknownColumnException, ItemAlreadyUsedException, ConditionNotAllowedException {
        Bucket bucket = bucketRepository.getOne(bucketId);
        bucket.setDeleted(true);
        bucketRepository.save(bucket);

        String dataTableName = TABLE_PREFIX + '-' + bucket.getId();

        // Drop bucket history table
        Query query = new Query(dataTableName + "-h", false).dropTable();
        jdbcTemplate.getJdbcTemplate().execute(query.toString(logger, null));

        // Drop bucket table
        query = new Query(dataTableName, false).dropTable();
        jdbcTemplate.getJdbcTemplate().execute(query.toString(logger, null));
    }

    public Bucket modifyBucket(BucketDto bucketDto) throws ItemDoNotExistsException, BucketAlreadyExistsException, IncorrectValueException, ExceededMaximumNumberOfCharactersException, DataAccessException {
        Bucket bucket = bucketRepository.getOne(bucketDto.getId());
        bucket.setName(bucketDto.getName());
        bucket.setDescription(bucketDto.getDescription());
        bucket.setIconName(bucketDto.getIconName());

        if (bucketDto.getDataClassId() != null)
            bucket.setDataClass(dataClassRepository.getOne(bucketDto.getDataClassId()));
        else
            bucket.setDataClass(null);

        if (bucketDto.getUsers() != null) {
            Set<User> users = new HashSet<>();
            for (long id : bucketDto.getUsers())
                users.add(userRepository.getOne(id));
            bucket.setUsers(users);
        } else
            bucket.setUsers(null);

        if (bucket.isHistory() != bucketDto.isHistory()) {
            if (bucketDto.isHistory()) {
                createAfterInsertTrigger(bucketDto.getName());
                createAfterUpdateTrigger(bucketDto.getName());
            } else {
                removeAfterInsertTrigger(bucketDto.getName());
                removeAfterUpdateTrigger(bucketDto.getName());
            }
        }

        return bucketRepository.save(bucket);
    }

    private void createBeforeDeleteTrigger(String bucketName) {
        removeBeforeDeleteTrigger(bucketName);

        String sql = "CREATE TRIGGER trigger_before_delete\n" +
                "BEFORE DELETE\n" +
                "ON \"" + bucketName + "\"" +
                "FOR EACH ROW\n" +
                "EXECUTE PROCEDURE before_delete()";

        logger.debug(sql);
        jdbcTemplate.getJdbcTemplate().execute(sql);
    }

    private void createAfterInsertTrigger(String bucketName) {
        removeAfterInsertTrigger(bucketName);

        String sql = "CREATE TRIGGER trigger_after_insert\n" +
                "AFTER INSERT\n" +
                "ON \"" + bucketName + "\"\n" +
                "FOR EACH ROW\n" +
                "EXECUTE PROCEDURE after_insert()";

        logger.debug(sql);
        jdbcTemplate.getJdbcTemplate().execute(sql);
    }

    private void createAfterUpdateTrigger(String bucketName) {
        removeAfterUpdateTrigger(bucketName);

        String sql = "CREATE TRIGGER trigger_after_update\n" +
                "AFTER UPDATE\n" +
                "ON \"" + bucketName + "\"\n" +
                "FOR EACH ROW\n" +
                "EXECUTE PROCEDURE after_update()";

        logger.debug(sql);
        jdbcTemplate.getJdbcTemplate().execute(sql);
    }

    private void removeBeforeDeleteTrigger(String bucketName) {
        String sql = "DROP TRIGGER IF EXISTS trigger_before_delete on \"" + bucketName + "\"";
        logger.debug(sql);
        jdbcTemplate.getJdbcTemplate().execute(sql);
    }

    private void removeAfterInsertTrigger(String bucketName) {
        String sql = "DROP TRIGGER IF EXISTS trigger_after_insert on \"" + bucketName + "\"";
        logger.debug(sql);
        jdbcTemplate.getJdbcTemplate().execute(sql);
    }

    private void removeAfterUpdateTrigger(String bucketName) {
        String sql = "DROP TRIGGER IF EXISTS trigger_after_update on \"" + bucketName + "\"";
        logger.debug(sql);
        jdbcTemplate.getJdbcTemplate().execute(sql);
    }

}