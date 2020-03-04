package pl.databucket.exception;

@SuppressWarnings("serial")
public class FilterAlreadyExistsException extends Exception {

	public FilterAlreadyExistsException(String filterName) {
        super("Filter '" + filterName + "' already exists!");
    }

}
