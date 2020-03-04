package pl.databucket.exception;

@SuppressWarnings("serial")
public class ItemDoNotExistsException extends Exception {

	public ItemDoNotExistsException(String type, int id) {
        super(type + " item with id '" + id + "' doesn't exist!");
    }
	
	public ItemDoNotExistsException(String type, String name) {
        super(type + " item with name '" + name + "' doesn't exist!");
    }
	
	public ItemDoNotExistsException(String message) {
        super(message);
    }

}
