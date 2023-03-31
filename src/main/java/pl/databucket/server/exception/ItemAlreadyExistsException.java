package pl.databucket.server.exception;


import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_ACCEPTABLE)
public class ItemAlreadyExistsException extends Exception {

    public ItemAlreadyExistsException(Class<?> clazz, String name) {
        super("Entity '" + clazz.getSimpleName() + "' with the given name '" + name + "' already exists!");
    }
}
