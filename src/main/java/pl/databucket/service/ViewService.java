package pl.databucket.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import pl.databucket.dto.ViewDto;
import pl.databucket.entity.View;
import pl.databucket.repository.*;


@Service
public class ViewService {

    @Autowired
    private BucketRepository bucketRepository;

    @Autowired
    private DataClassRepository dataClassRepository;

    @Autowired
    private ColumnsRepository columnsRepository;

    @Autowired
    private FilterRepository filterRepository;

    @Autowired
    private ViewRepository viewRepository;

    public View createView(ViewDto viewDto) {
        View view = new View();
        view.setName(viewDto.getName());
        view.setDescription(viewDto.getDescription());
        view.setColumns(columnsRepository.getOne(viewDto.getColumnsId()));

        if (viewDto.getFilterId() != null)
            view.setFilter(filterRepository.getOne(viewDto.getFilterId()));

        if (viewDto.getBucketId() != null)
            view.setBucket(bucketRepository.getOne(viewDto.getBucketId()));

        if (viewDto.getDataClassId() != null)
            view.setDataClass(dataClassRepository.getOne(viewDto.getDataClassId()));

        return viewRepository.save(view);
    }

    public Page<View> getViews(Specification<View> specification, Pageable pageable) {
        return viewRepository.findAll(specification, pageable);
    }

    public void deleteView(long viewId) {
        View view = viewRepository.getOne(viewId);
        view.setDeleted(true);
        viewRepository.save(view);
    }

    public View modifyView(ViewDto viewDto) {
        View view = viewRepository.getOne(viewDto.getId());
        view.setName(viewDto.getName());
        view.setDescription(viewDto.getDescription());
        view.setColumns(columnsRepository.getOne(viewDto.getColumnsId()));
        if (viewDto.getFilterId() != null)
            view.setFilter(filterRepository.getOne(viewDto.getFilterId()));
        else
            view.setFilter(null);

        if (viewDto.getBucketId() != null)
            view.setBucket(bucketRepository.getOne(viewDto.getBucketId()));
        else
            view.setBucket(null);

        if (viewDto.getDataClassId() != null)
            view.setDataClass(dataClassRepository.getOne(viewDto.getDataClassId()));
        else
            view.setDataClass(null);

        return viewRepository.save(view);
    }
}
