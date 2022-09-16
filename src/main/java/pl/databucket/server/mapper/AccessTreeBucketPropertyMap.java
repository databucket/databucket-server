package pl.databucket.server.mapper;

import org.modelmapper.PropertyMap;
import pl.databucket.server.dto.AccessTreeBucketDto;
import pl.databucket.server.entity.Bucket;

public class AccessTreeBucketPropertyMap extends PropertyMap<Bucket, AccessTreeBucketDto> {

    @Override
    protected void configure() {
        map().setId(source.getId());
        map().setName(source.getName());
        map().setIconName(source.getIconName());
        map().setIconColor(source.getIconColor());
        map().setIconSvg(source.getIconSvg());
        map().setClassId(source.getDataClass().getId());
    }
}