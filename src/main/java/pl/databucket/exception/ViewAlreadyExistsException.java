package pl.databucket.exception;

@SuppressWarnings("serial")
public class ViewAlreadyExistsException extends Exception {

	public ViewAlreadyExistsException(String viewName) {
        super("View '" + viewName + "' already exists!");
    }

}
