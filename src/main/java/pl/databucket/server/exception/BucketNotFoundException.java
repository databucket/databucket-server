package pl.databucket.server.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class BucketNotFoundException extends Exception {

    public BucketNotFoundException(String bucketName) {
        super("The bucket '" + bucketName + "' does not exist!");
    }
}
