package pl.databucket.controller;

import static org.springframework.http.ResponseEntity.ok;

import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pl.databucket.exception.CustomExceptionFormatter;
import pl.databucket.exception.ItemDoNotExistsException;
import pl.databucket.service.DataService;
import pl.databucket.response.BaseResponse;
import pl.databucket.response.DataResponse;

@CrossOrigin(origins = "*", allowedHeaders = "*")
@RequestMapping("/api")
@RestController
public class DataHistoryController {

  private final CustomExceptionFormatter customExceptionFormatter;
  private final DataService service;

  public DataHistoryController(DataService service) {
    this.service = service;
    this.customExceptionFormatter = new CustomExceptionFormatter(LoggerFactory.getLogger(DataHistoryController.class));
  }

  @GetMapping(value = {
          "/bucket/{bucketName}/data/{dataId}/history"},
          produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<BaseResponse> getDataHistory(
          @PathVariable("bucketName") String bucketName, 
          @PathVariable("dataId") Integer dataId) {

    DataResponse response = new DataResponse();
    
    try {
      response.setHistory(service.getDataHistory(bucketName, dataId));
      return ok(response);
    } catch (ItemDoNotExistsException e) {
      return customExceptionFormatter.customException(response, e, HttpStatus.NOT_FOUND);
    } catch (Exception ee) {
      return customExceptionFormatter.defaultException(response, ee);
    }
  }

  @GetMapping(value = {
          "/bucket/{bucketName}/data/{dataId}/history/properties/{ids}"},
          produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<BaseResponse> getDataHistoryProperties(
          @PathVariable String bucketName,
          @PathVariable Integer dataId,
          @PathVariable Integer[] ids) {

    DataResponse response = new DataResponse();

    try {
      response.setHistory(service.getDataHistoryProperties(bucketName, dataId, ids));
      return ok(response);
    } catch (ItemDoNotExistsException e) {
      return customExceptionFormatter.customException(response, e, HttpStatus.NOT_FOUND);
    } catch (Exception ee) {
      return customExceptionFormatter.defaultException(response, ee);
    }
  }

}
