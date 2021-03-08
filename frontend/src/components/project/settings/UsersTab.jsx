import MaterialTable from "material-table";
import React, {createRef, useContext, useEffect, useState} from "react";
import {MessageBox} from "../../utils/MessageBox";
import {
    getBaseUrl,
    getPageSizeOptions, getPutOptions, getSettingsTableHeight, getTableHeaderBackgroundColor,
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
import SelectTeamsDialog from "../dialogs/SelectTeamsDialog";
import {useWindowDimension} from "../../utils/UseWindowDimension";
import TeamsContext from "../../../context/teams/TeamsContext";

export default function UsersTab() {

    const theme = useTheme();
    const [height] = useWindowDimension();
    const [messageBox, setMessageBox] = useState({open: false, severity: 'error', title: '', message: ''});
    const [pageSize, setPageSize] = useState(getLastPageSize);
    const [filtering, setFiltering] = useState(false);
    const tableRef = createRef();
    const usersContext = useContext(UsersContext);
    const {users, fetchUsers, editUser} = usersContext;
    const rolesContext = useContext(RolesContext);
    const {roles, fetchRoles} = rolesContext;
    const teamsContext = useContext(TeamsContext);
    const {teams, fetchTeams, notifyTeams} = teamsContext;

    const changeableFields = ['id', 'username', 'rolesIds', 'teamsIds'];
    const userSpecification = {
        username: {title: 'Username', check: ['notEmpty', 'min1', 'max30']}
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
        if (teams == null)
            fetchTeams();
    }, [teams, fetchTeams]);


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
                        title: 'Teams', field: 'teamsIds', filtering: false, searchable: false, sorting: false,
                        render: rowData => getArrayLengthStr(rowData['teamsIds']),
                        editComponent: props => (
                            <SelectTeamsDialog
                                teams={teams != null ? teams : []}
                                rowData={props.rowData}
                                onChange={props.onChange}
                            />
                        )
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
                    maxBodyHeight: getSettingsTableHeight(height),
                    minBodyHeight: getSettingsTableHeight(height),
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
                                        if (!arraysEquals(newData, oldData, 'teamsIds'))
                                            notifyTeams('USER', user.id, user['teamsIds']);
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