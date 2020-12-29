package pl.databucket.exception;

public class ExceededMaximumNumberOfCharactersException extends Exception {

	public ExceededMaximumNumberOfCharactersException(String item, String value, int max) {
        super("Invalid '" + item + "' value. The given value '" + value + "' contains " + value.length() + " characters, but it can contain maximum " + max + " characters!");
    }

}
