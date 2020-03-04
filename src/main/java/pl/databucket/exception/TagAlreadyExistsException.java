package pl.databucket.exception;

@SuppressWarnings("serial")
public class TagAlreadyExistsException extends Exception {

	public TagAlreadyExistsException(String tagName) {
        super("Tag '" + tagName + "' already exists!");
    }

}
