package pl.databucket.server.exception;

public class ForbiddenRepetitionException extends Exception {

	public ForbiddenRepetitionException(Exception e) {
        super(e);
    }

	public ForbiddenRepetitionException(String message) {
        super(message);
    }

}
