package pl.databucket.response;

import lombok.Getter;
import lombok.Setter;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import pl.databucket.dto.BucketDto;
import pl.databucket.entity.Bucket;

import java.util.List;
import java.util.stream.Collectors;

@Getter
@Setter
public class BucketPageResponse extends BasePageResponse {
    private List<BucketDto> buckets;

    public BucketPageResponse(Page<Bucket> page, ModelMapper modelMapper) {
        super(page);
        buckets = page.getContent().stream().map(item -> modelMapper.map(item, BucketDto.class)).collect(Collectors.toList());
    }
}
