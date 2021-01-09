package pl.databucket.exception;

public class ItemNotFoundException extends Exception {

    public ItemNotFoundException(Class<?> clazz, String name) {
        super("Entity '" + clazz.getName() + "' with the given name '" + name + "' doesn't exist exception!");
    }

    public ItemNotFoundException(Class<?> clazz, Long id) {
        super("Entity '" + clazz.getName() + "' with the given id '" + id + "' doesn't exist exception!");
    }

}
