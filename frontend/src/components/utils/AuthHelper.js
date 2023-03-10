import {getBaseUrl} from "../../utils/UrlBuilder";
import {handleLoginErrors} from "../../utils/FetchHelper";
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
import {sortByKey} from "../../utils/JsonHelper";

export const signIn = (state) => {
    return fetch(getBaseUrl('public/sign-in'), {
        method: 'POST',
        body: JSON.stringify(state.projectId == null ? {
            username: state.username,
            password: state.password
        } : {username: state.username, password: state.password, projectId: state.projectId}),
        headers: {'Content-Type': 'application/json'}
    })
        .then(handleLoginErrors)
        .then(value => handleSuccessfulLogin(value, state));
};

export const handleSuccessfulLogin = (data, state) => {
    logOut();
    setUsername(state.username);
    if (data.changePassword != null && data.changePassword === true) {
        setToken(data.token);
        return {...state, changePassword: true};
    } else if (data.projects != null) {
        setRoles(data.roles);

        if (hasSuperRole()) {
            setToken(data.token);
        }

        let sortedProjects = sortByKey(data.projects, 'id');
        return {...state, projects: sortedProjects, changePassword: false};
    } else if (data.token != null) {
        setRoles(data.roles);
        setToken(data.token);
        if (hasMemberRole() || hasAdminRole()) {
            setActiveProjectId(data.project.id);
            // ReactGA.initialize('UA-86983600-1');
            // ReactGA.pageview("login-to-project");
            return {...state, projects: null, changePassword: false};
        } else if (hasSuperRole()) {
            return {...state, projects: null, changePassword: false};
        } else
            throw Error('This user does not have required role to see the project frontend!');
    }
    return null;
};
