package pl.databucket.service;

import org.springframework.security.core.context.SecurityContextHolder;
import pl.databucket.model.beans.RoleBean;
import pl.databucket.model.constants.RoleName;
import pl.databucket.model.entity.Role;
import pl.databucket.repository.role.RoleRepository;
import pl.databucket.repository.user.UserRepository;
import pl.databucket.model.entity.User;
import pl.databucket.model.beans.UserBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


@Service(value = "userService")
public class UserService implements UserDetailsService {
	
	@Autowired
	private UserRepository userRepository;

	@Autowired
	private RoleRepository roleRepository;

	@Autowired
	private BCryptPasswordEncoder bcryptEncoder;

	public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
		User user = userRepository.findByName(username);
		if(user == null){
			throw new UsernameNotFoundException("Invalid username or password.");
		}
		return new org.springframework.security.core.userdetails.User(user.getName(), user.getPassword(), getAuthority(user));
	}

	private Set<SimpleGrantedAuthority> getAuthority(User user) {
        Set<SimpleGrantedAuthority> authorities = new HashSet<>();
		user.getRoles().forEach(role -> {
            authorities.add(new SimpleGrantedAuthority("ROLE_" + role.getName()));
		});
		return authorities;
	}

	public List<User> findAll() {
		List<User> list = new ArrayList<>();
		userRepository.findAll().iterator().forEachRemaining(list::add);
		return list;
	}

	public void delete(long id) {
		userRepository.deleteById(id);
	}

	public User findByName(String name) {
		return userRepository.findByName(name);
	}

    public User save(UserBean userBean) {
		if (!userRepository.existsByName(userBean.getName())) {
			User newUser = new User();
			newUser.setName(userBean.getName());
			newUser.setPassword(bcryptEncoder.encode(userBean.getPassword()));
			return userRepository.save(newUser);
		} else
			throw new IllegalArgumentException("The given user already exists.");
    }

    public User addRole(RoleBean roleBean) {
		User user = userRepository.findByName(roleBean.getUserName());
		if (user == null)
			throw new IllegalArgumentException("The given user has not been found!");

		Role role = roleRepository.findByName(roleBean.getRoleName());
		if (role == null)
			throw new IllegalArgumentException("The given role has not been found!");

		Set<Role> roles = user.getRoles();
		if (roles == null)
			roles = new HashSet<>();
		roles.add(role);
		return userRepository.save(user);
	}

	public User removeRole(RoleBean roleBean) {
		User user = userRepository.findByName(roleBean.getUserName());
		if (user == null)
			throw new IllegalArgumentException("The given user has not been found!");

		Role role = roleRepository.findByName(roleBean.getRoleName());
		if (role == null)
			throw new IllegalArgumentException("The given role has not been found!");

		Set<Role> roles = user.getRoles();
		if (roles == null)
			return user;

		roles.remove(role);
		return userRepository.save(user);
	}

	public User resetPassword(UserBean userBean) {
		User user = userRepository.findByName(userBean.getName());
		if (user != null) {
			user.setPassword(bcryptEncoder.encode(userBean.getPassword()));
			user.setChangePassword(true);
			return userRepository.save(user);
		} else
			throw new IllegalArgumentException("The given user does not exist.");
	}

	public User changePassword(UserBean userBean) {
		String name = SecurityContextHolder.getContext().getAuthentication().getName();
		if (!name.equals(userBean.getName())) {
			User user = userRepository.findByName(userBean.getName());
			if (user.getRoles().size() == 1 && user.getRoles().stream().noneMatch(role -> role.getName().equals(RoleName.ROBOT)))
				throw new IllegalArgumentException("You cannot change the password of another user!");
		}

		User user = userRepository.findByName(userBean.getName());
		if (user != null) {
			user.setPassword(bcryptEncoder.encode(userBean.getPassword()));
			user.setChangePassword(false);
			return userRepository.save(user);
		} else
			throw new IllegalArgumentException("The given user does not exist.");
	}
}
