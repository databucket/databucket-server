package pl.databucket.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import pl.databucket.dto.DataClassDto;
import pl.databucket.entity.Bucket;
import pl.databucket.entity.DataClass;
import pl.databucket.exception.*;
import pl.databucket.repository.*;
import java.util.List;


@Service
public class DataClassService {

    @Autowired
    private DataClassRepository dataClassRepository;

    @Autowired
    private BucketRepository bucketRepository;

    @Autowired
    private TagRepository tagRepository;

    @Autowired
    private DataFilterRepository dataFilterRepository;

    @Autowired
    private DataColumnsRepository dataColumnsRepository;

    @Autowired
    private ViewRepository viewRepository;

    @Autowired
    private TaskRepository taskRepository;


    public DataClass createDataClass(DataClassDto dataClassDto) throws ItemAlreadyExistsException {
        if (dataClassRepository.existsByNameAndDeleted(dataClassDto.getName(), false))
            throw new ItemAlreadyExistsException(DataClass.class, dataClassDto.getName());

        DataClass dataClass = new DataClass();
        dataClass.setName(dataClassDto.getName());
        dataClass.setDescription(dataClassDto.getDescription());
        return dataClassRepository.save(dataClass);
    }

    public List<DataClass> getDataClasses() {
        return dataClassRepository.findAllByDeletedOrderById(false);
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

    public void deleteDataClass(long dataClassId) throws ItemNotFoundException, ItemAlreadyUsedException {
        DataClass dataClass = dataClassRepository.findByIdAndDeleted(dataClassId, false);

        if (dataClass == null)
            throw new ItemNotFoundException(DataClass.class, dataClassId);

        String usedByItems = "";
        List<Bucket> buckets = bucketRepository.findAllByDeletedAndDataClass(false, dataClass);
        for (Bucket bucket : buckets)
            usedByItems += " Bucket " + bucket.getName();

        //TODO sprawdzić dla reszty encji

        if (usedByItems.length() > 0)
            throw new ItemAlreadyUsedException("Class", dataClass.getName(), usedByItems);

        dataClass.setDeleted(true);
        dataClassRepository.save(dataClass);
    }

}
