package pl.databucket.server.mcp;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.databucket.server.configuration.Constants;
import pl.databucket.server.dto.BucketDto;
import pl.databucket.server.dto.DataClassItemDto;
import pl.databucket.server.dto.DataColumnsConfigDto;
import pl.databucket.server.dto.DataColumnsDto;
import pl.databucket.server.dto.DataEnumDto;
import pl.databucket.server.dto.DataEnumItemDto;
import pl.databucket.server.dto.DataClassDto;
import pl.databucket.server.dto.IndexedPropertyDto;
import pl.databucket.server.dto.ViewDto;
import pl.databucket.server.entity.DataClass;
import pl.databucket.server.entity.DataColumns;
import pl.databucket.server.entity.DataEnum;
import pl.databucket.server.entity.Role;
import pl.databucket.server.exception.ItemAlreadyExistsException;
import pl.databucket.server.exception.ItemNotFoundException;
import pl.databucket.server.repository.BucketRepository;
import pl.databucket.server.repository.DataClassRepository;
import pl.databucket.server.repository.DataEnumRepository;
import pl.databucket.server.repository.RoleRepository;
import pl.databucket.server.service.BucketIndexService;
import pl.databucket.server.service.BucketService;
import pl.databucket.server.service.DataClassService;
import pl.databucket.server.service.DataColumnsService;
import pl.databucket.server.service.DataEnumService;
import pl.databucket.server.service.ViewService;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.stream.IntStream;

/**
 * Creates a bucket ready to use, in one transaction: the enums of its enum fields, its data class, the bucket (with the
 * data and history tables), the columns and a view of the data table in the GUI. All are named after the bucket.
 */
@Service
public class McpBucketCreator {

    /** All the features of a view: search, details, creation, modifying, removal, history, tasks, reservation, ... */
    private static final Short[] ALL_FEATURES = IntStream.rangeClosed(1, 17).mapToObj(i -> (short) i).toArray(Short[]::new);

    private final BucketService bucketService;
    private final DataClassService dataClassService;
    private final DataEnumService dataEnumService;
    private final DataColumnsService dataColumnsService;
    private final ViewService viewService;
    private final BucketRepository bucketRepository;
    private final DataClassRepository dataClassRepository;
    private final DataEnumRepository dataEnumRepository;
    private final RoleRepository roleRepository;

    public McpBucketCreator(BucketService bucketService, DataClassService dataClassService, DataEnumService dataEnumService,
                            DataColumnsService dataColumnsService, ViewService viewService,
                            BucketRepository bucketRepository, DataClassRepository dataClassRepository,
                            DataEnumRepository dataEnumRepository, RoleRepository roleRepository) {
        this.bucketService = bucketService;
        this.dataClassService = dataClassService;
        this.dataEnumService = dataEnumService;
        this.dataColumnsService = dataColumnsService;
        this.viewService = viewService;
        this.bucketRepository = bucketRepository;
        this.dataClassRepository = dataClassRepository;
        this.dataEnumRepository = dataEnumRepository;
        this.roleRepository = roleRepository;
    }

    record Request(String name, String description, List<BucketSchema.Field> fields, boolean history,
                   boolean protectedData, List<IndexedPropertyDto> indexedProperties, String access) {
    }

    /** The names of what was created besides the bucket. */
    record Created(String dataClass, List<String> enums, String view) {
    }

    @Transactional(rollbackFor = Exception.class)
    public Created create(Request request) throws ItemAlreadyExistsException, ItemNotFoundException {
        String name = request.name() == null ? "" : request.name().trim();
        if (name.isEmpty() || name.length() > Constants.NAME_MAX)
            throw new IllegalArgumentException("The bucket name has 1.." + Constants.NAME_MAX + " characters");
        if (request.description() != null && request.description().length() > Constants.DESCRIPTION_MAX)
            throw new IllegalArgumentException("The description has at most " + Constants.DESCRIPTION_MAX + " characters");
        if (bucketRepository.existsByNameAndDeleted(name, false))
            throw new IllegalArgumentException("A bucket '" + name + "' already exists; choose another name");
        BucketIndexService.check(request.indexedProperties());
        Role role = role(request.access());

        // enums and the data class
        List<String> enums = new ArrayList<>();
        List<DataClassItemDto> items = new ArrayList<>();
        for (BucketSchema.Field field : request.fields()) {
            DataClassItemDto item = new DataClassItemDto();
            item.setUuid(UUID.randomUUID().toString());
            item.setPath(field.path());
            item.setTitle(field.title());
            item.setType(field.type());
            if (field.values() != null) {
                DataEnum dataEnum = dataEnumService.createDataEnum(enumDto(name, field));
                item.setEnumId(dataEnum.getId());
                enums.add(dataEnum.getName());
            }
            items.add(item);
        }
        DataClassDto classDto = new DataClassDto();
        classDto.setName(unique(name, n -> dataClassRepository.existsByNameAndDeleted(n, false)));
        classDto.setDescription("The data of the bucket " + name);
        classDto.setConfiguration(items);
        DataClass dataClass = dataClassService.createDataClass(classDto);

        // the bucket
        BucketDto bucketDto = new BucketDto();
        bucketDto.setName(name);
        bucketDto.setDescription(request.description());
        bucketDto.setHistory(request.history());
        bucketDto.setProtectedData(request.protectedData());
        bucketDto.setClassId(dataClass.getId());
        bucketDto.setRoleId(role.getId());
        bucketDto.setIndexedProperties(request.indexedProperties() == null || request.indexedProperties().isEmpty()
                ? null : request.indexedProperties());
        bucketDto = bucketService.createBucket(bucketDto);

        // columns and the view of the data table (the GUI needs MEMBER)
        DataColumnsDto columnsDto = new DataColumnsDto();
        columnsDto.setName(name);
        columnsDto.setDescription("All the fields of " + name);
        columnsDto.setClassId(dataClass.getId());
        columnsDto.setConfiguration(columns(items));
        DataColumns columns = dataColumnsService.createColumns(columnsDto);

        ViewDto viewDto = new ViewDto();
        viewDto.setName(name);
        viewDto.setDescription("All the data of " + name);
        viewDto.setColumnsId(columns.getId());
        viewDto.setBucketsIds(Set.of(bucketDto.getId()));
        viewDto.setRoleId(roleRepository.findByName(Constants.ROLE_MEMBER).getId());
        viewDto.setFeaturesIds(ALL_FEATURES);
        viewDto.setSort("desc(id)");
        viewService.createView(viewDto);

        return new Created(dataClass.getName(), enums, viewDto.getName());
    }

    private Role role(String access) {
        String name = access == null || access.isBlank() ? Constants.ROLE_AGENT : access.trim().toUpperCase();
        if (!List.of(Constants.ROLE_AGENT, Constants.ROLE_ROBOT, Constants.ROLE_MEMBER, Constants.ROLE_ADMIN).contains(name))
            throw new IllegalArgumentException("access is the lowest role that may use the bucket: AGENT, ROBOT, MEMBER or ADMIN");
        return roleRepository.findByName(name);
    }

    private DataEnumDto enumDto(String bucketName, BucketSchema.Field field) {
        DataEnumDto dto = new DataEnumDto();
        dto.setName(unique(bucketName + " " + field.title(), n -> dataEnumRepository.existsByNameAndDeleted(n, false)));
        dto.setDescription(field.title() + " in " + bucketName);
        dto.setItems(field.values().stream().map(value -> {
            DataEnumItemDto item = new DataEnumItemDto();
            item.setValue(value);
            item.setText(value);
            return item;
        }).toList());
        return dto;
    }

    private static DataColumnsConfigDto columns(List<DataClassItemDto> items) {
        List<Map<String, Object>> columns = new ArrayList<>();
        columns.add(column("uuid_data_id", "never"));
        columns.add(column("uuid_tag_id", "always"));
        columns.add(column("uuid_reserved", "always"));
        columns.add(column("uuid_owner", "never"));
        List<Map<String, Object>> properties = new ArrayList<>();
        for (DataClassItemDto item : items) {
            columns.add(column(item.getUuid(), "always"));
            Map<String, Object> property = new LinkedHashMap<>();
            property.put("path", item.getPath());
            property.put("type", item.getType());
            property.put("uuid", item.getUuid());
            property.put("title", item.getTitle());
            if (item.getEnumId() != null)
                property.put("enumId", item.getEnumId());
            properties.add(property);
        }
        columns.add(column("uuid_modified_at", "never"));
        columns.add(column("uuid_modified_by", "never"));
        DataColumnsConfigDto config = new DataColumnsConfigDto();
        config.setColumns(columns);
        config.setProperties(properties);
        return config;
    }

    private static Map<String, Object> column(String uuid, String editable) {
        Map<String, Object> column = new LinkedHashMap<>();
        column.put("uuid", uuid);
        column.put("enabled", true);
        column.put("sorting", true);
        column.put("filtering", true);
        column.put("editable", editable);
        return column;
    }

    /** The name, or with " 2", " 3"... when taken; at most NAME_MAX characters. */
    static String unique(String name, Predicate<String> taken) {
        String base = name.length() > Constants.NAME_MAX ? name.substring(0, Constants.NAME_MAX).trim() : name;
        String candidate = base;
        for (int i = 2; taken.test(candidate); i++) {
            String suffix = " " + i;
            candidate = (base.length() + suffix.length() > Constants.NAME_MAX
                    ? base.substring(0, Constants.NAME_MAX - suffix.length()).trim() : base) + suffix;
        }
        return candidate;
    }
}
