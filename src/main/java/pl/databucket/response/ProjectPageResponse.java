package pl.databucket.response;

import lombok.Getter;
import lombok.Setter;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import pl.databucket.dto.ManageProjectDto;
import pl.databucket.entity.Project;

import java.util.List;
import java.util.stream.Collectors;

@Getter
@Setter
public class ProjectPageResponse extends BasePageResponse {
    private List<ManageProjectDto> projects;

    public ProjectPageResponse(Page<Project> page, ModelMapper modelMapper) {
        super(page);
        projects = page.getContent().stream().map(item -> modelMapper.map(item, ManageProjectDto.class)).collect(Collectors.toList());
    }
}
