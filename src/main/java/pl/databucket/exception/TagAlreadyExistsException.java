package pl.databucket.exception;

public class TagAlreadyExistsException extends Exception {

	public TagAlreadyExistsException(String tagName) {
        super("Tag '" + tagName + "' already exists!");
    }

}
