package pl.databucket.server.exception;

public class ModifyByNullEntityIdException extends Exception {

    public ModifyByNullEntityIdException(Class<?> clazz) {
        super("Can not modify entity '" + clazz.getSimpleName() + "' with null id!");
    }

}
