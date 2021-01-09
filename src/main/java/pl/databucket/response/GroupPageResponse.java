package pl.databucket.response;

import lombok.Getter;
import lombok.Setter;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import pl.databucket.dto.GroupDto;
import pl.databucket.entity.Group;

import java.util.List;
import java.util.stream.Collectors;

@Getter
@Setter
public class GroupPageResponse extends BasePageResponse {
    private List<GroupDto> groups;

    public GroupPageResponse(Page<Group> page, ModelMapper modelMapper) {
        super(page);
        groups = page.getContent().stream().map(item -> modelMapper.map(item, GroupDto.class)).collect(Collectors.toList());
    }
}
