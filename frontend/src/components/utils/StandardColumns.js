import React from 'react';
import {getArrayLengthStr} from "../../utils/JsonHelper";
import SelectBucketsDialog from "../project/dialogs/SelectBucketsDialog";
import SelectClassesDialog from "../project/dialogs/SelectClassesDialog";
import SelectUsersDialog from "../project/dialogs/SelectUsersDialog";
import SelectGroupsDialog from "../project/dialogs/SelectGroupsDialog";

export const getColumnId = () => {
    return {
        title: 'Id',
        field: 'id',
        type: 'numeric',
        editable: 'never',
        filtering: true,
        cellStyle: {width: '1%'}
    };
};

export const getColumnName = () => {
    return {
        title: 'Name',
        field: 'name',
        type: 'string',
        editable: 'always',
        filtering: true
    };
};

export const getColumnDescription = () => {
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
        type: 'boolean'
    };
};

export const getColumnPrivate = () => {
    return {
        title: 'Private',
        field: 'privateItem',
        type: 'boolean'
    };
};

const CLASS_DEFAULT = 'none';
export const getColumnClass = (classesLookup) => {
    return {
        title: 'Class',
        field: 'classId',
        initialEditValue: CLASS_DEFAULT,
        emptyValue: CLASS_DEFAULT,
        lookup: classesLookup
    };
};

export const getColumnClasses = (classes) => {
    return {
        title: 'Classes', field: 'classesIds', filtering: false, searchable: false, sorting: false,
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

export const getColumnBuckets = (buckets) => {
    return {
        title: 'Buckets', field: 'bucketsIds', filtering: false, searchable: false, sorting: false,
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

export const getColumnUsers = (users, roles) => {
    return {
        title: 'Users',
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

export const getColumnGroups = (groups) => {
    return {
        title: 'Groups', field: 'groupsIds', filtering: false, searchable: false, sorting: false,
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

export const getColumnCreatedDate = () => {
    return {
        title: 'Created date',
        field: 'createdDate',
        type: 'datetime',
        editable: 'never',
        filtering: false,
        render: rowData => (
            <div>
                {rowData != null ? rowData['createdDate'] != null ? new Date(rowData['createdDate']).toLocaleString() : null : null}
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

export const getColumnLastModifiedDate = () => {
    return {
        title: 'Modified date',
        field: 'lastModifiedDate',
        type: 'datetime',
        editable: 'never',
        filtering: false,
        render: rowData => (
            <div>
                {rowData != null ? rowData['lastModifiedDate'] != null ? new Date(rowData['lastModifiedDate']).toLocaleString() : null : null}
            </div>
        )

    };
};

export const getColumnLastModifiedBy = () => {
    return {
        title: 'Modified by',
        field: 'lastModifiedBy',
        editable: 'never'
    };
};