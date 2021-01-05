package pl.databucket.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pl.databucket.exception.ExceptionFormatter;
import pl.databucket.exception.ItemDoNotExistsException;
import pl.databucket.response.DataResponse;
import pl.databucket.service.DataService;

import static org.springframework.http.ResponseEntity.ok;

@CrossOrigin(origins = "*", allowedHeaders = "*")
@RequestMapping("/api")
@RestController
public class DataHistoryController {

  private final ExceptionFormatter exceptionFormatter;
  private final DataService service;

  public DataHistoryController(DataService service) {
    this.service = service;
    this.exceptionFormatter = new ExceptionFormatter(DataHistoryController.class);
  }

  @GetMapping(value = {
          "/bucket/{bucketName}/data/{dataId}/history"},
          produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<?> getDataHistory(
          @PathVariable("bucketName") String bucketName, 
          @PathVariable("dataId") Integer dataId) {

    DataResponse response = new DataResponse();
    
    try {
      response.setHistory(service.getDataHistory(bucketName, dataId));
      return ok(response);
    } catch (ItemDoNotExistsException e) {
      return exceptionFormatter.customException(e, HttpStatus.NOT_FOUND);
    } catch (Exception ee) {
      return exceptionFormatter.defaultException(ee);
    }
  }

  @GetMapping(value = {
          "/bucket/{bucketName}/data/{dataId}/history/properties/{ids}"},
          produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<?> getDataHistoryProperties(
          @PathVariable String bucketName,
          @PathVariable Integer dataId,
          @PathVariable Integer[] ids) {

    DataResponse response = new DataResponse();

    try {
      response.setHistory(service.getDataHistoryProperties(bucketName, dataId, ids));
      return ok(response);
    } catch (ItemDoNotExistsException e) {
      return exceptionFormatter.customException(e, HttpStatus.NOT_FOUND);
    } catch (Exception ee) {
      return exceptionFormatter.defaultException(ee);
    }
  }

}
