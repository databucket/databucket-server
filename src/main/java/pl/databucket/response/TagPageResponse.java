package pl.databucket.response;

import lombok.Getter;
import lombok.Setter;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import pl.databucket.dto.TagDto;
import pl.databucket.entity.Tag;

import java.util.List;
import java.util.stream.Collectors;

@Getter
@Setter
public class TagPageResponse extends BasePageResponse {
    private List<TagDto> tags;

    public TagPageResponse(Page<Tag> page, ModelMapper modelMapper) {
        super(page);
        tags = page.getContent().stream().map(item -> modelMapper.map(item, TagDto.class)).collect(Collectors.toList());
    }
}
