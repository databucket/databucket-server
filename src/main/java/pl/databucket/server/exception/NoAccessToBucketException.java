package pl.databucket.server.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class NoAccessToBucketException extends Exception {

    public NoAccessToBucketException(String bucketName) {
        super("This user has no access to bucket '" + bucketName + "'!");
    }

}
