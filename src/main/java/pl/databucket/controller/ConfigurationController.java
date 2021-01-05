package pl.databucket.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pl.databucket.exception.ExceptionFormatter;
import java.util.HashMap;
import java.util.Map;

@CrossOrigin(origins = "*", allowedHeaders = "*")
@RequestMapping("/api")
@RestController
public class ConfigurationController {

  private final ExceptionFormatter exceptionFormatter;

  ConfigurationController() {
    this.exceptionFormatter = new ExceptionFormatter(ConfigurationController.class);
  }

  @Value("${databucket.title}")
  private String title;

  @GetMapping(value = "/title", produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<?> getTitle() {
    try {
      Map<String, Object> response = new HashMap<>();
      response.put("title", this.title);
      return new ResponseEntity<>(response, HttpStatus.OK);
    } catch (Exception ee) {
      return exceptionFormatter.defaultException(ee);
    }
  }
}
