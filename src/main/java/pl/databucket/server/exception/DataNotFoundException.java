package pl.databucket.server.exception;

/**
 * A data row that doesn't exist in a bucket, or that the user may not see (in a bucket with protected data a row not
 * reserved by the user): the same answer for both, so that it doesn't tell which rows exist.
 */
public class DataNotFoundException extends Exception {

    public DataNotFoundException(String bucketName, Long dataId) {
        super("The data row " + dataId + " doesn't exist in the bucket '" + bucketName + "'!");
    }
}
