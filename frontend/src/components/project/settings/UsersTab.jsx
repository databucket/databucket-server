import MaterialTable from "material-table";
import React, {createRef, useContext, useEffect, useState} from "react";
import {MessageBox} from "../../utils/MessageBox";
import {
    getBaseUrl,
    getPageSizeOptions, getPutOptions, getTableHeaderBackgroundColor,
    getTableIcons, getTableRowBackgroundColor, getUserIcon
} from "../../../utils/MaterialTableHelper";
import {getLastPageSize, setLastPageSize} from "../../../utils/ConfigurationStorage";
import {
    arraysEquals,
    convertNullValuesInObject, getArrayLengthStr, getRolesNames,
    getSelectedValues,
    isItemChanged,
    validateItem
} from "../../../utils/JsonHelper";
import Refresh from "@material-ui/icons/Refresh";
import FilterList from "@material-ui/icons/FilterList";
import {useTheme} from "@material-ui/core/styles";
import {handleErrors} from "../../../utils/FetchHelper";
import RolesContext from "../../../context/roles/RolesContext";
import UsersContext from "../../../context/users/UsersContext";
import {
    getColumnCreatedBy,
    getColumnCreatedDate,
    getColumnLastModifiedBy, getColumnLastModifiedDate,
} from "../../utils/StandardColumns";
import {getManageUserMapper} from "../../../utils/NullValueMappers";
import SelectGroupsDialog from "../dialogs/SelectGroupsDialog";
import GroupsContext from "../../../context/groups/GroupsContext";
import SelectBucketsDialog from "../dialogs/SelectBucketsDialog";
import BucketsContext from "../../../context/buckets/BucketsContext";

export default function UsersTab() {

    const theme = useTheme();
    const [messageBox, setMessageBox] = useState({open: false, severity: 'error', title: '', message: ''});
    const [pageSize, setPageSize] = useState(getLastPageSize);
    const [filtering, setFiltering] = useState(false);
    const tableRef = createRef();
    const usersContext = useContext(UsersContext);
    const {users, fetchUsers, editUser} = usersContext;
    const rolesContext = useContext(RolesContext);
    const {roles, fetchRoles} = rolesContext;
    const groupContext = useContext(GroupsContext);
    const {groups, fetchGroups, notifyGroups} = groupContext;
    const bucketsContext = useContext(BucketsContext);
    const {buckets, fetchBuckets, notifyBuckets} = bucketsContext;

    const changeableFields = ['id', 'username', 'rolesIds', 'groupsIds', 'bucketsIds', 'viewsIds'];
    const userSpecification = {
        username: {title: 'Username', check: ['notEmpty', 'min3', 'max50']}
    };

    useEffect(() => {
        if (users == null)
            fetchUsers();
    }, [users, fetchUsers]);

    useEffect(() => {
        if (roles == null)
            fetchRoles();
    }, [roles, fetchRoles]);

    useEffect(() => {
        if (groups == null)
            fetchGroups();
    }, [groups, fetchGroups]);

    useEffect(() => {
        if (buckets == null)
            fetchBuckets();
    }, [buckets, fetchBuckets]);

    const onChangeRowsPerPage = (pageSize) => {
        setPageSize(pageSize);
        setLastPageSize(pageSize);
    }

    return (
        <div>
            <MaterialTable
                icons={getTableIcons()}
                title='Users'
                tableRef={tableRef}
                columns={[
                    {filtering: false, cellStyle: { width: '1%'}, editable: 'never', searchable: false, sorting: false, render: (rowData) => getUserIcon(rowData)},
                    {title: 'Name', field: 'username', editable: 'onAdd', filtering: true,},
                    {
                        title: 'Roles', field: 'rolesIds', filtering: false, editable: 'never', sorting: false,
                        render: rowData => getRolesNames(roles, rowData['rolesIds'])
                    },
                    {
                        title: 'Groups', field: 'groupsIds', filtering: false, searchable: false, sorting: false,
                        render: rowData => getArrayLengthStr(rowData['groupsIds']),
                        editComponent: props => (
                            <SelectGroupsDialog
                                groups={groups != null ? groups : []}
                                rowData={props.rowData}
                                onChange={props.onChange}
                            />
                        )
                    },
                    {
                        title: 'Buckets', field: 'bucketsIds', filtering: false, searchable: false, sorting: false,
                        render: rowData => getArrayLengthStr(rowData['bucketsIds']),
                        editComponent: props => (
                            <SelectBucketsDialog
                                buckets={buckets != null ? buckets : []}
                                rowData={props.rowData}
                                onChange={props.onChange}
                            />
                        )
                    },
                    {
                        title: 'View',
                        field: 'viewsIds',
                        filtering: false,
                        searchable: false,
                        sorting: false,
                        editable: 'never',
                        render: rowData => getArrayLengthStr(rowData['viewsIds']),
                    },
                    getColumnCreatedDate(),
                    getColumnCreatedBy(),
                    getColumnLastModifiedDate(),
                    getColumnLastModifiedBy()
                ]}
                data={users != null ? users : []}
                onChangeRowsPerPage={onChangeRowsPerPage}
                options={{
                    pageSize: pageSize,
                    pageSizeOptions: getPageSizeOptions(),
                    paginationType: 'stepped',
                    actionsColumnIndex: -1,
                    sorting: true,
                    search: true,
                    filtering: filtering,
                    debounceInterval: 700,
                    padding: 'dense',
                    headerStyle: {backgroundColor: getTableHeaderBackgroundColor(theme)},
                    rowStyle: rowData => ({backgroundColor: getTableRowBackgroundColor(rowData, theme)})
                }}
                components={{
                    Container: props => <div {...props} />
                }}
                actions={[
                    {
                        icon: () => <Refresh/>,
                        tooltip: 'Refresh',
                        isFreeAction: true,
                        onClick: () => fetchUsers()
                    },
                    {
                        icon: () => <FilterList/>,
                        tooltip: 'Enable/disable filter',
                        isFreeAction: true,
                        onClick: () => setFiltering(!filtering)

                    }
                ]}
                editable={{
                    onRowUpdate: (newData, oldData) =>
                        new Promise((resolve, reject) => {
                            if (!isItemChanged(oldData, newData, changeableFields)) {
                                setMessageBox({
                                    open: true,
                                    severity: 'info',
                                    title: 'Nothing changed',
                                    message: ''
                                });
                                reject();
                                return;
                            }

                            let message = validateItem(newData, userSpecification);
                            if (message != null) {
                                setMessageBox({
                                    open: true,
                                    severity: 'warning',
                                    title: 'Item is not valid!',
                                    message: message
                                });
                                reject();
                                return;
                            }

                            const payload = getSelectedValues(newData, changeableFields);

                            fetch(getBaseUrl('users'), getPutOptions(payload))
                                .then(handleErrors)
                                .catch(error => {
                                    setMessageBox({open: true, severity: 'error', title: 'Error', message: error});
                                    reject();
                                })
                                .then((user) => {
                                    if (user != null) {
                                        editUser(convertNullValuesInObject(user, getManageUserMapper()));
                                        if (!arraysEquals(newData, oldData, 'groupsIds'))
                                            notifyGroups('USER', user.id, user['groupsIds']);
                                        if (!arraysEquals(newData, oldData, 'bucketsIds'))
                                            notifyBuckets('USER', user.id, user['bucketsIds']);
                                        resolve();
                                    }
                                });
                        })
                }}
            />
            <MessageBox
                config={messageBox}
                onClose={() => setMessageBox({...messageBox, open: false})}
            />
        </div>
    )
}