package pl.databucket.controller;

import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pl.databucket.dto.DataEnumDto;
import pl.databucket.entity.DataEnum;
import pl.databucket.exception.*;
import pl.databucket.service.DataEnumService;

import javax.validation.Valid;
import java.util.List;
import java.util.stream.Collectors;

@CrossOrigin(origins = "*", allowedHeaders = "*")
@RequestMapping("/api/enums")
@RestController
public class DataEnumController {

    private final ExceptionFormatter exceptionFormatter = new ExceptionFormatter(DataEnumController.class);

    @Autowired
    private DataEnumService dataEnumService;

    @Autowired
    private ModelMapper modelMapper;

    @PostMapping
    public ResponseEntity<?> createDataEnum(@Valid @RequestBody DataEnumDto dataEnumDto) {
        try {
            DataEnum dataEnum = dataEnumService.createDataEnum(dataEnumDto);
            modelMapper.map(dataEnum, dataEnumDto);
            return new ResponseEntity<>(dataEnumDto, HttpStatus.CREATED);
        } catch (ItemAlreadyExistsException e) {
            return exceptionFormatter.customException(e, HttpStatus.NOT_ACCEPTABLE);
        } catch (Exception e) {
            return exceptionFormatter.defaultException(e);
        }
    }

    @GetMapping
    public ResponseEntity<?> getDataEnums() {
        try {
            List<DataEnum> dataEnums = dataEnumService.getDataEnums();
            List<DataEnumDto> dataEnumsDto = dataEnums.stream().map(item -> modelMapper.map(item, DataEnumDto.class)).collect(Collectors.toList());
            return new ResponseEntity<>(dataEnumsDto, HttpStatus.OK);
        } catch (Exception ee) {
            return exceptionFormatter.defaultException(ee);
        }
    }

    @PutMapping
    public ResponseEntity<?> modifyDataEnum(@Valid @RequestBody DataEnumDto dataEnumDto) {
        try {
            DataEnum dataEnum = dataEnumService.modifyDataEnum(dataEnumDto);
            modelMapper.map(dataEnum, dataEnumDto);
            return new ResponseEntity<>(dataEnumDto, HttpStatus.OK);
        } catch (ItemNotFoundException | ModifyByNullEntityIdException e) {
            return exceptionFormatter.customException(e, HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            return exceptionFormatter.defaultException(e);
        }
    }

    @DeleteMapping(value = "/{dataEnumId}")
    public ResponseEntity<?> deleteDataEnum(@PathVariable("dataEnumId") int dataEnumId) {
        try {
            dataEnumService.deleteDataEnum(dataEnumId);
            return new ResponseEntity<>(null, HttpStatus.NO_CONTENT);
        } catch (ItemNotFoundException e) {
            return exceptionFormatter.customException(e, HttpStatus.NOT_FOUND);
        } catch (ItemAlreadyUsedException e) {
            return exceptionFormatter.customException(e, HttpStatus.FORBIDDEN);
        } catch (Exception e) {
            return exceptionFormatter.defaultException(e);
        }
    }
}
