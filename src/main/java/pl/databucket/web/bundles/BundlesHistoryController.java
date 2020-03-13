package pl.databucket.web.bundles;

import static org.springframework.http.ResponseEntity.ok;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pl.databucket.exception.ItemDoNotExistsException;
import pl.databucket.service.DatabucketService;
import pl.databucket.service.ResponseBody;
import pl.databucket.service.ResponseStatus;

@CrossOrigin(origins = "*", allowedHeaders = "*")
@RequestMapping("/api")
@RestController
public class BundlesHistoryController {

  private static final Logger logger = LoggerFactory.getLogger(BundlesHistoryController.class);

  private final DatabucketService databucketService;

  public BundlesHistoryController(DatabucketService databucketService) {
    this.databucketService = databucketService;
  }

  @GetMapping(value = {"/buckets/{bucketName}/bundles/{bundleId}/history"}, produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<ResponseBody> getBundleHistory(@PathVariable("bucketName") String bucketName, @PathVariable("bundleId") Integer bundleId) {

    ResponseBody rb = new ResponseBody();
    try {
      rb.setHistory(databucketService.getBundleHistory(bucketName, bundleId));
      rb.setStatus(ResponseStatus.OK);
      return ok(rb);
    } catch (ItemDoNotExistsException e) {
      return customException(rb, e, HttpStatus.NOT_FOUND);
    } catch (Exception ee) {
      return defaultException(rb, ee);
    }
  }

  @GetMapping(value = {"/buckets/{bucketName}/bundles/{bundleId}/history/properties/{ids}"}, produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<ResponseBody> getBundleHistoryProperties(@PathVariable String bucketName, @PathVariable Integer bundleId, @PathVariable Integer[] ids) {

    ResponseBody rb = new ResponseBody();
    try {
      rb.setHistory(databucketService.getBundleHistoryProperties(bucketName, bundleId, ids));
      rb.setStatus(ResponseStatus.OK);
      return ok(rb);
    } catch (ItemDoNotExistsException e) {
      return customException(rb, e, HttpStatus.NOT_FOUND);
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

  private ResponseEntity<ResponseBody> customException(ResponseBody rb, Exception e, HttpStatus status) {
    logger.warn(e.getMessage());
    rb.setStatus(ResponseStatus.FAILED);
    rb.setMessage(e.getMessage());
    return new ResponseEntity<>(rb, status);
  }
}
