package pl.databucket.exception;

import pl.databucket.database.Condition;

public class ConditionNotAllowedException extends Exception {

    public ConditionNotAllowedException(Condition condition) {
        super("The following condition is not allowed: " + condition.toString());
    }

}
