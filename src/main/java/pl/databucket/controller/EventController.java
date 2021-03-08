package pl.databucket.controller;

import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pl.databucket.dto.EventDto;
import pl.databucket.entity.Event;
import pl.databucket.exception.ExceptionFormatter;
import pl.databucket.exception.ItemAlreadyExistsException;
import pl.databucket.exception.ModifyByNullEntityIdException;
import pl.databucket.service.EventService;

import javax.validation.Valid;
import java.util.List;
import java.util.stream.Collectors;

@CrossOrigin(origins = "*", allowedHeaders = "*")
@RequestMapping("/api/events")
@RestController
public class EventController {

  private final ExceptionFormatter exceptionFormatter = new ExceptionFormatter(EventController.class);

  @Autowired
  private EventService eventService;

  @Autowired
  private ModelMapper modelMapper;

  @PostMapping(produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<?> createEvent(@Valid @RequestBody EventDto eventDto) {
    try {
      Event event = eventService.createEvent(eventDto);
      modelMapper.map(event, eventDto);
      return new ResponseEntity<>(eventDto, HttpStatus.CREATED);
    } catch (Exception e) {
      return exceptionFormatter.defaultException(e);
    }
  }

  @GetMapping
  public ResponseEntity<?> getEvents() {
    try {
      List<Event> events = eventService.getEvents();
      List<EventDto> eventsDto = events.stream().map(item -> modelMapper.map(item, EventDto.class)).collect(Collectors.toList());
      return new ResponseEntity<>(eventsDto, HttpStatus.OK);
    } catch (Exception ee) {
      return exceptionFormatter.defaultException(ee);
    }
  }

  @PutMapping
  public ResponseEntity<?> modifyEvent(@Valid @RequestBody EventDto eventDto) {
    try {
      Event event = eventService.modifyEvent(eventDto);
      modelMapper.map(event, eventDto);
      return new ResponseEntity<>(eventDto, HttpStatus.OK);
    } catch (ItemAlreadyExistsException e1) {
      return exceptionFormatter.customException(e1, HttpStatus.NOT_ACCEPTABLE);
    } catch (ModifyByNullEntityIdException e) {
      return exceptionFormatter.customException(e, HttpStatus.NOT_FOUND);
    } catch (Exception e) {
      return exceptionFormatter.defaultException(e);
    }
  }

  @DeleteMapping(value = "/{eventId}", produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<?> deleteEvent(@PathVariable Long eventId) {
    try {
      eventService.deleteEvent(eventId);
      return new ResponseEntity<>(null, HttpStatus.NO_CONTENT);
    } catch (Exception e) {
      return exceptionFormatter.defaultException(e);
    }
  }

}
