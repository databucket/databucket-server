import React from 'react';
import {getAdminMemberRolesLookup, getArrayLengthStr, getItemName, getRoleName} from "../../utils/JsonHelper";
import SelectBucketsDialog from "../dialogs/SelectBucketsDialog";
import SelectClassesDialog from "../dialogs/SelectClassesDialog";
import SelectUsersDialog from "../dialogs/SelectUsersDialog";
import SelectGroupsDialog from "../dialogs/SelectGroupsDialog";
import SelectColumnsDialog from "../dialogs/SelectColumnsDialog";
import SelectFilterDialog from "../dialogs/SelectFilterDialog";
import SelectTeamsDialog from "../dialogs/SelectTeamsDialog";

export const getColumnId = () => {
    return {
        title: 'Id',
        field: 'id',
        type: 'numeric',
        editable: 'never',
        filtering: true,
        width: '1%',
        cellStyle: {width: '1%'}
    };
};

export const getColumnName = (width) => {
    if (width != null)
        return {
            title: 'Name',
            field: 'name',
            type: 'string',
            editable: 'always',
            filtering: true,
            width: {width},
            cellStyle: {width: {width}}
        };
    else
        return {
            title: 'Name',
            field: 'name',
            type: 'string',
            editable: 'always',
            filtering: true
        };
};

export const getColumnShortName = () => {
    return {
        title: 'Short name',
        field: 'shortName',
        type: 'string',
        editable: 'always',
        filtering: true
    };
};

export const getColumnDescription = (width) => {
    if (width != null) {
        return {
            title: 'Description',
            field: 'description',
            type: 'string',
            editable: 'always',
            filtering: true,
            width: {width},
            cellStyle: {width: {width}}
        };
    } else
        return {
            title: 'Description',
            field: 'description',
            type: 'string',
            editable: 'always',
            filtering: true
        };
};

export const getColumnEnabled = () => {
    return {
        title: 'Enabled',
        field: 'enabled',
        type: 'boolean',
        width: '1%',
        cellStyle: {width: '1%'}
    };
};

const CLASS_DEFAULT = 'none';
export const getColumnClass = (classesLookup, customTitle) => {
    return {
        title: customTitle != null ? customTitle : 'Class',
        field: 'classId',
        initialEditValue: CLASS_DEFAULT,
        emptyValue: CLASS_DEFAULT,
        lookup: classesLookup
    };
};

export const getColumnColumns = (columns) => {
    return {
        title: 'Columns', field: 'columnsId', filtering: false, searchable: false, sorting: false,
        render: rowData => getItemName(columns, rowData['columnsId']),
        editComponent: props => (
            <SelectColumnsDialog
                columns={columns != null ? columns : []}
                rowData={props.rowData}
                onChange={props.onChange}
            />
        )
    };
};

export const getColumnFilter = (filters) => {
    return {
        title: 'Filter', field: 'filterId', filtering: false, searchable: false, sorting: false,
        render: rowData => getItemName(filters, rowData['filterId']),
        editComponent: props => (
            <SelectFilterDialog
                filters={filters != null ? filters : []}
                rowData={props.rowData}
                onChange={props.onChange}
            />
        )
    };
};

export const getColumnClasses = (classes, customTitle) => {
    return {
        title: customTitle != null ? customTitle : 'Classes',
        field: 'classesIds',
        filtering: false,
        searchable: false,
        sorting: false,
        render: rowData => getArrayLengthStr(rowData['classesIds']),
        editComponent: props => (
            <SelectClassesDialog
                classes={classes != null ? classes : []}
                rowData={props.rowData}
                onChange={props.onChange}
            />
        )
    };
};

export const getColumnBuckets = (buckets, customTitle) => {
    return {
        title: customTitle != null ? customTitle : 'Buckets',
        field: 'bucketsIds',
        filtering: false,
        searchable: false,
        sorting: false,
        render: rowData => getArrayLengthStr(rowData['bucketsIds']),
        editComponent: props => (
            <SelectBucketsDialog
                buckets={buckets != null ? buckets : []}
                rowData={props.rowData}
                onChange={props.onChange}
            />
        )
    };
};

export const getColumnUsers = (users, roles, customTitle) => {
    return {
        title: customTitle != null ? customTitle : 'Users',
        field: 'usersIds',
        filtering: false,
        searchable: false,
        sorting: false,
        render: rowData => getArrayLengthStr(rowData['usersIds']),
        editComponent: props => (
            <SelectUsersDialog
                users={users != null ? users : []}
                roles={roles != null ? roles : []}
                rowData={props.rowData}
                onChange={props.onChange}
            />
        )
    };
};

export const getColumnTeams = (teams, customTitle) => {
    return {
        title: customTitle != null ? customTitle : 'Teams',
        field: 'teamsIds',
        filtering: false,
        searchable: false,
        sorting: false,
        render: rowData => getArrayLengthStr(rowData['teamsIds']),
        editComponent: props => (
            <SelectTeamsDialog
                teams={teams != null ? teams : []}
                rowData={props.rowData}
                onChange={props.onChange}
            />
        )
    };
};

export const getColumnRole = (roles, customTitle) => {
    return {
        title: customTitle != null ? customTitle : 'Role',
        type: 'numeric',
        initialEditValue: 0,
        field: 'roleId',
        filtering: false,
        sorting: false,
        render: rowData => getRoleName(roles, rowData['roleId']),
        lookup: getAdminMemberRolesLookup(roles),
    }
};

export const getColumnGroups = (groups, customTitle) => {
    return {
        title: customTitle != null ? customTitle : 'Groups',
        field: 'groupsIds',
        filtering: false,
        searchable: false,
        sorting: false,
        render: rowData => getArrayLengthStr(rowData['groupsIds']),
        editComponent: props => (
            <SelectGroupsDialog
                groups={groups != null ? groups : []}
                rowData={props.rowData}
                onChange={props.onChange}
            />
        )
    };
};

export const getColumnExpirationDate = () => {
    return {
        title: 'Expiration date',
        field: 'expirationDate',
        type: 'datetime',
        editable: 'always',
        filtering: false,
        render: rowData => (
            <div>
                {rowData != null ? rowData['expirationDate'] != null ? new Date(rowData['expirationDate']).toLocaleString() : null : null}
            </div>
        )
    };
};

export const getColumnCreatedAt = () => {
    return {
        title: 'Created at',
        field: 'createdAt',
        type: 'datetime',
        editable: 'never',
        filtering: false,
        render: rowData => (
            <div>
                {rowData != null ? rowData['createdAt'] != null ? new Date(rowData['createdAt']).toLocaleString() : null : null}
            </div>
        )
    };
};

export const getColumnCreatedBy = () => {
    return {
        title: 'Created by',
        field: 'createdBy',
        editable: 'never'
    };
};

export const getColumnModifiedAt = () => {
    return {
        title: 'Modified at',
        field: 'modifiedAt',
        type: 'datetime',
        editable: 'never',
        filtering: false,
        render: rowData => (
            <div>
                {rowData != null ? rowData['modifiedAt'] != null ? new Date(rowData['modifiedAt']).toLocaleString() : null : null}
            </div>
        )

    };
};

export const getColumnModifiedBy = () => {
    return {
        title: 'Modified by',
        field: 'modifiedBy',
        editable: 'never'
    };
};