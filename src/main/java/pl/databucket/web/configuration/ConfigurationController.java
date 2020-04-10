package pl.databucket.web.configuration;

import static org.springframework.http.ResponseEntity.ok;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pl.databucket.service.ResponseBody;
import pl.databucket.service.ResponseStatus;

@CrossOrigin(origins = "*", allowedHeaders = "*")
@RequestMapping("/api")
@RestController
public class ConfigurationController {

  private static final Logger logger = LoggerFactory.getLogger(ConfigurationController.class);

  @Value("${databucket.title}")
  private String title;

  @GetMapping(value = "/title", produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<ResponseBody> getTitle() {
    ResponseBody rb = new ResponseBody();

    try {
      rb.setTitle(this.title);
      rb.setStatus(ResponseStatus.OK);
      return ok(rb);
    } catch (Exception ee) {
      return defaultException(rb, ee);
    }
  }

  private ResponseEntity<ResponseBody> defaultException(ResponseBody rb, Exception e) {
    logger.error("ERROR:", e);
    rb.setStatus(ResponseStatus.FAILED);
    rb.setMessage(e.getMessage());
    return new ResponseEntity<>(rb, HttpStatus.INTERNAL_SERVER_ERROR);
  }

}
