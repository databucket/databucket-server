package pl.databucket.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import pl.databucket.exception.*;
import pl.databucket.dto.DataColumnsDto;
import pl.databucket.entity.DataColumns;
import pl.databucket.repository.DataColumnsRepository;

import java.util.List;


@Service
public class DataColumnsService {

    @Autowired
    private DataColumnsRepository columnsRepository;

    public DataColumns createColumns(DataColumnsDto dataColumnsDto) throws ItemNotFoundException {
        DataColumns dataColumns = new DataColumns();
        dataColumns.setName(dataColumnsDto.getName());
        dataColumns.setDescription(dataColumnsDto.getDescription());
        dataColumns.setConfiguration(dataColumnsDto.getConfiguration());

        return columnsRepository.save(dataColumns);
    }

    public List<DataColumns> getColumns() {
        return columnsRepository.findAllByDeletedOrderById(false);
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

        return columnsRepository.save(dataColumns);
    }

    public void deleteColumns(long columnsId) throws ItemNotFoundException {
        DataColumns columns = columnsRepository.findByIdAndDeleted(columnsId, false);

        if (columns == null)
            throw new ItemNotFoundException(DataColumns.class, columnsId);

        columns.setDeleted(true);
        columnsRepository.save(columns);
    }

}
