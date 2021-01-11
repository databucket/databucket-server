package pl.databucket.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import pl.databucket.dto.DataClassDto;
import pl.databucket.entity.DataClass;
import pl.databucket.exception.*;
import pl.databucket.repository.DataClassRepository;


@Service
public class DataClassService {

    @Autowired
    private DataClassRepository dataClassRepository;

    public DataClass createDataClass(DataClassDto dataClassDto) throws ItemAlreadyExistsException {
        if (dataClassRepository.existsByNameAndDeleted(dataClassDto.getName(), false))
            throw new ItemAlreadyExistsException(DataClass.class, dataClassDto.getName());

        DataClass dataClass = new DataClass();
        dataClass.setName(dataClassDto.getName());
        dataClass.setDescription(dataClassDto.getDescription());
        return dataClassRepository.save(dataClass);
    }

    public Page<DataClass> getDataClasses(Specification<DataClass> specification, Pageable pageable) {
        return dataClassRepository.findAll(specification, pageable);
    }

    public DataClass modifyDataClass(DataClassDto dataClassDto) throws ItemNotFoundException, ItemAlreadyExistsException, ModifyByNullEntityIdException {
        if (dataClassDto.getId() == null)
            throw new ModifyByNullEntityIdException(DataClass.class);

        DataClass dataClass = dataClassRepository.findByIdAndDeleted(dataClassDto.getId(), false);

        if (dataClass == null)
            throw new ItemNotFoundException(DataClass.class, dataClassDto.getId());

        if (!dataClass.getName().equals(dataClassDto.getName()))
            if (dataClassRepository.existsByNameAndDeleted(dataClassDto.getName(), false))
                throw new ItemAlreadyExistsException(DataClass.class, dataClassDto.getName());

        dataClass.setName(dataClassDto.getName());
        dataClass.setDescription(dataClassDto.getDescription());
        return dataClassRepository.save(dataClass);
    }

    public void deleteDataClass(long dataClassId) throws ItemNotFoundException {
        DataClass dataClass = dataClassRepository.findByIdAndDeleted(dataClassId, false);

        if (dataClass == null)
            throw new ItemNotFoundException(DataClass.class, dataClassId);

        dataClass.setDeleted(true);
        dataClassRepository.save(dataClass);
    }

}
