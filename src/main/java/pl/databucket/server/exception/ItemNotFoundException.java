package pl.databucket.server.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class ItemNotFoundException extends Exception {

    public ItemNotFoundException(Class<?> clazz, String name) {
        super("Entity '" + clazz.getSimpleName() + "' with the given name '" + name + "' doesn't exist!");
    }

    public ItemNotFoundException(Class<?> clazz, Long id) {
        super("Entity '" + clazz.getSimpleName() + "' with the given id '" + id + "' doesn't exist!");
    }

    public ItemNotFoundException(Class<?> clazz, Integer id) {
        super("Entity '" + clazz.getSimpleName() + "' with the given id '" + id + "' doesn't exist!");
    }

    public ItemNotFoundException(Class<?> clazz, Short id) {
        super("Entity '" + clazz.getSimpleName() + "' with the given id '" + id + "' doesn't exist!");
    }

}
