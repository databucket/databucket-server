package pl.databucket.server.exception;

import java.util.List;

public class SomeItemsNotFoundException extends Exception {

    public SomeItemsNotFoundException(Class<?> clazz, List<Long> ids) {
        super("Entity '" + clazz.getName() + "' with the given ids '" + ids + "' doesn't exist!");
    }

}
