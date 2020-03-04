package pl.databucket.exception;

@SuppressWarnings("serial")
public class UnexpectedException extends Exception {

	public UnexpectedException(Exception e) {
        super(e);
    }
	
	public UnexpectedException(String message) {
        super(message);
    }

}
