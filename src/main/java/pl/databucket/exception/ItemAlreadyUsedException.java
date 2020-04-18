package pl.databucket.exception;

@SuppressWarnings("serial")
public class ItemAlreadyUsedException extends Exception {

	public ItemAlreadyUsedException() {
        super("The item cannot be removed because it is currently in use.");
    }

    public ItemAlreadyUsedException(String message) {
        super("The item cannot be removed.\nThe following object[s] use this item:\n" + message);
    }

}
