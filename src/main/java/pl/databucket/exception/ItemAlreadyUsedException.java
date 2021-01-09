package pl.databucket.exception;

public class ItemAlreadyUsedException extends Exception {

    public ItemAlreadyUsedException(Class<?> clazz, Long id) {
        super("Entity '" + clazz.getName() + "' with the given id '" + id + "' already used exception!");
    }

    public ItemAlreadyUsedException(String message) {
        super("The item cannot be removed.\nThe following object[s] use this item:\n" + message);
    }

}
