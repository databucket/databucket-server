package pl.databucket.exception;

@SuppressWarnings("serial")
public class BucketAlreadyExistsException extends Exception {

	public BucketAlreadyExistsException(String bucketName) {
        super("Bucket '" + bucketName + "' already exists!");
    }

}
