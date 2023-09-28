package pl.databucket.server.security;

import java.util.Collection;
import java.util.Set;
import lombok.Builder;
import lombok.Data;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import pl.databucket.server.entity.Project;

@Data
@Builder
public class CustomUserDetails implements UserDetails {

    private Collection<? extends GrantedAuthority> authorities;
    private String password;
    private String username;
    private Integer projectId;
    private boolean enabled;
    private boolean expired;
    private boolean superUser;
    private boolean changePassword;
    private Set<Project> projects;


    @Override
    public boolean isAccountNonExpired() {
        return !expired;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return !changePassword;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }
}
