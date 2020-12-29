package pl.databucket.exception;

public class FilterAlreadyExistsException extends Exception {

	public FilterAlreadyExistsException(String filterName) {
        super("Filter '" + filterName + "' already exists!");
    }

}
