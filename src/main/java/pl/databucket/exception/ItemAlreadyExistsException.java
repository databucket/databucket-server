package pl.databucket.exception;

public class ItemAlreadyExistsException extends Exception {

    public ItemAlreadyExistsException(Class<?> clazz, String name) {
        super("Entity '" + clazz.getSimpleName() + "' with the given name '" + name + "' already exists!");
    }
}
