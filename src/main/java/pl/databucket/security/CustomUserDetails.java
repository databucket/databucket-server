package pl.databucket.security;

import lombok.Getter;
import lombok.Setter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import pl.databucket.dto.AuthProjectDto;

import java.util.Collection;
import java.util.List;
import java.util.Set;

@Getter
@Setter
public class CustomUserDetails implements UserDetails {

    private Collection<? extends GrantedAuthority> authorities;
    private String password;
    private String username;
    private boolean changePassword;
    private Integer projectId;
    private List<AuthProjectDto> projects;
    private Set<Long> groupsIds;
    private Set<Long> bucketsIds;
    private Boolean enabled;

    public CustomUserDetails(String username,
                             String password,
                             Collection<? extends GrantedAuthority> authorities,
                             Integer projectId,
                             List<AuthProjectDto> projects,
                             boolean changePassword,
                             boolean enabled) {

        this.username = username;
        this.password = password;
        this.authorities = authorities;
        this.projectId = projectId;
        this.projects = projects;
        this.changePassword = changePassword;
        this.enabled = enabled;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }
}
