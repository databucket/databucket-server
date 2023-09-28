package pl.databucket.server.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class ModifyByNullEntityIdException extends Exception {

    public ModifyByNullEntityIdException(Class<?> clazz) {
        super("Can not modify entity '" + clazz.getSimpleName() + "' with null id!");
    }

}
