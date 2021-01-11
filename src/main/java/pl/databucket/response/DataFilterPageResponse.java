package pl.databucket.response;

import lombok.Getter;
import lombok.Setter;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import pl.databucket.dto.DataFilterDto;
import pl.databucket.entity.DataFilter;

import java.util.List;
import java.util.stream.Collectors;

@Getter
@Setter
public class DataFilterPageResponse extends BasePageResponse {
    private List<DataFilterDto> dataFilters;

    public DataFilterPageResponse(Page<DataFilter> page, ModelMapper modelMapper) {
        super(page);
        dataFilters = page.getContent().stream().map(item -> modelMapper.map(item, DataFilterDto.class)).collect(Collectors.toList());
    }
}
