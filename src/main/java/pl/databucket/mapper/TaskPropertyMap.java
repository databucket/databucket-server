package pl.databucket.mapper;

import org.modelmapper.PropertyMap;
import pl.databucket.dto.TaskDto;
import pl.databucket.entity.Task;

public class TaskPropertyMap extends PropertyMap<Task, TaskDto> {

    @Override
    protected void configure() {
        map().setId(source.getId());
        map().setName(source.getName());
        map().setDescription(source.getDescription());
        map().setBuckets(source.getListOfBuckets());
        map().setDataClasses(source.getListOfDataClasses());
        map().setConfiguration(source.getConfiguration());

        map().setCreatedBy(source.getCreatedBy());
        map().setCreatedDate(source.getCreatedDate());
        map().setLastModifiedBy(source.getLastModifiedBy());
        map().setLastModifiedDate(source.getLastModifiedDate());
    }
}