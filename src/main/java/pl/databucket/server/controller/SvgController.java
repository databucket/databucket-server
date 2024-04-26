package pl.databucket.server.controller;

import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import pl.databucket.server.dto.SvgDto;
import pl.databucket.server.entity.Svg;
import pl.databucket.server.exception.ExceptionFormatter;
import pl.databucket.server.exception.ItemAlreadyExistsException;
import pl.databucket.server.exception.ModifyByNullEntityIdException;
import pl.databucket.server.service.SvgService;

import javax.validation.Valid;
import java.util.List;
import java.util.stream.Collectors;

@CrossOrigin(origins = "*", allowedHeaders = "*")
@RequestMapping("/api/svg")
@RestController
public class SvgController {

    private final ExceptionFormatter exceptionFormatter = new ExceptionFormatter(SvgController.class);

    @Autowired
    private SvgService svgService;

    @Autowired
    private ModelMapper modelMapper;


    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ResponseEntity<?> createSvg(@Valid @RequestBody SvgDto svgDto) {
        try {
            Svg svg = svgService.createSvg(svgDto);
            modelMapper.map(svg, svgDto);
            return new ResponseEntity<>(svgDto, HttpStatus.CREATED);
        } catch (ItemAlreadyExistsException e) {
            return exceptionFormatter.customException(e, HttpStatus.NOT_ACCEPTABLE);
        } catch (Exception e) {
            return exceptionFormatter.defaultException(e);
        }
    }

    @GetMapping
    public ResponseEntity<?> getSvgList() {
        try {
            List<Svg> svgList = svgService.getSvgList();
            List<SvgDto> svgDtoList = svgList.stream().map(item -> modelMapper.map(item, SvgDto.class)).collect(Collectors.toList());
            return new ResponseEntity<>(svgDtoList, HttpStatus.OK);
        } catch (Exception ee) {
            return exceptionFormatter.defaultException(ee);
        }
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping
    public ResponseEntity<?> modifySvg(@Valid @RequestBody SvgDto svgDto) {
        try {
            Svg svg = svgService.modifySvg(svgDto);
            modelMapper.map(svg, svgDto);
            return new ResponseEntity<>(svgDto, HttpStatus.OK);
        } catch (ModifyByNullEntityIdException e) {
            return exceptionFormatter.customException(e, HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            return exceptionFormatter.defaultException(e);
        }
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping(value = "/{svgId}")
    public ResponseEntity<?> deleteSvg(@PathVariable long svgId) {
        try {
            svgService.deleteSvg(svgId);
            return new ResponseEntity<>(null, HttpStatus.NO_CONTENT);
        } catch (Exception e) {
            return exceptionFormatter.defaultException(e);
        }
    }
}
