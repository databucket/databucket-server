package pl.databucket.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import pl.databucket.dto.DataColumnsItemDto;
import pl.databucket.dto.DataEnumDto;
import pl.databucket.entity.DataColumns;
import pl.databucket.entity.DataEnum;
import pl.databucket.exception.ItemAlreadyExistsException;
import pl.databucket.exception.ItemAlreadyUsedException;
import pl.databucket.exception.ItemNotFoundException;
import pl.databucket.exception.ModifyByNullEntityIdException;
import pl.databucket.repository.*;

import java.util.List;


@Service
public class DataEnumService {

    @Autowired
    private DataEnumRepository dataEnumRepository;

    @Autowired
    private DataColumnsRepository dataColumnsRepository;


    public DataEnum createDataEnum(DataEnumDto dataEnumDto) throws ItemAlreadyExistsException {
        if (dataEnumRepository.existsByNameAndDeleted(dataEnumDto.getName(), false))
            throw new ItemAlreadyExistsException(DataEnum.class, dataEnumDto.getName());

        DataEnum dataEnum = new DataEnum();
        dataEnum.setName(dataEnumDto.getName());
        dataEnum.setDescription(dataEnumDto.getDescription());
        dataEnum.setTextValues(dataEnumDto.isTextValues());
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
        dataEnum.setTextValues(dataEnumDto.isTextValues());
        dataEnum.setItems(dataEnumDto.getItems());
        return dataEnumRepository.save(dataEnum);
    }

    public void deleteDataEnum(int dataEnumId) throws ItemNotFoundException, ItemAlreadyUsedException {
        DataEnum dataEnum = dataEnumRepository.findByIdAndDeleted(dataEnumId, false);

        if (dataEnum == null)
            throw new ItemNotFoundException(DataEnum.class, dataEnumId);

        String usedByItems = "";
        List<DataColumns> dataColumnsList = dataColumnsRepository.findAllByDeletedOrderById(false);
        for (DataColumns columns : dataColumnsList)
            for (DataColumnsItemDto columnsConfig : columns.getConfiguration())
                if (columnsConfig.getEnumId() == dataEnumId)
                    usedByItems += " Columns: " + columns.getName() + " -> Column: " + columnsConfig.getTitle() + ", ";

        if (usedByItems.length() > 0)
            throw new ItemAlreadyUsedException("Columns", dataEnum.getName(), usedByItems);

        dataEnum.setDeleted(true);
        dataEnumRepository.save(dataEnum);
    }

}
