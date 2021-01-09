package pl.databucket.response;

import lombok.Getter;
import lombok.Setter;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import pl.databucket.dto.ViewDto;
import pl.databucket.entity.View;

import java.util.List;
import java.util.stream.Collectors;

@Getter
@Setter
public class ViewPageResponse extends BasePageResponse {
    private List<ViewDto> views;

    public ViewPageResponse(Page<View> page, ModelMapper modelMapper) {
        super(page);
        views = page.getContent().stream().map(item -> modelMapper.map(item, ViewDto.class)).collect(Collectors.toList());
    }
}
