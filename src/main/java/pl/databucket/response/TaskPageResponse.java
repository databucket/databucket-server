package pl.databucket.response;

import lombok.Getter;
import lombok.Setter;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import pl.databucket.dto.TaskDto;
import pl.databucket.entity.Task;

import java.util.List;
import java.util.stream.Collectors;

@Getter
@Setter
public class TaskPageResponse extends BasePageResponse {
    private List<TaskDto> tasks;

    public TaskPageResponse(Page<Task> page, ModelMapper modelMapper) {
        super(page);
        tasks = page.getContent().stream().map(item -> modelMapper.map(item, TaskDto.class)).collect(Collectors.toList());
    }
}
