const THEME_NAME = "theme-name";
const USER_NAME = "user-name";
const TOKEN = 'token';
const PROJECT_NAME = 'project-name';
const PROJECT_ID = 'project-id';
const ROLES = 'roles';
const LAST_PAGE_SIZE = 'last-page-size';

export const logOut = () => {
    clearUsername();
    clearToken();
    clearRoles();
    clearProjectName();
    clearProjectId();
}

export const clearUsername = () => {
    localStorage.removeItem(USER_NAME);
}

export const setUsername = (username) => {
    localStorage.setItem(USER_NAME, username);
}

export const getUsername = () => {
    return localStorage.getItem(USER_NAME);
}

export const setToken = (token) => {
    localStorage.setItem(TOKEN, token);
}

export const getToken = () => {
    return localStorage.getItem(TOKEN);
}

export const clearToken = () => {
    localStorage.removeItem(TOKEN)
}

export const isLogin = () => {
    return !!localStorage.getItem(TOKEN) && !!localStorage.getItem(PROJECT_ID);
}

export const hasToken = () => {
    return !!localStorage.getItem(TOKEN);
}

export const setProjectName = (projectName) => {
    localStorage.setItem(PROJECT_NAME, projectName);
}

export const getProjectName = () => {
    return localStorage.getItem(PROJECT_NAME)
}

export const clearProjectName = () => {
    localStorage.removeItem(PROJECT_NAME);
}

export const setProjectId = (projectId) => {
    localStorage.setItem(PROJECT_ID, projectId);
}

// export const getProjectId = () => {
//     return localStorage.getItem(PROJECT_ID)
// }

export const clearProjectId = () => {
    localStorage.removeItem(PROJECT_ID);
}

export const setRoles = (roles) => {
    localStorage.setItem(ROLES, roles.join(","));
}

export const getRoles = () => {
    return localStorage.getItem(ROLES);
}

export const hasRole = (role) => {
    if (localStorage.getItem(ROLES) != null)
        return localStorage.getItem(ROLES).includes(role);
    else
        return false;
}

export const clearRoles = () => {
    localStorage.removeItem(ROLES);
}

export const saveThemeName = (name) => {
    localStorage.setItem(THEME_NAME, name);
}

export const getThemeName = () => {
    if (localStorage.getItem(THEME_NAME) != null)
        return localStorage.getItem(THEME_NAME);
    else
        return 'light';
}

export const getLastPageSize = () => {
    if (localStorage.getItem(LAST_PAGE_SIZE) != null)
        return parseInt(localStorage.getItem(LAST_PAGE_SIZE));
    else
        return 15;
}

export const setLastPageSize = (size) => {
    localStorage.setItem(LAST_PAGE_SIZE, size.toString());
}
