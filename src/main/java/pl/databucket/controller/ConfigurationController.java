package pl.databucket.controller;

import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pl.databucket.exception.CustomExceptionFormatter;
import pl.databucket.response.BaseResponse;
import pl.databucket.response.ConfigurationResponse;

import static org.springframework.http.ResponseEntity.ok;

@CrossOrigin(origins = "*", allowedHeaders = "*")
@RequestMapping("/api")
@RestController
public class ConfigurationController {

  private final CustomExceptionFormatter customExceptionFormatter;

  ConfigurationController() {
    this.customExceptionFormatter = new CustomExceptionFormatter(LoggerFactory.getLogger(ConfigurationController.class));
  }

  @Value("${databucket.title}")
  private String title;

  @GetMapping(value = "/title", produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<BaseResponse> getTitle() {
    ConfigurationResponse rb = new ConfigurationResponse();

    try {
      rb.setTitle(this.title);
      return ok(rb);
    } catch (Exception ee) {
      return customExceptionFormatter.defaultException(rb, ee);
    }
  }
}
