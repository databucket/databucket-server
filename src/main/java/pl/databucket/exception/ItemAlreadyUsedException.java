package pl.databucket.exception;

public class ItemAlreadyUsedException extends Exception {

    public ItemAlreadyUsedException(Class<?> clazz, Long id) {
        super("Entity '" + clazz.getSimpleName() + "' with the given id '" + id + "' already used!");
    }

    public ItemAlreadyUsedException(String message) {
        super("The item cannot be removed.\nThe following object[s] use this item:\n" + message);
    }

}
