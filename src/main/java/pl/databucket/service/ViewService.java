package pl.databucket.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import pl.databucket.dto.ViewDto;
import pl.databucket.entity.*;
import pl.databucket.exception.ItemNotFoundException;
import pl.databucket.exception.ModifyByNullEntityIdException;
import pl.databucket.repository.*;

import java.util.List;


@Service
public class ViewService {

    @Autowired
    private BucketRepository bucketRepository;

    @Autowired
    private DataClassRepository dataClassRepository;

    @Autowired
    private DataColumnsRepository columnsRepository;

    @Autowired
    private DataFilterRepository dataFilterRepository;

    @Autowired
    private ViewRepository viewRepository;

    public View createView(ViewDto viewDto) throws ItemNotFoundException {
        View view = new View();
        view.setName(viewDto.getName());
        view.setDescription(viewDto.getDescription());
        view.setPrivateItem(viewDto.isPrivateItem());

        DataColumns dataColumns = columnsRepository.findByIdAndDeleted(viewDto.getColumnsId(), false);
        if (dataColumns != null)
            view.setDataColumns(dataColumns);
        else
            throw new ItemNotFoundException(DataColumns.class, viewDto.getColumnsId());

        if (viewDto.getFilterId() != null) {
            DataFilter dataFilter = dataFilterRepository.findByIdAndDeleted(viewDto.getFilterId(), false);
            if (dataFilter != null)
                view.setDataFilter(dataFilter);
            else
                throw new ItemNotFoundException(DataFilter.class, viewDto.getFilterId());
        }

        if (viewDto.getBucketId() != null) {
            Bucket bucket = bucketRepository.findByIdAndDeleted(viewDto.getBucketId(), false);
            if (bucket != null)
                view.setBucket(bucket);
            else
                throw new ItemNotFoundException(Bucket.class, viewDto.getBucketId());
        }

        if (viewDto.getDataClassId() != null) {
            DataClass dataClass = dataClassRepository.findByIdAndDeleted(viewDto.getDataClassId(), false);
            if (dataClass != null)
                view.setDataClass(dataClass);
            else
                throw new ItemNotFoundException(DataClass.class, viewDto.getDataClassId());
        }

        return viewRepository.save(view);
    }

    public List<View> getViews() {
        return viewRepository.findAllByDeletedOrderById(false);
    }

    public View modifyView(ViewDto viewDto) throws ItemNotFoundException, ModifyByNullEntityIdException {
        if (viewDto.getId() == null)
            throw new ModifyByNullEntityIdException(View.class);

        View view = viewRepository.findByIdAndDeleted(viewDto.getId(), false);

        if (view == null)
            throw new ItemNotFoundException(View.class, viewDto.getId());

        view.setName(viewDto.getName());
        view.setDescription(viewDto.getDescription());
        view.setPrivateItem(viewDto.isPrivateItem());

        DataColumns dataColumns = columnsRepository.findByIdAndDeleted(viewDto.getColumnsId(), false);
        if (dataColumns != null)
            view.setDataColumns(dataColumns);
        else
            throw new ItemNotFoundException(DataColumns.class, viewDto.getColumnsId());

        if (viewDto.getFilterId() != null) {
            DataFilter dataFilter = dataFilterRepository.findByIdAndDeleted(viewDto.getFilterId(), false);
            if (dataFilter != null)
                view.setDataFilter(dataFilter);
            else
                throw new ItemNotFoundException(DataFilter.class, viewDto.getFilterId());
        } else
            view.setDataFilter(null);

        if (viewDto.getBucketId() != null) {
            Bucket bucket = bucketRepository.findByIdAndDeleted(viewDto.getBucketId(), false);
            if (bucket != null)
                view.setBucket(bucket);
            else
                throw new ItemNotFoundException(Bucket.class, viewDto.getBucketId());
        } else
            view.setBucket(null);

        if (viewDto.getDataClassId() != null) {
            DataClass dataClass = dataClassRepository.findByIdAndDeleted(viewDto.getDataClassId(), false);
            if (dataClass != null)
                view.setDataClass(dataClass);
            else
                throw new ItemNotFoundException(DataClass.class, viewDto.getDataClassId());
        } else
            view.setDataClass(null);

        return viewRepository.save(view);
    }

    public void deleteView(long viewId) throws ItemNotFoundException {
        View view = viewRepository.findByIdAndDeleted(viewId, false);

        if (view == null)
            throw new ItemNotFoundException(View.class, viewId);

        view.setDeleted(true);
        viewRepository.save(view);
    }
}
