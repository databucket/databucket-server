package pl.databucket.server.controller;

import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import pl.databucket.server.dto.RoleDto;
import pl.databucket.server.entity.Role;
import pl.databucket.server.exception.ExceptionFormatter;
import pl.databucket.server.repository.RoleRepository;
import java.util.List;
import java.util.stream.Collectors;

@PreAuthorize("hasAnyRole('SUPER', 'ADMIN', 'MEMBER')")
@CrossOrigin(origins = "*", maxAge = 3600)
@RequestMapping("/api/roles")
@RestController
public class RolesController {

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private ModelMapper modelMapper;

    private final ExceptionFormatter exceptionFormatter = new ExceptionFormatter(RolesController.class);

    @GetMapping
    public ResponseEntity<?> getRoles() {
        try {
            List<Role> roles = roleRepository.findAllByOrderByIdAsc();
            List<RoleDto> rolesDto = roles.stream().map(item -> modelMapper.map(item, RoleDto.class)).collect(Collectors.toList());
            return new ResponseEntity<>(rolesDto, HttpStatus.OK);
        } catch (IllegalArgumentException e1) {
            return exceptionFormatter.customException(e1, HttpStatus.NOT_ACCEPTABLE);
        }
    }

}
