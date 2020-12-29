package pl.databucket.exception;

public class BucketAlreadyExistsException extends Exception {

	public BucketAlreadyExistsException(String bucketName) {
        super("Bucket '" + bucketName + "' already exists!");
    }

}
