package pl.databucket.response;

import lombok.Getter;
import lombok.Setter;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import pl.databucket.dto.DataColumnsDto;
import pl.databucket.entity.DataColumns;

import java.util.List;
import java.util.stream.Collectors;

@Getter
@Setter
public class DataColumnsPageResponse extends BasePageResponse {
    private List<DataColumnsDto> columns;

    public DataColumnsPageResponse(Page<DataColumns> page, ModelMapper modelMapper) {
        super(page);
        columns = page.getContent().stream().map(item -> modelMapper.map(item, DataColumnsDto.class)).collect(Collectors.toList());
    }
}
