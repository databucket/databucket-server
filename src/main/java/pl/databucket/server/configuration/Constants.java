package pl.databucket.server.configuration;

public class Constants {

    public static final String DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSSZ";
    public static final int NAME_MIN = 1;
    public static final int NAME_MAX = 30;
    /** User names, also stored as created_by / modified_by; fits the client ID (a GUID) of a robot from Entra ID. */
    public static final int USERNAME_MAX = 50;
    public static final int DESCRIPTION_MAX = 200;

    // Projects
    public static final String PROJECT_DEMO_NAME1 = "DEMO1";
    public static final String PROJECT_DEMO_DESCRIPTION1 = "Demonstration project 1";
    public static final String PROJECT_DEMO_NAME2 = "DEMO2";
    public static final String PROJECT_DEMO_DESCRIPTION2 = "Demonstration project 2";

    // Roles
    public static final String ROLE_SUPER = "SUPER";
    public static final String ROLE_ADMIN = "ADMIN";
    public static final String ROLE_MEMBER = "MEMBER";
    public static final String ROLE_ROBOT = "ROBOT";

    /**
     * Since 3.12.0 a user has one role, and each role includes the ones below it: SUPER > ADMIN > MEMBER > ROBOT.
     * The role of a bucket, view or group means that role or higher.
     */
    public static final java.util.List<String> ROLES_BY_LEVEL = java.util.List.of(ROLE_ROBOT, ROLE_MEMBER, ROLE_ADMIN, ROLE_SUPER);

    /** Position of the role in the hierarchy (ROBOT = 0); -1 for an unknown name. */
    public static int roleLevel(String roleName) {
        return ROLES_BY_LEVEL.indexOf(roleName);
    }

    // Columns
    public static final String COLUMNS_DEFAULT = "default";

    // Views
    public static final String VIEWS_DEFAULT = "default";

}