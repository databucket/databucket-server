package pl.databucket.response;

import lombok.Getter;
import lombok.Setter;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import pl.databucket.dto.EventDto;
import pl.databucket.dto.TagDto;
import pl.databucket.entity.Event;
import pl.databucket.entity.Tag;

import java.util.List;
import java.util.stream.Collectors;

@Getter
@Setter
public class EventPageResponse extends BasePageResponse {
    private List<EventDto> events;

    public EventPageResponse(Page<Event> page, ModelMapper modelMapper) {
        super(page);
        events = page.getContent().stream().map(item -> modelMapper.map(item, EventDto.class)).collect(Collectors.toList());
    }
}
