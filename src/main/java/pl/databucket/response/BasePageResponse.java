package pl.databucket.response;

import lombok.Getter;
import lombok.Setter;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import pl.databucket.dto.GroupDto;
import pl.databucket.entity.Group;

import java.util.List;
import java.util.stream.Collectors;

@Getter
@Setter
public class BasePageResponse {
    private long page;
    private int size;
    private long total;
    private int totalPages;

    public BasePageResponse(Page<?> objPage) {
        total = objPage.getTotalElements();
        totalPages = objPage.getTotalPages();
        page = objPage.getNumber();
        size = objPage.getSize();
    }
}
