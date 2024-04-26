package pl.databucket.server.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import pl.databucket.server.dto.DataEnumDto;
import pl.databucket.server.entity.DataColumns;
import pl.databucket.server.entity.DataEnum;
import pl.databucket.server.entity.DataFilter;
import pl.databucket.server.entity.Task;
import pl.databucket.server.exception.ItemAlreadyExistsException;
import pl.databucket.server.exception.ItemAlreadyUsedException;
import pl.databucket.server.exception.ItemNotFoundException;
import pl.databucket.server.exception.ModifyByNullEntityIdException;
import pl.databucket.server.repository.DataColumnsRepository;
import pl.databucket.server.repository.DataEnumRepository;
import pl.databucket.server.repository.DataFilterRepository;
import pl.databucket.server.repository.TaskRepository;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


@Service
public class DataEnumService {

    @Autowired
    private DataEnumRepository dataEnumRepository;

    @Autowired
    private DataColumnsRepository dataColumnsRepository;

    @Autowired
    private DataFilterRepository dataFilterRepository;

    @Autowired
    private TaskRepository taskRepository;


    public DataEnum createDataEnum(DataEnumDto dataEnumDto) throws ItemAlreadyExistsException {
        if (dataEnumRepository.existsByNameAndDeleted(dataEnumDto.getName(), false))
            throw new ItemAlreadyExistsException(DataEnum.class, dataEnumDto.getName());

        DataEnum dataEnum = new DataEnum();
        dataEnum.setName(dataEnumDto.getName());
        dataEnum.setDescription(dataEnumDto.getDescription());
        dataEnum.setIconsEnabled(dataEnumDto.isIconsEnabled());
        dataEnum.setItems(dataEnumDto.getItems());
        return dataEnumRepository.save(dataEnum);
    }

    public List<DataEnum> getDataEnums() {
        return dataEnumRepository.findAllByDeletedOrderById(false);
    }

    public DataEnum modifyDataEnum(DataEnumDto dataEnumDto) throws ItemNotFoundException, ItemAlreadyExistsException, ModifyByNullEntityIdException {
        if (dataEnumDto.getId() == null)
            throw new ModifyByNullEntityIdException(DataEnum.class);

        DataEnum dataEnum = dataEnumRepository.findByIdAndDeleted(dataEnumDto.getId(), false);

        if (dataEnum == null)
            throw new ItemNotFoundException(DataEnum.class, dataEnumDto.getId());

        if (!dataEnum.getName().equals(dataEnumDto.getName()))
            if (dataEnumRepository.existsByNameAndDeleted(dataEnumDto.getName(), false))
                throw new ItemAlreadyExistsException(DataEnum.class, dataEnumDto.getName());

        dataEnum.setName(dataEnumDto.getName());
        dataEnum.setDescription(dataEnumDto.getDescription());
        dataEnum.setIconsEnabled(dataEnumDto.isIconsEnabled());
        dataEnum.setItems(dataEnumDto.getItems());
        return dataEnumRepository.save(dataEnum);
    }

    public void deleteDataEnum(int dataEnumId) throws ItemNotFoundException, ItemAlreadyUsedException {
        DataEnum dataEnum = dataEnumRepository.findByIdAndDeleted(dataEnumId, false);

        if (dataEnum == null)
            throw new ItemNotFoundException(DataEnum.class, dataEnumId);

        Map<String, List<String>> usedByItems = new HashMap<>();

        List<DataColumns> dataColumnsList = dataColumnsRepository.findAllByDeletedOrderById(false);
        List<String> columnsNames = new ArrayList<>();
        dataColumnsList.forEach(dataColumns -> dataColumns.getConfiguration().getProperties().forEach(property -> {
            if (property.containsKey("enumId") && property.get("enumId") != null && (int) property.get("enumId") == dataEnumId)
                columnsNames.add(dataColumns.getName());
        }));

        if (columnsNames.size() > 0)
            usedByItems.put("columns", columnsNames);

        List<DataFilter> dataFilterList = dataFilterRepository.findAllByDeletedOrderById(false);
        List<String> filterNames = new ArrayList<>();
        dataFilterList.forEach(dataFilter -> dataFilter.getConfiguration().getProperties().forEach(property -> {
            if (property.containsKey("enumId") && property.get("enumId") != null && (int) property.get("enumId") == dataEnumId)
                filterNames.add(dataFilter.getName());
        }));

        if (filterNames.size() > 0)
            usedByItems.put("filters", filterNames);

        List<Task> taskList = taskRepository.findAllByDeletedOrderById(false);
        List<String> taskNames = new ArrayList<>();
        taskList.forEach(task -> task.getConfiguration().getProperties().forEach(property -> {
            if (property.containsKey("enumId") && property.get("enumId") != null && (int) property.get("enumId") == dataEnumId)
                taskNames.add(task.getName());
        }));

        if (taskNames.size() > 0)
            usedByItems.put("tasks", taskNames);

        if (usedByItems.size() > 0)
            throw new ItemAlreadyUsedException("Enum", dataEnum.getName(), usedByItems.toString());

        dataEnum.setDeleted(true);
        dataEnumRepository.save(dataEnum);
    }

}
