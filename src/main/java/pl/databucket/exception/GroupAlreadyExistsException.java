package pl.databucket.exception;

@SuppressWarnings("serial")
public class GroupAlreadyExistsException extends Exception {

	public GroupAlreadyExistsException(String groupName) {
        super("Group '" + groupName + "' already exists!");
    }

}
