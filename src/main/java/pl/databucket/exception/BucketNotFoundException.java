package pl.databucket.exception;

public class BucketNotFoundException extends Exception {
    public BucketNotFoundException(String bucketName) {
        super("The bucket '" + bucketName + "' does not exist!");
    }
}
