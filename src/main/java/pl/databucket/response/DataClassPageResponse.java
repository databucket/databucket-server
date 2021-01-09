package pl.databucket.response;

import lombok.Getter;
import lombok.Setter;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import pl.databucket.dto.DataClassDto;
import pl.databucket.entity.DataClass;

import java.util.List;
import java.util.stream.Collectors;

@Getter
@Setter
public class DataClassPageResponse extends BasePageResponse {
    private List<DataClassDto> dataClasses;

    public DataClassPageResponse(Page<DataClass> page, ModelMapper modelMapper) {
        super(page);
        dataClasses = page.getContent().stream().map(item -> modelMapper.map(item, DataClassDto.class)).collect(Collectors.toList());
    }
}
