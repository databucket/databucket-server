package pl.databucket.exception;

public class ItemAlreadyUsedException extends Exception {

    public ItemAlreadyUsedException(String message) {
        super("The item cannot be removed.\nThe following object[s] use this item:\n" + message);
    }

}
