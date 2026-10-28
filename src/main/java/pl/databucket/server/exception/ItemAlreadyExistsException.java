package pl.databucket.server.exception;

public class ItemAlreadyExistsException extends Exception {

    public ItemAlreadyExistsException(Class<?> clazz, String name) {
        this(clazz, "name", name);
    }

    public ItemAlreadyExistsException(Class<?> clazz, String field, String value) {
        super("Entity '" + clazz.getSimpleName() + "' with the given " + field + " '" + value + "' already exists!");
    }
}
