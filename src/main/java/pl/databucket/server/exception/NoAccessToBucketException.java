package pl.databucket.server.exception;

public class NoAccessToBucketException extends Exception {

	public NoAccessToBucketException(String bucketName) {
        super("This user has no access to bucket '" + bucketName + "'!");
    }

}
