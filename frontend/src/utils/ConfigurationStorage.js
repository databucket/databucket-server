const THEME_NAME = "theme-name";
const USER_NAME = "user-name";
const PATHNAME = "href";
const TOKEN = 'token';
const PROJECT_ID = 'project-id';
const PROJECT_CONTEXT = 'project-context';
const ROLES = 'roles';
const LAST_PAGE_SIZE = 'last-page-size';
const LAST_PAGE_SIZE_DIALOG = 'last-page-size-dialog';
const LAST_SETTINGS_PAGE_NAME = 'last-settings-page-name';
const LAST_MANAGEMENT_PAGE_NAME = 'last-management-page-name';
const LEFT_PANEL_OPEN = 'left-panel-open';
const LAST_ACTIVE_GROUP = 'last-active-group';
const LAST_ACTIVE_BUCKET = 'last-active-bucket';
const LAST_OPENED_BUCKETS = 'last-opened-buckets';
const LAST_ACTIVE_VIEW = 'last-active-view';
const LAST_SEARCHED_TEXT = 'last-searched-text';
const LAST_ORDER = 'last-order';
const DATA_DETAILS_DIALOG_SIZE = 'data-details-dialog-size';
const DATA_FILTER_DIALOG_SIZE = 'data-filter-dialog-size';
const TASK_EXECUTION_DIALOG_SIZE = 'task-execution-dialog-size';

export const logOut = () => {
    clearUsername();
    clearToken();
    clearRoles();
    clearActiveProjectId();
}

export const setPathname = (pathname) => {
    localStorage.setItem(PATHNAME, pathname);
}

export const getPathname = () => {
    return localStorage.getItem(PATHNAME);
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

export const hasToken = () => {
    return !!localStorage.getItem(TOKEN);
}

export const hasProject = () => {
    return !!localStorage.getItem(PROJECT_ID);
}

export const setActiveProjectId = (projectId) => {
    localStorage.setItem(PROJECT_ID, projectId);
}

export const getActiveProjectId = () => {
    return parseInt(localStorage.getItem(PROJECT_ID) || "0", 10);
}

export const clearActiveProjectId = () => {
    localStorage.removeItem(PROJECT_ID);
}

export const setRoles = (roles) => {
    localStorage.setItem(ROLES, roles.join(","));
}

export const getRoles = () => {
    return localStorage.getItem(ROLES);
}

export const hasRobotRole = () => {
    return !!localStorage.getItem(ROLES) && localStorage.getItem(ROLES).includes("ROBOT");
}

export const hasMemberRole = () => {
    return !!localStorage.getItem(ROLES) && localStorage.getItem(ROLES).includes("MEMBER");
}

export const hasAdminRole = () => {
    return !!localStorage.getItem(ROLES) && localStorage.getItem(ROLES).includes("ADMIN");
}

export const hasSuperRole = () => {
    return !!localStorage.getItem(ROLES) && localStorage.getItem(ROLES).includes("SUPER");
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
        return parseInt(localStorage.getItem(LAST_PAGE_SIZE_DIALOG), 10);
    else
        return 10;
}

export const setLastPageSizeOnDialog = (size) => {
    localStorage.setItem(LAST_PAGE_SIZE_DIALOG, size.toString());
}

export const getLastPageSize = () => {
    if (localStorage.getItem(LAST_PAGE_SIZE) != null)
        return parseInt(localStorage.getItem(LAST_PAGE_SIZE), 10);
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

export const isLeftPanelOpen = () => {
    return !!localStorage.getItem(LEFT_PANEL_OPEN) && localStorage.getItem(LEFT_PANEL_OPEN).toLowerCase() === 'true';
}

export const setLeftPanelOpen = (open) => {
    localStorage.setItem(LEFT_PANEL_OPEN, JSON.stringify(open));
}

// ********************************  PROJECT CONTEXT  ********************************

const getActiveProjectKey = () => {
    return `P-${getActiveProjectId()}`;
}

const setActiveProjectContextProperty = (propertyName, propertyValue) => {
    let projectContext = JSON.parse(localStorage.getItem(PROJECT_CONTEXT));
    if (projectContext == null)
        projectContext = {};

    let activeProjectContext = projectContext[getActiveProjectKey()];
    if (activeProjectContext == null)
        activeProjectContext = {};

    activeProjectContext[propertyName] = propertyValue;
    projectContext[getActiveProjectKey()] = activeProjectContext;

    localStorage.setItem(PROJECT_CONTEXT, JSON.stringify(projectContext))
}

const getActiveProjectContextProperty = (propertyName) => {
    if (localStorage.getItem(PROJECT_CONTEXT) != null) {
        const projectContext = JSON.parse(localStorage.getItem(PROJECT_CONTEXT));
        const activeProjectContext = projectContext[getActiveProjectKey()];
        if (activeProjectContext != null)
            return activeProjectContext[propertyName];
    } else
        return null;
}


export const getLastActiveGroup = () => {
    const lastActiveGroup = getActiveProjectContextProperty(LAST_ACTIVE_GROUP);

    if (lastActiveGroup != null)
        return parseInt(lastActiveGroup, 10);
    else
        return -1;
}

export const setLastActiveGroup = (groupId) => {
    setActiveProjectContextProperty(LAST_ACTIVE_GROUP, groupId);
}

export const getLastActiveBucket = () => {
    const lastActiveBucket = getActiveProjectContextProperty(LAST_ACTIVE_BUCKET);

    if (lastActiveBucket != null)
        return parseInt(lastActiveBucket, 10);
    else
        return -1;
}

export const setLastActiveBucket = (bucketId) => {
    setActiveProjectContextProperty(LAST_ACTIVE_BUCKET, bucketId);
}

export const getLastOpenedBuckets = () => {
    const lastOpenedBuckets = getActiveProjectContextProperty(LAST_OPENED_BUCKETS);

    if (lastOpenedBuckets != null)
        return lastOpenedBuckets;
    else
        return [];
}

export const setLastOpenedBuckets = (buckets) => {
    setActiveProjectContextProperty(LAST_OPENED_BUCKETS, buckets.map(({id}) => id));
}

export const getLastActiveView = (bucketId) => {
    const lastActiveView = getActiveProjectContextProperty(LAST_ACTIVE_VIEW);

    if (lastActiveView != null)
        return lastActiveView[`${bucketId}`];
}

export const setLastActiveView = (bucketId, viewId) => {
    let lastActiveView = getActiveProjectContextProperty(LAST_ACTIVE_VIEW);
    if (lastActiveView == null)
        lastActiveView = {};

    lastActiveView[`${bucketId}`] = viewId;
    setActiveProjectContextProperty(LAST_ACTIVE_VIEW, lastActiveView);
}

export const getLastBucketSearchedText = (bucketId) => {
    const lastSearchedText = getActiveProjectContextProperty(LAST_SEARCHED_TEXT);

    if (lastSearchedText != null) {
        const text = lastSearchedText[`${bucketId}`];
        if (text != null)
            return text;
    }
    return "";
}

export const setLastBucketSearchedText = (bucketId, searchedText) => {
    let lastSearchedText = getActiveProjectContextProperty(LAST_SEARCHED_TEXT);
    if (lastSearchedText == null)
        lastSearchedText = {};

    lastSearchedText[`${bucketId}`] = searchedText;
    setActiveProjectContextProperty(LAST_SEARCHED_TEXT, lastSearchedText);
}

export const clearAllSearchedText = () => {
    setActiveProjectContextProperty(LAST_SEARCHED_TEXT, null);
}

export const getLastBucketOrder = (bucketId) => {
    const lastOrder = getActiveProjectContextProperty(LAST_ORDER);
    let result = null;

    if (lastOrder != null) {
        const order = lastOrder[`${bucketId}`];
        if (order != null)
            result = order;
    }
    return result;
}

export const setLastBucketOrder = (bucketId, order) => {
    let orderList = getActiveProjectContextProperty(LAST_ORDER);
    if (orderList == null)
        orderList = {};

    orderList[`${bucketId}`] = order;
    setActiveProjectContextProperty(LAST_ORDER, orderList);
}

export const clearAllOrders = () => {
    setActiveProjectContextProperty(LAST_ORDER, null);
}

export const getDataDetailsDialogSize = () => {
    if (localStorage.getItem(DATA_DETAILS_DIALOG_SIZE) != null) {
        return localStorage.getItem(DATA_DETAILS_DIALOG_SIZE);
    } else
        return 'lg';
}

export const setDataDetailsDialogSize = (size) => {
    localStorage.setItem(DATA_DETAILS_DIALOG_SIZE, size.toString());
}

export const getDataFilterDialogSize = () => {
    if (localStorage.getItem(DATA_FILTER_DIALOG_SIZE) != null) {
        return localStorage.getItem(DATA_FILTER_DIALOG_SIZE);
    } else
        return 'md';
}

export const setDataFilterDialogSize = (size) => {
    localStorage.setItem(DATA_FILTER_DIALOG_SIZE, size);
}

export const getTaskExecutionDialogSize = () => {
    if (localStorage.getItem(TASK_EXECUTION_DIALOG_SIZE) != null) {
        return localStorage.getItem(TASK_EXECUTION_DIALOG_SIZE);
    } else
        return 'md';
}

export const setTaskExecutionDialogSize = (size) => {
    localStorage.setItem(TASK_EXECUTION_DIALOG_SIZE, size);
}

