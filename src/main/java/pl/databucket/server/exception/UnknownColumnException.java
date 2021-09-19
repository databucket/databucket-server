package pl.databucket.server.exception;

public class UnknownColumnException extends Exception {

	public UnknownColumnException(String columnName) {
		super("Column '" + columnName + "' is unknown!");
    }

}
