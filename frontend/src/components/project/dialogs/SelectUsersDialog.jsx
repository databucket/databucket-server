import React from 'react';
import PropTypes from 'prop-types';
import SelectMultiDialog from "../../utils/SelectMultiDialog";
import {
    getColumnCreatedBy,
    getColumnCreatedDate
} from "../../utils/StandardColumns";
import {getRolesNames} from "../../../utils/JsonHelper";
import SelectMultiRolesLookup from "../../lookup/SelectMultiRolesLookup";
import {getUserIcon} from "../../../utils/MaterialTableHelper";

SelectUsersDialog.propTypes = {
    users: PropTypes.array.isRequired,
    roles: PropTypes.array.isRequired,
    rowData: PropTypes.object.isRequired,
    onChange: PropTypes.func.isRequired
}

export default function SelectUsersDialog(props) {

    const columns = [
        {filtering: false, cellStyle: { width: '1%'}, editable: 'never', searchable: false, sorting: false, render: (rowData) => getUserIcon(rowData)},
        {title: 'Name', field: 'username', editable: 'onAdd', filtering: true},
        {
            title: 'Roles', field: 'rolesIds', filtering: false, sorting: false,
            render: rowData => getRolesNames(props.roles, rowData['rolesIds']),
            editComponent: props => (
                <SelectMultiRolesLookup
                    rowData={props.rowData}
                    roles={props.roles}
                    onChange={props.onChange}
                />
            )
        },
        getColumnCreatedDate(),
        getColumnCreatedBy()
    ]

    return (
        <SelectMultiDialog
            columns={columns}
            data={props.users}
            ids={props.rowData['usersIds'] != null ? props.rowData['usersIds'] : []}
            tooltipTitle={'Select users'}
            dialogTitle={'Select users'}
            tableTitle={'Name: ' + props.rowData['name']}
            maxWidth='md' //'xs' | 'sm' | 'md' | 'lg' | 'xl' | false
            onChange={props.onChange}
        />
    );
}