package pl.databucket.response;

import lombok.Getter;
import lombok.Setter;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import pl.databucket.dto.UserDtoResponse;
import pl.databucket.entity.User;

import java.util.List;
import java.util.stream.Collectors;

@Getter
@Setter
public class UserPageResponse extends BasePageResponse {
    private List<UserDtoResponse> users;

    public UserPageResponse(Page<User> page, ModelMapper modelMapper) {
        super(page);
        users = page.getContent().stream().map(item -> modelMapper.map(item, UserDtoResponse.class)).collect(Collectors.toList());
    }
}
