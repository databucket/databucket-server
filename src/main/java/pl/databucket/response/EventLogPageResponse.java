package pl.databucket.response;

import lombok.Getter;
import lombok.Setter;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import pl.databucket.dto.EventLogDto;
import pl.databucket.entity.EventLog;

import java.util.List;
import java.util.stream.Collectors;

@Getter
@Setter
public class EventLogPageResponse extends BasePageResponse {
    private List<EventLogDto> eventsLog;

    public EventLogPageResponse(Page<EventLog> page, ModelMapper modelMapper) {
        super(page);
        eventsLog = page.getContent().stream().map(item -> modelMapper.map(item, EventLogDto.class)).collect(Collectors.toList());
    }
}
