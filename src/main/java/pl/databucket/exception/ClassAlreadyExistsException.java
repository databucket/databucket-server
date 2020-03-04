package pl.databucket.exception;

@SuppressWarnings("serial")
public class ClassAlreadyExistsException extends Exception {

	public ClassAlreadyExistsException(String className) {
        super("Class '" + className + "' already exists!");
    }

}
