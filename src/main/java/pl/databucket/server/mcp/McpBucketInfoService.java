package pl.databucket.server.mcp;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.databucket.server.dto.DataClassItemDto;
import pl.databucket.server.dto.DataEnumItemDto;
import pl.databucket.server.entity.Bucket;
import pl.databucket.server.entity.DataClass;
import pl.databucket.server.entity.DataEnum;
import pl.databucket.server.entity.User;
import pl.databucket.server.repository.DataEnumRepository;
import pl.databucket.server.repository.TagRepository;
import pl.databucket.server.service.BucketService;
import pl.databucket.server.service.UserService;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * The buckets of the signed-in user and what an agent needs to know about one of them. Each method runs in one
 * read-only transaction: the access checks compare the user and the buckets by identity and walk lazy relations.
 */
@Service
public class McpBucketInfoService {

    @Autowired
    private UserService userService;

    @Autowired
    private BucketService bucketService;

    @Autowired
    private TagRepository tagRepository;

    @Autowired
    private DataEnumRepository dataEnumRepository;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record BucketSummary(String name, String description, String dataClass, boolean history, boolean protectedData) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record Field(String title, String path, String type, List<String> values) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record TagInfo(long id, String name, String description) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record BucketDescription(String name, String description, boolean history, boolean protectedData,
                                    String dataClass, String dataClassDescription, List<Field> fields, List<TagInfo> tags) {
    }

    @Transactional(readOnly = true)
    public List<BucketSummary> listBuckets() {
        User user = userService.getCurrentUser();
        return bucketService.getAccessTreeBuckets(user).stream()
                .map(bucket -> new BucketSummary(bucket.getName(), bucket.getDescription(),
                        bucket.getDataClass() != null ? bucket.getDataClass().getName() : null,
                        bucket.isHistory(), bucket.isProtectedData()))
                .toList();
    }

    /** @throws IllegalArgumentException when the bucket doesn't exist or the user has no access to it */
    @Transactional(readOnly = true)
    public BucketDescription describeBucket(String bucketName) {
        User user = userService.getCurrentUser();
        Bucket bucket = bucketService.getBucket(bucketName);
        if (bucket == null || !bucketService.hasUserAccessToBucket(bucket, user))
            throw McpTools.noBucket(bucketName);

        DataClass dataClass = bucket.getDataClass();
        List<Field> fields = new ArrayList<>();
        if (dataClass != null && dataClass.getConfiguration() != null) {
            List<Integer> enumIds = dataClass.getConfiguration().stream()
                    .map(DataClassItemDto::getEnumId).filter(Objects::nonNull).distinct().toList();
            Map<Integer, DataEnum> enums = enumIds.isEmpty() ? Map.of()
                    : dataEnumRepository.findAllByDeletedAndIdIn(false, enumIds).stream()
                    .collect(Collectors.toMap(DataEnum::getId, Function.identity()));
            for (DataClassItemDto item : dataClass.getConfiguration()) {
                DataEnum dataEnum = item.getEnumId() != null ? enums.get(item.getEnumId()) : null;
                List<String> values = dataEnum != null && dataEnum.getItems() != null
                        ? dataEnum.getItems().stream().map(DataEnumItemDto::getValue).toList() : null;
                fields.add(new Field(item.getTitle(), item.getPath(), item.getType(), values));
            }
        }

        // the tags of the bucket or of its class, as in the bucket's data table
        List<TagInfo> tags = tagRepository.findAllByDeletedOrderById(false).stream()
                .filter(tag -> (tag.getBuckets() != null && tag.getBuckets().contains(bucket))
                        || (dataClass != null && tag.getDataClasses() != null && tag.getDataClasses().contains(dataClass)))
                .map(tag -> new TagInfo(tag.getId(), tag.getName(), tag.getDescription()))
                .toList();

        return new BucketDescription(bucket.getName(), bucket.getDescription(), bucket.isHistory(), bucket.isProtectedData(),
                dataClass != null ? dataClass.getName() : null, dataClass != null ? dataClass.getDescription() : null,
                fields, tags);
    }
}
