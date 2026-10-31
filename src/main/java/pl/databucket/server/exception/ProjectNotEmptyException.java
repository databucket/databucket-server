package pl.databucket.server.exception;

// a project is deleted only when it holds nothing: it has to be cleared first
public class ProjectNotEmptyException extends Exception {

    public ProjectNotEmptyException(String name) {
        super("The project '" + name + "' is not empty. Clear it before deleting it.");
    }

}
