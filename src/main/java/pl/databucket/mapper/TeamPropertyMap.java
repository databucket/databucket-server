package pl.databucket.mapper;

import org.modelmapper.PropertyMap;
import pl.databucket.dto.TeamDto;
import pl.databucket.entity.Team;

public class TeamPropertyMap extends PropertyMap<Team, TeamDto> {

    @Override
    protected void configure() {
        map().setId(source.getId());
        map().setName(source.getName());
        map().setDescription(source.getDescription());
        map().setUsersIds(source.getUsersIds());

        map().setCreatedBy(source.getCreatedBy());
        map().setCreatedDate(source.getCreatedDate());
        map().setLastModifiedBy(source.getLastModifiedBy());
        map().setLastModifiedDate(source.getLastModifiedDate());
    }
}