package pl.databucket.exception;

@SuppressWarnings("serial")
public class ColumnsAlreadyExistsException extends Exception {

	public ColumnsAlreadyExistsException(String columnsName) {
        super("Columns '" + columnsName + "' already exists!");
    }

}
