package pl.databucket.server.controller;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pl.databucket.server.dto.RoleDto;
import pl.databucket.server.entity.Role;
import pl.databucket.server.exception.ExceptionFormatter;
import pl.databucket.server.repository.RoleRepository;

@PreAuthorize("hasAnyRole('SUPER', 'ADMIN', 'MEMBER')")
@CrossOrigin(origins = "*", maxAge = 3600)
@RequestMapping("/api/roles")
@RestController
@RequiredArgsConstructor
public class RolesController {

    private final RoleRepository roleRepository;
    private final ModelMapper modelMapper;

    private final ExceptionFormatter exceptionFormatter = new ExceptionFormatter(RolesController.class);

    @GetMapping
    public ResponseEntity<?> getRoles() {
        try {
            List<Role> roles = roleRepository.findAllByOrderByIdAsc();
            List<RoleDto> rolesDto = roles.stream().map(item -> modelMapper.map(item, RoleDto.class)).toList();
            return ResponseEntity.ok(rolesDto);
        } catch (IllegalArgumentException e1) {
            return exceptionFormatter.customException(e1, HttpStatus.NOT_ACCEPTABLE);
        }
    }

}
