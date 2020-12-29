package pl.databucket.exception;

public class ViewAlreadyExistsException extends Exception {

	public ViewAlreadyExistsException(String viewName) {
        super("View '" + viewName + "' already exists!");
    }

}
