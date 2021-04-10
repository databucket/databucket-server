import {fetchHelper} from "./FetchHelper";
import {getToken} from "./ConfigurationStorage";
import React, {forwardRef} from "react";
import AddBox from "@material-ui/icons/AddBox";
import Check from "@material-ui/icons/Check";
import Clear from "@material-ui/icons/Clear";
import DeleteOutline from "@material-ui/icons/DeleteOutline";
import ChevronRight from "@material-ui/icons/ChevronRight";
import Edit from "@material-ui/icons/Edit";
import SaveAlt from "@material-ui/icons/SaveAlt";
import FilterList from "@material-ui/icons/FilterList";
import FirstPage from "@material-ui/icons/FirstPage";
import LastPage from "@material-ui/icons/LastPage";
import ChevronLeft from "@material-ui/icons/ChevronLeft";
import Search from "@material-ui/icons/Search";
import ArrowUpward from "@material-ui/icons/ArrowUpward";
import Remove from "@material-ui/icons/Remove";
import ViewColumn from "@material-ui/icons/ViewColumn";
import {darken, lighten} from '@material-ui/core/styles';
import UserIcon from "@material-ui/icons/AccountCircle";
import DisabledUserIcon from "@material-ui/icons/NotInterested";
import ExpiredUserIcon from "@material-ui/icons/EventBusy";
import Tooltip from "@material-ui/core/Tooltip";

const reverseMapping = (payload) => {
    let newPayload = JSON.parse(JSON.stringify(payload));

    if (newPayload['name'] != null && newPayload['name'] === '')
        newPayload['name'] = null;

    if (newPayload['description'] != null && newPayload['description'] === '')
        newPayload['description'] = null;

    if (newPayload['classId'] != null && newPayload['classId'] === 'none')
        newPayload['classId'] = null;

    if (newPayload['filterId'] === -1)
        newPayload['filterId'] = null;

    if (newPayload['roleId'] === 0 || newPayload['roleId'] === '0')
        newPayload['roleId'] = null;

    return JSON.stringify(newPayload);
}

export const getGetOptions = () => {
    return ({
        method: 'GET',
        headers: fetchHelper(getToken())
    });
}

export const getPostOptions = (payload) => {
    return ({
        method: 'POST',
        body: reverseMapping(payload),
        headers: fetchHelper(getToken())
    });
}

export const getPutOptions = (payload) => {
    return ({
        method: 'PUT',
        body: reverseMapping(payload),
        headers: fetchHelper(getToken())
    });
}

export const getDeleteOptions = () => {
    return ({
        method: 'DELETE',
        headers: fetchHelper(getToken())
    });
}

export const getPageSizeOptions = () => {
    return [15, 20, 25, 30, 35, 40, 50];
}

export const getPageSizeOptionsOnDialog = () => {
    return [10, 15, 20, 25, 30, 40];
}

export const getTableIcons = () => {
    return ({
        Add: forwardRef((props, ref) => <AddBox {...props} ref={ref}/>),
        Check: forwardRef((props, ref) => <Check {...props} ref={ref}/>),
        Clear: forwardRef((props, ref) => <Clear {...props} ref={ref}/>),
        Delete: forwardRef((props, ref) => <DeleteOutline {...props} ref={ref}/>),
        DetailPanel: forwardRef((props, ref) => <ChevronRight {...props} ref={ref}/>),
        Edit: forwardRef((props, ref) => <Edit {...props} ref={ref}/>),
        Export: forwardRef((props, ref) => <SaveAlt {...props} ref={ref}/>),
        Filter: forwardRef((props, ref) => <FilterList {...props} ref={ref}/>),
        FirstPage: forwardRef((props, ref) => <FirstPage {...props} ref={ref}/>),
        LastPage: forwardRef((props, ref) => <LastPage {...props} ref={ref}/>),
        NextPage: forwardRef((props, ref) => <ChevronRight {...props} ref={ref}/>),
        PreviousPage: forwardRef((props, ref) => <ChevronLeft {...props} ref={ref}/>),
        ResetSearch: forwardRef((props, ref) => <Clear {...props} ref={ref}/>),
        Search: forwardRef((props, ref) => <Search {...props} ref={ref}/>),
        SortArrow: forwardRef((props, ref) => <ArrowUpward {...props} ref={ref}/>),
        ThirdStateCheck: forwardRef((props, ref) => <Remove {...props} ref={ref}/>),
        ViewColumn: forwardRef((props, ref) => <ViewColumn {...props} ref={ref}/>),
    });
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

export const getTableHeaderBackgroundColor = (theme) => {
    if (theme.palette.type === 'light') {
        return darken(theme.palette.background.default, 0.1);
    } else
        return lighten(theme.palette.background.default, 0.1);
}

export const getTableRowBackgroundColor = (rowData, theme) => {
    if (theme.palette.type === 'light')
        return rowData.tableData.id % 2 === 1 ? darken(theme.palette.background.default, 0.02) : theme.palette.background.default;
    else
        return rowData.tableData.id % 2 === 1 ? lighten(theme.palette.background.default, 0.02) : theme.palette.background.default;
}

export const getSettingsTabsColor = (theme) => {
    if (theme.palette.type === 'light') {
        return theme.palette.text;
    } else
        return theme.palette.primary.contrastText;
}

export const getSettingsTabsBackgroundColor = (theme) => {
    if (theme.palette.type === 'light') {
        return darken(theme.palette.background.paper, 0.1);
    } else
        return theme.palette.background.paper;
}

export const getSettingsTabHooverBackgroundColor = (theme) => {
    if (theme.palette.type === 'light') {
        return darken(theme.palette.background.paper, 0.03);
    } else
        return lighten(theme.palette.background.paper, 0.1);
}

export const getSettingsTabSelectedBackgroundColor = (theme) => {
    if (theme.palette.type === 'light') {
        return darken(theme.palette.background.paper, 0.05);
    } else
        return lighten(theme.palette.background.paper, 0.05);
}

export const getSettingsTabSelectedColor = (theme) => {
    if (theme.palette.type === 'light') {
        return darken(theme.palette.secondary.main, 0.2);
    } else
        return theme.palette.secondary.main;
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

    if (!enabled)
        return (
            <Tooltip title={'Disabled'}>
                <DisabledUserIcon color={'error'}/>
            </Tooltip>
        );
    else if (expired)
        return (
            <Tooltip title={'Expired'}>
                <ExpiredUserIcon color={'error'}/>
            </Tooltip>
        );
    else
        return (<UserIcon/>);
}

export const moveUp = (dataCollection, itemId) => {
    const updated = dataCollection.map(item => {
        if (item.tableData.id === itemId - 1)
            item.tableData.id = itemId;
        else if (item.tableData.id === itemId)
            item.tableData.id = itemId - 1;
        return item;
    });

    return (updated.sort((a, b) => {
        return (a.tableData.id > b.tableData.id) ? 1 : -1;
    }));
}

export const moveDown = (dataCollection, itemId) => {
    const updated = dataCollection.map(item => {
        if (item.tableData.id === itemId)
            item.tableData.id = itemId + 1;
        else if (item.tableData.id === itemId + 1)
            item.tableData.id = itemId;
        return item;
    });

    return updated.sort((a, b) => {
        return (a.tableData.id > b.tableData.id) ? 1 : -1;
    });
}

export const getTableHeight = (height) => {
    const tableHeight = ((height - 64 - 64 - 52 - 2) / height * 100).toFixed(2);
    return `${tableHeight}vh`;
}

export const getManagementTableHeight = (height) => {
    const tableHeight = ((height - 64 - 64 - 40) / height * 100).toFixed(2);
    return `${tableHeight}vh`;
}

export const getSettingsTableHeight = (height) => {
    const tableHeight = ((height - 64 - 64 + 10) / height * 100).toFixed(2);
    return `${tableHeight}vh`;
}

export const getDialogTableHeight = (height, custom) => {
    const tableHeight = ((height - 64 - 64 - 52 - 2) / height * 100).toFixed(2) - custom;
    return `${tableHeight}vh`;
}

export const getPropertiesTableHeight = (height, custom) => {
    const tableHeight = ((height - 64 - 64 - 143) / height * 100).toFixed(2) - custom;
    return `${tableHeight}vh`;
}