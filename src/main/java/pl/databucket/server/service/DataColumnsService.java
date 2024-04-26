package pl.databucket.server.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import pl.databucket.server.dto.DataColumnsDto;
import pl.databucket.server.entity.DataClass;
import pl.databucket.server.entity.DataColumns;
import pl.databucket.server.entity.View;
import pl.databucket.server.exception.ItemAlreadyUsedException;
import pl.databucket.server.exception.ItemNotFoundException;
import pl.databucket.server.exception.ModifyByNullEntityIdException;
import pl.databucket.server.repository.DataClassRepository;
import pl.databucket.server.repository.DataColumnsRepository;
import pl.databucket.server.repository.ViewRepository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


@Service
public class DataColumnsService {

    @Autowired
    private DataColumnsRepository columnsRepository;

    @Autowired
    private DataClassRepository dataClassRepository;

    @Autowired
    private ViewRepository viewRepository;


    public DataColumns createColumns(DataColumnsDto dataColumnsDto) throws ItemNotFoundException {
        DataColumns dataColumns = new DataColumns();
        dataColumns.setName(dataColumnsDto.getName());
        dataColumns.setDescription(dataColumnsDto.getDescription());
        dataColumns.setConfiguration(dataColumnsDto.getConfiguration());

        if (dataColumnsDto.getClassId() != null) {
            DataClass dataClass = dataClassRepository.findByIdAndDeleted(dataColumnsDto.getClassId(), false);
            if (dataClass != null)
                dataColumns.setDataClass(dataClass);
            else
                throw new ItemNotFoundException(DataClass.class, dataColumnsDto.getClassId());
        }

        return columnsRepository.save(dataColumns);
    }

    public List<DataColumns> getColumns() {
        return columnsRepository.findAllByDeletedOrderById(false);
    }

    public List<DataColumns> getColumns(List<Long> ids) {
        return columnsRepository.findAllByDeletedAndIdIn(false, ids);
    }

    public DataColumns modifyColumns(DataColumnsDto dataColumnsDto) throws ItemNotFoundException, ModifyByNullEntityIdException {
        if (dataColumnsDto.getId() == null)
            throw new ModifyByNullEntityIdException(DataColumns.class);

        DataColumns dataColumns = columnsRepository.findByIdAndDeleted(dataColumnsDto.getId(), false);

        if (dataColumns == null)
            throw new ItemNotFoundException(DataColumns.class, dataColumnsDto.getId());

        dataColumns.setName(dataColumnsDto.getName());
        dataColumns.setDescription(dataColumnsDto.getDescription());
        dataColumns.setConfiguration(dataColumnsDto.getConfiguration());


        if (dataColumnsDto.getClassId() != null) {
            DataClass dataClass = dataClassRepository.findByIdAndDeleted(dataColumnsDto.getClassId(), false);
            if (dataClass != null)
                dataColumns.setDataClass(dataClass);
            else
                throw new ItemNotFoundException(DataClass.class, dataColumnsDto.getClassId());
        } else
            dataColumns.setDataClass(null);

        return columnsRepository.save(dataColumns);
    }

    public void deleteColumns(long columnsId) throws ItemNotFoundException, ItemAlreadyUsedException {
        DataColumns columns = columnsRepository.findByIdAndDeleted(columnsId, false);

        if (columns == null)
            throw new ItemNotFoundException(DataColumns.class, columnsId);

        Map<String, List<String>> usedByItems = new HashMap<>();
        List<View> views = viewRepository.findAllByDeletedAndDataColumns(false, columns);
        if (views.size() > 0)
            usedByItems.put("views", views.stream().map(View::getName).collect(Collectors.toList()));

        if (usedByItems.size() > 0)
            throw new ItemAlreadyUsedException("Column", columns.getName(), usedByItems.toString());

        columns.setDeleted(true);
        columnsRepository.save(columns);
    }

}
