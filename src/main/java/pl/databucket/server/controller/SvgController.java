package pl.databucket.server.controller;

import java.util.List;
import java.util.Map;
import javax.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pl.databucket.server.dto.SvgDto;
import pl.databucket.server.exception.ExceptionFormatter;
import pl.databucket.server.exception.ItemNotFoundException;
import pl.databucket.server.exception.ModifyByNullEntityIdException;
import pl.databucket.server.service.SvgService;

@RequestMapping("/api/svg")
@RestController
@RequiredArgsConstructor
public class SvgController {

    private final ExceptionFormatter exceptionFormatter = new ExceptionFormatter(SvgController.class);

    private final SvgService svgService;

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ResponseEntity<SvgDto> createSvg(@Valid @RequestBody SvgDto svgDto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(svgService.createSvg(svgDto));
    }

    @GetMapping
    public ResponseEntity<List<SvgDto>> getSvgList() {
        List<SvgDto> svgList = svgService.getSvgList();
        return ResponseEntity.ok(svgList);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping
    public ResponseEntity<SvgDto> modifySvg(@Valid @RequestBody SvgDto svgDto) throws ModifyByNullEntityIdException {
        SvgDto svg = svgService.modifySvg(svgDto);
        return ResponseEntity.ok(svg);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping(value = "/{svgId}")
    public ResponseEntity<Void> deleteSvg(@PathVariable long svgId) throws ItemNotFoundException {
        svgService.deleteSvg(svgId);
        return ResponseEntity.noContent().build();
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleError(Exception ex) {
        return exceptionFormatter.defaultException(ex);
    }

}
