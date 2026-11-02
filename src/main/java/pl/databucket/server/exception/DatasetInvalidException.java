package pl.databucket.server.exception;

import java.util.List;

// a dataset that can't be imported: what is wrong with it, one problem per line
public class DatasetInvalidException extends Exception {

    public DatasetInvalidException(String problem) {
        super(problem);
    }

    public DatasetInvalidException(List<String> problems) {
        super("The dataset can't be imported:\n" + String.join("\n", problems));
    }

}
