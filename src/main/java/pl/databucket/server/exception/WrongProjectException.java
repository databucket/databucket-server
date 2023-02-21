package pl.databucket.server.exception;

public class WrongProjectException extends RuntimeException {

    public WrongProjectException() {
        super("Incorrect project");
    }

}
