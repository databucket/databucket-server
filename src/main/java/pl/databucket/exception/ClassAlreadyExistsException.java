package pl.databucket.exception;

public class ClassAlreadyExistsException extends Exception {

	public ClassAlreadyExistsException(String className) {
        super("Class '" + className + "' already exists!");
    }

}
