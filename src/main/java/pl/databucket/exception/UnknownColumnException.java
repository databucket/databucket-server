package pl.databucket.exception;

@SuppressWarnings("serial")
public class UnknownColumnException extends Exception {

	public UnknownColumnException(String columnName) {
		super("Column '" + columnName + "' is unknown!");
    }

}
