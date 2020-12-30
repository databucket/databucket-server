package pl.databucket.exception;

import org.slf4j.Logger;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import pl.databucket.response.BaseResponse;

public class CustomExceptionFormatter {

    private final Logger logger;

    public CustomExceptionFormatter(Logger logger) {
        this.logger = logger;
    }

    public ResponseEntity<BaseResponse> defaultException(BaseResponse rb, Exception e) {
        logger.error("ERROR:", e);
        rb.setMessage(e.getMessage());
        return new ResponseEntity<>(rb, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    public ResponseEntity<BaseResponse> customException(BaseResponse baseResponse, Exception e, HttpStatus status) {
        logger.warn(e.getMessage());
        baseResponse.setMessage(e.getMessage());
        return new ResponseEntity<>(baseResponse, status);
    }

    public ResponseEntity<BaseResponse> customException(Exception e, HttpStatus status) {
        BaseResponse baseResponse = new BaseResponse();
        logger.warn(e.getMessage());
        baseResponse.setMessage(e.getMessage());
        return new ResponseEntity<>(baseResponse, status);
    }

}
