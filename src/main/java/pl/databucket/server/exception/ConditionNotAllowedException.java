package pl.databucket.server.exception;

import pl.databucket.server.service.data.Condition;

public class ConditionNotAllowedException extends Exception {

    public ConditionNotAllowedException(Condition condition) {
        super("The following condition is not allowed: " + condition.toString());
    }

}
