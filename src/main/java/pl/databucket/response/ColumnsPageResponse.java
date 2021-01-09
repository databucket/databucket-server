package pl.databucket.response;

import lombok.Getter;
import lombok.Setter;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import pl.databucket.dto.ColumnsDto;
import pl.databucket.entity.Columns;

import java.util.List;
import java.util.stream.Collectors;

@Getter
@Setter
public class ColumnsPageResponse extends BasePageResponse {
    private List<ColumnsDto> columns;

    public ColumnsPageResponse(Page<Columns> page, ModelMapper modelMapper) {
        super(page);
        columns = page.getContent().stream().map(item -> modelMapper.map(item, ColumnsDto.class)).collect(Collectors.toList());
    }
}
