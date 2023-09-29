import React from "react";
import {fetchHelper} from "./FetchHelper";
import {getToken} from "./ConfigurationStorage";
import {
    AccountCircle as UserIcon,
    EventBusy as ExpiredUserIcon,
    NotInterested as DisabledUserIcon
} from "@mui/icons-material";
import {darken, lighten, rgbToHex, Tooltip} from "@mui/material";
import {DarkTheme, LightTheme} from "./Themes";

const reverseMapping = (payload) => {
    let newPayload = JSON.parse(JSON.stringify(payload));

    if (newPayload['name'] != null && newPayload['name'] === '') {
        newPayload['name'] = null;
    }

    if (newPayload['description'] != null && newPayload['description'] === '') {
        newPayload['description'] = null;
    }

    if (newPayload['classId'] != null && newPayload['classId'] === 'none') {
        newPayload['classId'] = null;
    }

    if (newPayload['filterId'] === -1) {
        newPayload['filterId'] = null;
    }

    if (newPayload['roleId'] === 0 || newPayload['roleId'] === '0') {
        newPayload['roleId'] = null;
    }

    return JSON.stringify(newPayload);
}

export const getGetOptions = () => {
    return ({
        method: 'GET',
        headers: fetchHelper(getToken())
    });
}

export const getPostOptions = (payload) => {
    if (payload != null) {
        return ({
            method: 'POST',
            body: reverseMapping(payload),
            headers: fetchHelper(getToken())
        });
    } else {
        return ({
            method: 'POST',
            headers: fetchHelper(getToken())
        });
    }
}

export const getPutOptions = (payload) => {
    return ({
        method: 'PUT',
        body: reverseMapping(payload),
        headers: fetchHelper(getToken())
    });
}

export const getDeleteOptions = (payload) => {
    if (payload != null) {
        return ({
            method: 'DELETE',
            body: reverseMapping(payload),
            headers: fetchHelper(getToken())
        });
    } else {
        return ({
            method: 'DELETE',
            headers: fetchHelper(getToken())
        });
    }
}

export const getPageSizeOptions = () => {
    return [10, 15, 20, 25, 30, 35, 40, 50];
}

export const getTemplatePageSizeOptions = () => {
    return [10, 15, 20];
}

export const getPageSizeOptionsOnDialog = () => {
    return [10, 15, 20, 25, 30, 40, 50];
}

export const handleErrors = (res) => {
    if (res.ok) {
        return res.json();
    } else {
        return res.json().then(err => {
            throw err;
        });
    }
}

export const getButtonColor = (theme) => {
    return theme.palette.text.primary;
}

export const getTableHeaderBackgroundColor = (theme) => {
    if (theme.palette.mode === 'light') {
        return darken(theme.palette.background.default, 0.15);
    } else {
        return lighten(theme.palette.background.default, 0.15);
    }
}

export const getTableToolbarBackgroundColor = (theme) => {
    if (theme.palette.mode === 'light') {
        return darken(theme.palette.background.default, 0.07);
    } else {
        return lighten(theme.palette.background.default, 0.07);
    }
}

export const getTableRowBackgroundColor = (rowData, theme) => {
    if (theme.palette.mode === 'light') {
        return rowData.tableData.id % 2 === 1 ? darken(
            theme.palette.background.default, 0.04) : darken(
            theme.palette.background.default, 0.02);
    } else {
        return rowData.tableData.id % 2 === 1 ? lighten(
            theme.palette.background.default, 0.04) : lighten(
            theme.palette.background.default, 0.02);
    }
}

export const getTableRowForegroundColor = (rowData, theme, editable) => {
    if (theme.palette.mode === 'light') {
        return editable ? theme.palette.foreground.default
            : theme.palette.color.red;
    } else {
        return editable ? theme.palette.foreground.default
            : theme.palette.color.red;
    }
}

export const getSettingsTabsColor = (theme) => {
    if (theme.palette.mode === 'light') {
        return theme.palette.text;
    } else {
        return theme.palette.primary.contrastText;
    }
}

export const getSettingsTabsBackgroundColor = (theme) => {
    if (theme.palette.mode === 'light') {
        return darken(theme.palette.background.paper, 0.1);
    } else {
        return theme.palette.background.paper;
    }
}

export const getSettingsTabHooverBackgroundColor = (theme) => {
    if (theme.palette.mode === 'light') {
        return darken(theme.palette.background.paper, 0.03);
    } else {
        return lighten(theme.palette.background.paper, 0.1);
    }
}

export const getSettingsTabSelectedBackgroundColor = (theme) => {
    if (theme.palette.mode === 'light') {
        return darken(theme.palette.background.paper, 0.05);
    } else {
        return lighten(theme.palette.background.paper, 0.05);
    }
}

export const getSettingsTabSelectedColor = (theme) => {
    if (theme.palette.mode === 'light') {
        return darken(theme.palette.secondary.main, 0.2);
    } else {
        return theme.palette.secondary.main;
    }
}

export const getUserIcon = (rowData) => {
    const enabled = rowData['enabled'];
    const expiredDateStr = rowData['expirationDate'];
    let expired = false;

    if (expiredDateStr != null) {
        const now = new Date();
        const expiredDate = Date.parse(expiredDateStr);
        expired = expiredDate < now;
    }

    if (!enabled) {
        return (
            <Tooltip title={'Disabled'}>
                <DisabledUserIcon color={'error'}/>
            </Tooltip>
        );
    } else if (expired) {
        return (
            <Tooltip title={'Expired'}>
                <ExpiredUserIcon color={'error'}/>
            </Tooltip>
        );
    } else {
        return (<UserIcon/>);
    }
}

export const moveUp = (dataCollection, itemId) => {
    const updated = dataCollection.map(item => {
        if (item.tableData.id === itemId - 1) {
            item.tableData.id = itemId;
        } else if (item.tableData.id === itemId) {
            item.tableData.id = itemId - 1;
        }
        return item;
    });

    return (updated.sort((a, b) => {
        return (a.tableData.id > b.tableData.id) ? 1 : -1;
    }));
}

export const moveDown = (dataCollection, itemId) => {
    const updated = dataCollection.map(item => {
        if (item.tableData.id === itemId) {
            item.tableData.id = itemId + 1;
        } else if (item.tableData.id === itemId + 1) {
            item.tableData.id = itemId;
        }
        return item;
    });

    return updated.sort((a, b) => {
        return (a.tableData.id > b.tableData.id) ? 1 : -1;
    });
}

export const getTableHeight = () => {
    // Total Height - Appbar (72) - Toolbar (64) - Footer (52)
    const staticElements = 72 + 64 + 52 + 2;
    // const staticElements = 72 + 64 + 52 + 2;
    return `calc(100vh - ${staticElements}px)`;
}

export const getManagementTableHeight = () => {
    const staticElements = 64 + 64 + 40;
    return `calc(100vh - ${staticElements}px)`;
}

export const getSettingsTableHeight = () => {
    // Header (64) + footer (52)
    const staticElements = 64 + 52;
    return `calc(100vh - ${staticElements}px)`;
}

export const getTemplateTableHeight = () => {
    return `calc(100vh - 250px)`;
}

export const getDialogTableHeight = (custom) => {
    const staticElements = 64 + 64 + 52 + 2;
    return `calc(100vh - ${staticElements}px - ${custom}vh)`;
}

export const getPropertiesTableHeight = (height, custom) => {
    const tableHeight = ((height - 64 - 64 - 150) / height * 100).toFixed(2)
        - custom;
    return `${tableHeight}vh`;
}

export const getTableBodyHeight = (parentRef, tableHeadHeight) => {
    let newHeight = 300;
    if (parentRef.current) {
        newHeight = parentRef.current.offsetHeight - tableHeadHeight;
    }
    return newHeight;
}

export const getBodyHeight = () => {
    const staticElements = 64 + 64 + 60;
    return `calc(100vh - ${staticElements}px)`;
}

export const getIconColor = (type, color) => {
    if (!!color) {
        if (type === 'light') {
            return rgbToHex(darken(color, 0.1));
        } else if (type === 'dark') {
            return rgbToHex(lighten(color, 0.25));
        } else if (type === 'banner') {
            return rgbToHex(lighten(color, 0.15));
        } else {
            return color;
        }
    } else {
        if (type === 'light') {
            return LightTheme.palette.text.primary;
        } else if (type === 'dark') {
            return DarkTheme.palette.text.primary;
        } else if (type === 'banner') {
            return DarkTheme.palette.text.primary;
        } else {
            return LightTheme.palette.text.primary;
        }
    }
}
