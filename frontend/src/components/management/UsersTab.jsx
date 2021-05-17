import MaterialTable from "material-table";
import React, {createRef, useContext, useEffect, useState} from "react";
import {MessageBox} from "../utils/MessageBox";
import {
    getManagementTableHeight,
    getPageSizeOptions, getPostOptions, getPutOptions, getTableHeaderBackgroundColor, getTableRowBackgroundColor, getUserIcon
} from "../../utils/MaterialTableHelper";
import {getLastPageSize, setLastPageSize} from "../../utils/ConfigurationStorage";
import {
    arraysEquals,
    convertNullValuesInObject, getArrayLengthStr, getRolesNames,
    getSelectedValues,
    isItemChanged,
    validateItem
} from "../../utils/JsonHelper";
import Refresh from "@material-ui/icons/Refresh";
import FilterList from "@material-ui/icons/FilterList";
import ResetPasswordIcon from "@material-ui/icons/VpnKey";
import SelectMultiRolesLookup from "../lookup/SelectMultiRolesLookup";
import SelectProjectsDialog from "../dialogs/SelectProjectsDialog";
import {useTheme} from "@material-ui/core/styles";
import {handleErrors} from "../../utils/FetchHelper";
import ResetPasswordDialog from "./ResetPasswordDialog";
import RolesContext from "../../context/roles/RolesContext";
import ManageUsersContext from "../../context/users/ManageUsersContext";
import ProjectsContext from "../../context/projects/ProjectsContext";
import {
    getColumnCreatedBy,
    getColumnCreatedAt,
    getColumnEnabled,
    getColumnExpirationDate,
    getColumnModifiedBy, getColumnModifiedAt,
} from "../utils/StandardColumns";
import {getManageUserMapper} from "../../utils/NullValueMappers";
import {useWindowDimension} from "../utils/UseWindowDimension";
import {getBaseUrl} from "../../utils/UrlBuilder";

export default function UsersTab() {

    const theme = useTheme();
    const [height] = useWindowDimension();
    const [messageBox, setMessageBox] = useState({open: false, severity: 'error', title: '', message: ''});
    const [resetPasswordDialog, setResetPasswordDialog] = useState({open: false, username: null});
    const [pageSize, setPageSize] = useState(getLastPageSize);
    const [filtering, setFiltering] = useState(false);
    const tableRef = createRef();
    const usersContext = useContext(ManageUsersContext);
    const {users, fetchUsers, addUser, editUser} = usersContext;
    const rolesContext = useContext(RolesContext);
    const {roles, fetchRoles} = rolesContext;
    const projectsContext = useContext(ProjectsContext);
    const {projects, fetchProjects, notifyProjects} = projectsContext;
    const changeableFields = ['id', 'username', 'enabled', 'expirationDate', 'rolesIds', 'projectsIds'];
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
        if (projects == null)
            fetchProjects();
    }, [projects, fetchProjects]);

    const onChangeRowsPerPage = (pageSize) => {
        setPageSize(pageSize);
        setLastPageSize(pageSize);
    }

    return (
        <div>
            <MaterialTable
                title='Users'
                tableRef={tableRef}
                columns={[
                    {filtering: false, cellStyle: { width: '1%'}, editable: 'never', searchable: false, sorting: false, render: (rowData) => getUserIcon(rowData)},
                    getColumnEnabled(),
                    {title: 'Name', field: 'username', editable: 'onAdd', filtering: true},
                    {
                        title: 'Roles', field: 'rolesIds', filtering: false, sorting: false,
                        render: rowData => getRolesNames(roles, rowData['rolesIds']),
                        editComponent: props => <SelectMultiRolesLookup rowData={props.rowData} roles={roles}
                                                                        onChange={props.onChange}/>
                    },
                    {
                        title: 'Projects', field: 'projectsIds', filtering: false, searchable: false, sorting: false,
                        render: rowData => getArrayLengthStr(rowData['projectsIds']),
                        editComponent: props => (
                            <SelectProjectsDialog
                                projects={projects != null ? projects : []}
                                rowData={props.rowData}
                                onChange={props.onChange}
                            />
                        )
                    },
                    getColumnExpirationDate(),
                    getColumnCreatedAt(),
                    getColumnCreatedBy(),
                    getColumnModifiedAt(),
                    getColumnModifiedBy()
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
                    maxBodyHeight: getManagementTableHeight(height),
                    minBodyHeight: getManagementTableHeight(height),
                    rowStyle: rowData => ({backgroundColor: getTableRowBackgroundColor(rowData, theme)})
                }}
                components={{
                    Container: props => <div {...props} />
                }}
                actions={[
                    rowData => ({
                        icon: () => <ResetPasswordIcon/>,
                        tooltip: 'Reset Password',
                        onClick: (event, rowData) => {
                            setResetPasswordDialog({open: true, username: rowData.username});
                        }
                    }),
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
                    onRowAdd: newData =>
                        new Promise((resolve, reject) => {
                            let message = validateItem(newData, userSpecification);
                            if (message != null) {
                                setMessageBox({
                                    open: true,
                                    severity: 'warning',
                                    title: 'Item is not valid',
                                    message: message
                                });
                                reject();
                                return;
                            }

                            fetch(getBaseUrl('manage/users'), getPostOptions(newData))
                                .then(handleErrors)
                                .catch(error => {
                                    setMessageBox({open: true, severity: 'error', title: 'Error', message: error});
                                    reject();
                                })
                                .then((user) => {
                                    if (user != null) {
                                        addUser(convertNullValuesInObject(user, getManageUserMapper()));
                                        notifyProjects('USER', user.id, user['projectsIds']);
                                        resolve();
                                    }
                                });
                        }),
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

                            fetch(getBaseUrl('manage/users'), getPutOptions(payload))
                                .then(handleErrors)
                                .catch(error => {
                                    setMessageBox({open: true, severity: 'error', title: 'Error', message: error});
                                    reject();
                                })
                                .then((user) => {
                                    if (user != null) {
                                        editUser(convertNullValuesInObject(user, getManageUserMapper()));
                                        if (!arraysEquals(newData, oldData, 'projectsIds'))
                                            notifyProjects('USER', user.id, user['projectsIds']);
                                        resolve();
                                    }
                                });
                        })
                }}
            />
            <ResetPasswordDialog
                open={resetPasswordDialog.open}
                username={resetPasswordDialog.username}
                onClose={() => setResetPasswordDialog({...resetPasswordDialog, open: false})}
            />
            <MessageBox
                config={messageBox}
                onClose={() => setMessageBox({...messageBox, open: false})}
            />
        </div>
    )
}