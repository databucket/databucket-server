package pl.databucket.server.exception;

public class UnexpectedException extends Exception {

	public UnexpectedException(Exception e) {
        super(e);
    }
	
	public UnexpectedException(String message) {
        super(message);
    }

}
