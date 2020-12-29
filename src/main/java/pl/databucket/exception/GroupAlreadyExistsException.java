package pl.databucket.exception;

public class GroupAlreadyExistsException extends Exception {

	public GroupAlreadyExistsException(String groupName) {
        super("Group '" + groupName + "' already exists!");
    }

}
