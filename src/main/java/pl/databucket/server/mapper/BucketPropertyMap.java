package pl.databucket.server.mapper;

import org.modelmapper.PropertyMap;
import pl.databucket.server.dto.BucketDto;
import pl.databucket.server.entity.Bucket;

public class BucketPropertyMap extends PropertyMap<Bucket, BucketDto> {

    @Override
    protected void configure() {
        map().setId(source.getId());
        map().setName(source.getName());
        map().setDescription(source.getDescription());
        map().setIcon(source.getIconDto());
        map().setClassId(source.getDataClass().getId());
        map().setGroupsIds(source.getGroupsIds());
        map().setUsersIds(source.getUsersIds());
        map().setHistory(source.isHistory());
        map().setProtectedData(source.isProtectedData());
        map().setRoleId(source.getRole().getId());
        map().setTeamsIds(source.getTeamsIds());

        map().setCreatedBy(source.getCreatedBy());
        map().setCreatedAt(source.getCreatedAt());
        map().setModifiedBy(source.getModifiedBy());
        map().setModifiedAt(source.getModifiedAt());
    }
}