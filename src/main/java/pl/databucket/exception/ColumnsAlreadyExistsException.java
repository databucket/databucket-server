package pl.databucket.exception;

public class ColumnsAlreadyExistsException extends Exception {

	public ColumnsAlreadyExistsException(String columnsName) {
        super("Columns '" + columnsName + "' already exists!");
    }

}
