package pl.databucket.exception;

@SuppressWarnings("serial")
public class HarmlessException extends Exception {

	public HarmlessException(String message) {
        super(message);
    }

}
