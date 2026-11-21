package pl.databucket.server.audit;

import io.swagger.v3.oas.annotations.Hidden;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import pl.databucket.server.exception.ExceptionFormatter;
import pl.databucket.server.security.CustomUserDetails;

import java.time.Instant;
import java.time.format.DateTimeParseException;

/**
 * The changes of the settings: of the signed-in project (Settings > Activity, ADMIN) and of everything (Management >
 * Audit log, SUPER). For the web application.
 */
@Hidden
@CrossOrigin(origins = "*", allowedHeaders = "*")
@RestController
public class AuditController {

    private final ExceptionFormatter exceptionFormatter = new ExceptionFormatter(AuditController.class);

    private final AuditService auditService;

    public AuditController(AuditService auditService) {
        this.auditService = auditService;
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping(value = "/api/activity", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> getActivity(@RequestParam(required = false) String actor,
                                         @RequestParam(required = false) String type,
                                         @RequestParam(required = false) String action,
                                         @RequestParam(required = false) String from,
                                         @RequestParam(required = false) String to,
                                         @RequestParam(required = false, defaultValue = "1") int page,
                                         @RequestParam(required = false, defaultValue = "50") int limit) {
        Integer projectId = currentProject();
        if (projectId == null)
            return exceptionFormatter.customException("Sign in to a project", HttpStatus.BAD_REQUEST);
        return search(new FilterParams(projectId, false, actor, type, action, from, to), page, limit);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping(value = "/api/activity/export", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> exportActivity(@RequestParam(required = false) String actor,
                                            @RequestParam(required = false) String type,
                                            @RequestParam(required = false) String action,
                                            @RequestParam(required = false) String from,
                                            @RequestParam(required = false) String to) {
        Integer projectId = currentProject();
        if (projectId == null)
            return exceptionFormatter.customException("Sign in to a project", HttpStatus.BAD_REQUEST);
        return export(new FilterParams(projectId, false, actor, type, action, from, to));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping(value = "/api/activity/facets", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> getActivityFacets() {
        Integer projectId = currentProject();
        if (projectId == null)
            return exceptionFormatter.customException("Sign in to a project", HttpStatus.BAD_REQUEST);
        return new ResponseEntity<>(auditService.facets(projectId), HttpStatus.OK);
    }

    /** project: a project id, or 0 for the events outside projects (users); none = all. */
    @PreAuthorize("hasRole('SUPER')")
    @GetMapping(value = "/api/manage/audit", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> getAuditLog(@RequestParam(required = false) Integer project,
                                         @RequestParam(required = false) String actor,
                                         @RequestParam(required = false) String type,
                                         @RequestParam(required = false) String action,
                                         @RequestParam(required = false) String from,
                                         @RequestParam(required = false) String to,
                                         @RequestParam(required = false, defaultValue = "1") int page,
                                         @RequestParam(required = false, defaultValue = "50") int limit) {
        boolean outside = project != null && project == 0;
        return search(new FilterParams(outside ? null : project, outside, actor, type, action, from, to), page, limit);
    }

    @PreAuthorize("hasRole('SUPER')")
    @GetMapping(value = "/api/manage/audit/export", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> exportAuditLog(@RequestParam(required = false) Integer project,
                                            @RequestParam(required = false) String actor,
                                            @RequestParam(required = false) String type,
                                            @RequestParam(required = false) String action,
                                            @RequestParam(required = false) String from,
                                            @RequestParam(required = false) String to) {
        boolean outside = project != null && project == 0;
        return export(new FilterParams(outside ? null : project, outside, actor, type, action, from, to));
    }

    @PreAuthorize("hasRole('SUPER')")
    @GetMapping(value = "/api/manage/audit/facets", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> getAuditFacets() {
        return new ResponseEntity<>(auditService.facets(null), HttpStatus.OK);
    }

    private record FilterParams(Integer projectId, boolean outside, String actor, String type, String action,
                                String from, String to) {
    }

    private ResponseEntity<?> search(FilterParams params, int page, int limit) {
        try {
            AuditService.Filter filter = new AuditService.Filter(params.projectId(), params.outside(), params.actor(),
                    params.type(), params.action(), instant(params.from()), instant(params.to()));
            return new ResponseEntity<>(auditService.search(filter, page, limit), HttpStatus.OK);
        } catch (DateTimeParseException e) {
            return exceptionFormatter.customException("A date like 2026-11-21T10:00:00Z: " + e.getParsedString(), HttpStatus.BAD_REQUEST);
        } catch (Exception e) {
            return exceptionFormatter.defaultException(e);
        }
    }

    // the events for a CSV file, written by the web application (at most AuditService.MAX_EXPORT)
    private ResponseEntity<?> export(FilterParams params) {
        try {
            AuditService.Filter filter = new AuditService.Filter(params.projectId(), params.outside(), params.actor(),
                    params.type(), params.action(), instant(params.from()), instant(params.to()));
            return new ResponseEntity<>(auditService.export(filter), HttpStatus.OK);
        } catch (DateTimeParseException e) {
            return exceptionFormatter.customException("A date like 2026-11-21T10:00:00Z: " + e.getParsedString(), HttpStatus.BAD_REQUEST);
        } catch (Exception e) {
            return exceptionFormatter.defaultException(e);
        }
    }

    private static Instant instant(String text) {
        return AuditService.instant(text);
    }

    private static Integer currentProject() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return principal instanceof CustomUserDetails details ? details.getProjectId() : null;
    }
}
