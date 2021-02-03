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
import { darken, lighten } from '@material-ui/core/styles';

export const getPageableUlr = (endpoint, query, enableFilters) => {

    let url = `${window.apiURL}/${endpoint}?size=${query.pageSize}&page=${query.page}`;

    if (query.orderBy != null && query.orderBy.field != null)
        url += `&sort=${query.orderBy.field}`;
    if (query.orderDirection === 'desc')
        url += ',desc';

    if (enableFilters && query.filters.length > 0)
        for (const filter of query.filters)
            if (filter.column.type === 'boolean')
                url += `&${filter.column.field}=${filter.value === 'checked'}`;
            else
                url += `&${filter.column.field}=${escape(filter.value)}`;

    return url;
}

export const getBaseUrl = (endpoint) => {
    return `${window.apiURL}/${endpoint}`;
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
        body: JSON.stringify(payload),
        headers: fetchHelper(getToken())
    });
}

export const getPutOptions = (payload) => {
    return ({
        method: 'PUT',
        body: JSON.stringify(payload),
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
        return darken(theme.palette.background.default, 0.15);
    } else if (theme.palette.type === 'dark') {
        return lighten(theme.palette.background.default, 0.15);
    } else
        return darken(theme.palette.background.default, 0.15);
}

export const getTableRowBackgroundColor = (rowData, theme) => {
    if (theme.palette.type === 'light') {
        return rowData.tableData.id % 2 === 1 ? darken(theme.palette.background.default, 0.06) : darken(theme.palette.background.default, 0.03);
    } else if (theme.palette.type === 'dark') {
        return rowData.tableData.id % 2 === 1 ? lighten(theme.palette.background.default, 0.06) : lighten(theme.palette.background.default, 0.03);
    } else
        return rowData.tableData.id % 2 === 1 ? darken(theme.palette.background.default, 0.06) : darken(theme.palette.background.default, 0.03);
}
