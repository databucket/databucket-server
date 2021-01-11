package pl.databucket.mapper;

import org.modelmapper.PropertyMap;
import pl.databucket.dto.BucketDto;
import pl.databucket.entity.Bucket;

public class BucketPropertyMap extends PropertyMap<Bucket, BucketDto> {

    @Override
    protected void configure() {
        map().setId(source.getId());
        map().setName(source.getName());
        map().setDescription(source.getDescription());
        map().setHistory(source.isHistory());
        map().setHiddenData(source.isHiddenData());
        map().setIconName(source.getIconName());
        map().setDataClassId(source.getDataClass().getId());

        map().setCreatedBy(source.getCreatedBy());
        map().setCreatedDate(source.getCreatedDate());
        map().setLastModifiedBy(source.getLastModifiedBy());
        map().setLastModifiedDate(source.getLastModifiedDate());
    }
}