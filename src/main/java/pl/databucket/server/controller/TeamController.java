package pl.databucket.server.controller;

import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import pl.databucket.server.dto.TeamDto;
import pl.databucket.server.entity.Team;
import pl.databucket.server.exception.*;
import pl.databucket.server.service.TeamService;

import javax.validation.Valid;
import java.util.List;
import java.util.stream.Collectors;

@CrossOrigin(origins = "*", allowedHeaders = "*")
@PreAuthorize("hasRole('ADMIN')")
@RequestMapping("/api/teams")
@RestController
public class TeamController {

    @Autowired
    private TeamService teamService;

    @Autowired
    private ModelMapper modelMapper;

    private final ExceptionFormatter exceptionFormatter = new ExceptionFormatter(TeamController.class);

    @PostMapping
    public ResponseEntity<?> createTeam(@Valid @RequestBody TeamDto teamDto) {
        try {
            Team team = teamService.createTeam(teamDto);
            modelMapper.map(team, teamDto);
            return new ResponseEntity<>(teamDto, HttpStatus.CREATED);
        } catch (ItemAlreadyExistsException e) {
            return exceptionFormatter.customException(e, HttpStatus.NOT_ACCEPTABLE);
        } catch (SomeItemsNotFoundException e) {
            return exceptionFormatter.customException(e, HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            return exceptionFormatter.defaultException(e);
        }
    }

    @GetMapping
    public ResponseEntity<?> getTeams() {
        try {
            List<Team> teams = teamService.getTeams();
            List<TeamDto> teamsDto = teams.stream().map(item -> modelMapper.map(item, TeamDto.class)).collect(Collectors.toList());
            return new ResponseEntity<>(teamsDto, HttpStatus.OK);
        } catch (Exception ee) {
            return exceptionFormatter.defaultException(ee);
        }
    }

    @PutMapping
    public ResponseEntity<?> modifyTeam(@Valid @RequestBody TeamDto teamDto) {
        try {
            Team team = teamService.modifyTeam(teamDto);
            modelMapper.map(team, teamDto);
            return new ResponseEntity<>(teamDto, HttpStatus.OK);
        } catch (ItemNotFoundException | SomeItemsNotFoundException | ModifyByNullEntityIdException e) {
            return exceptionFormatter.customException(e, HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            return exceptionFormatter.defaultException(e);
        }
    }

    @DeleteMapping(value = "/{teamId}")
    public ResponseEntity<?> deleteTeam(@PathVariable("teamId") short teamId) {
        try {
            teamService.deleteTeam(teamId);
            return new ResponseEntity<>(null, HttpStatus.NO_CONTENT);
        } catch (ItemNotFoundException e) {
            return exceptionFormatter.customException(e, HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            return exceptionFormatter.defaultException(e);
        }
    }
}
