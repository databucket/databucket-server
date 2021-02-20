const THEME_NAME = "theme-name";
const USER_NAME = "user-name";
const TOKEN = 'token';
const PROJECT_NAME = 'project-name';
const PROJECT_ID = 'project-id';
const ROLES = 'roles';
const LAST_PAGE_SIZE = 'last-page-size';
const LAST_PAGE_SIZE_DIALOG = 'last-page-size-dialog';
const LAST_SETTINGS_PAGE_NAME = 'last-settings-page-name';
const LAST_MANAGEMENT_PAGE_NAME = 'last-management-page-name';

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

export const getProjectId = () => {
    return localStorage.getItem(PROJECT_ID)
}

export const clearProjectId = () => {
    localStorage.removeItem(PROJECT_ID);
}

export const setRoles = (roles) => {
    localStorage.setItem(ROLES, roles.join(","));
}

export const getRoles = () => {
    return localStorage.getItem(ROLES);
}

export const hasSuperRole = () => {
    return !!localStorage.getItem(ROLES) && localStorage.getItem(ROLES).includes("SUPER");
}

export const hasOnlyRobotRole = (roles) => {
    return (roles != null) && (roles.length === 1) && (roles[0] === 'ROBOT');
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

export const getLastPageSizeOnDialog = () => {
    if (localStorage.getItem(LAST_PAGE_SIZE_DIALOG) != null)
        return parseInt(localStorage.getItem(LAST_PAGE_SIZE_DIALOG));
    else
        return 10;
}

export const setLastPageSizeOnDialog = (size) => {
    localStorage.setItem(LAST_PAGE_SIZE_DIALOG, size.toString());
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

export const setLastSettingsPageName = (name) => {
    localStorage.setItem(LAST_SETTINGS_PAGE_NAME, name);
}

export const getLastSettingsPageName = () => {
    return !!localStorage.getItem(LAST_SETTINGS_PAGE_NAME) ? localStorage.getItem(LAST_SETTINGS_PAGE_NAME) : 'groups';
}

export const setLastManagementPageName = (name) => {
    localStorage.setItem(LAST_MANAGEMENT_PAGE_NAME, name);
}

export const getLastManagementPageName = () => {
    return !!localStorage.getItem(LAST_MANAGEMENT_PAGE_NAME) ? localStorage.getItem(LAST_MANAGEMENT_PAGE_NAME) : 'projects';
}
