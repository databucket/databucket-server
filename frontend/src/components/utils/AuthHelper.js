import {
    hasAdminRole,
    hasMemberRole,
    hasSuperRole,
    logOut,
    setActiveProjectId,
    setRoles,
    setToken,
    setUsername
} from "../../utils/ConfigurationStorage";

export const handleSuccessfulLogin = (data, state) => {
    logOut();
    setUsername(data.username);
    if (!!data.token) {
        setToken(data.token);
    }
    // if (data.changePassword != null && data.changePassword === true) {
    //     setToken(data.token);
    //     return {...state, changePassword: true};
    // } else if (data.projects != null) {
    if (data.projects != null) {
        setRoles(data.roles);

        if (hasSuperRole()) {
            setToken(data.token);
        }

        return {...state, projects: data.projects, changePassword: false};
    } else if (data.token != null) {
        setRoles(data.roles);
        setToken(data.token);
        if (hasMemberRole() || hasAdminRole()) {
            setActiveProjectId(data.project.id);
            // ReactGA.initialize('UA-86983600-1');
            // ReactGA.pageview("login-to-project");
            return {...state, projects: null, changePassword: false, projectId: data.project.id};
        } else if (hasSuperRole()) {
            return {...state, projects: null, changePassword: false};
        } else
            throw Error('This user does not have required role to see the project frontend!');
    }
    return null;
};
