package pl.databucket.server.exception;

// deleting a project, or importing a dataset into it, needs an empty project: it has to be cleared first
public class ProjectNotEmptyException extends Exception {

    // action: e.g. "deleting it", "importing a dataset"
    public ProjectNotEmptyException(String name, String action) {
        super("The project '" + name + "' is not empty. Clear it before " + action + ".");
    }

}
