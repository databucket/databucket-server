import MaterialTable from "material-table";
import React, {createRef, useContext, useEffect, useState} from "react";
import {MessageBox} from "../utils/MessageBox";
import {
    getBaseUrl, getDeleteOptions,
    getPageSizeOptions, getPostOptions, getPutOptions, getTableHeaderBackgroundColor,
    getTableIcons, getTableRowBackgroundColor
} from "../../utils/MaterialTableHelper";
import {getLastPageSize, setLastPageSize} from "../../utils/ConfigurationStorage";
import {
    arraysEquals,
    convertNullValuesInObject,
    getArrayLengthStr,
    getSelectedValues,
    isItemChanged,
    validateItem
} from "../../utils/JsonHelper";
import Refresh from "@material-ui/icons/Refresh";
import FilterList from "@material-ui/icons/FilterList";
import {useTheme} from "@material-ui/core/styles";
import {handleErrors} from "../../utils/FetchHelper";
import ProjectsContext from "../../context/projects/ProjectsContext";
import {
    getColumnCreatedBy,
    getColumnCreatedDate, getColumnDescription,
    getColumnEnabled,
    getColumnExpirationDate,
    getColumnId, getColumnLastModifiedBy, getColumnLastModifiedDate,
    getColumnName
} from "../utils/StandardColumns";
import ManageUsersContext from "../../context/users/ManageUsersContext";
import SelectUsersDialog from "../project/dialogs/SelectUsersDialog";
import RolesContext from "../../context/roles/RolesContext";
import {getManageProjectMapper} from "../../utils/NullValueMappers";
import ConfirmRemovingDialog from "../utils/ConfirmRemovingDialog";

export default function ProjectsTab() {

    const theme = useTheme();
    const [messageBox, setMessageBox] = useState({open: false, severity: 'error', title: '', message: ''});
    const [confirmRemove, setConfirmRemove] = useState({open: false, id: 0, name: ''});
    const [pageSize, setPageSize] = useState(getLastPageSize);
    const [filtering, setFiltering] = useState(false);
    const tableRef = createRef();
    const projectsContext = useContext(ProjectsContext);
    const {projects, fetchProjects, addProject, editProject, removeProject} = projectsContext;
    const usersContext = useContext(ManageUsersContext);
    const {users, fetchUsers, notifyUsers} = usersContext;
    const rolesContext = useContext(RolesContext);
    const {roles, fetchRoles} = rolesContext;
    const changeableFields = ['id', 'enabled', 'name', 'description', 'usersIds', 'expirationDate'];
    const projectSpecification = {
        name: {title: 'Name', check: ['notEmpty', 'min1', 'max30']},
        description: {title: 'Description', check: ['max250']}
    };

    useEffect(() => {
        if (users == null)
            fetchUsers();
    }, [users, fetchUsers]);

    useEffect(() => {
        if (projects == null)
            fetchProjects();
    }, [projects, fetchProjects]);

    useEffect(() => {
        if (roles == null)
            fetchRoles();
    }, [roles, fetchRoles]);

    const onChangeRowsPerPage = (pageSize) => {
        setPageSize(pageSize);
        setLastPageSize(pageSize);
    }

    const onRemove = (remove) => {
        if (remove) {
            // TODO temporary interruption action
            setConfirmRemove({open: false, id: 0, name: ''});
            setMessageBox({open: true, severity: 'info', title: 'This action is under development!', message: ''});
            if (remove) return;

            setTimeout(() => {
                fetch(getBaseUrl(`manage/projects/${confirmRemove.id}`), getDeleteOptions())
                    .then(handleErrors)
                    .catch(error => {
                        setMessageBox({open: true, severity: 'error', title: 'Error', message: error});
                    })
                    .then(() => {
                        removeProject(confirmRemove.id);
                    });
            }, 100);
        }

        setConfirmRemove({open: false, id: 0, name: ''});
    }

    return (
        <div>
            <MaterialTable
                icons={getTableIcons()}
                title='Projects'
                tableRef={tableRef}
                columns={[
                    getColumnId(),
                    getColumnEnabled(),
                    getColumnName(),
                    getColumnDescription(),
                    getColumnExpirationDate(),
                    {
                        title: 'Users', field: 'usersIds', filtering: false, searchable: false, sorting: false,
                        render: rowData => getArrayLengthStr(rowData['usersIds']),
                        editComponent: props => (
                            <SelectUsersDialog
                                users={users != null ? users : []}
                                roles={roles != null ? roles : []}
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
                data={projects != null ? projects : []}
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
                        onClick: () => fetchProjects()
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
                            let message = validateItem(newData, projectSpecification);
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

                            fetch(getBaseUrl('manage/projects'), getPostOptions(newData))
                                .then(handleErrors)
                                .catch(error => {
                                    reject();
                                    setMessageBox({open: true, severity: 'error', title: 'Error', message: error});
                                })
                                .then((project) => {
                                    if (project != null) {
                                        addProject(convertNullValuesInObject(project, getManageProjectMapper()));
                                        notifyUsers('PROJECT', project.id, project['usersIds']);
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

                            let message = validateItem(newData, projectSpecification);
                            if (message != null) {
                                setMessageBox({
                                    open: true,
                                    severity: 'error',
                                    title: 'Item is not valid',
                                    message: message
                                });
                                reject();
                                return;
                            }

                            const payload = getSelectedValues(newData, changeableFields);

                            fetch(getBaseUrl('manage/projects'), getPutOptions(payload))
                                .then(handleErrors)
                                .catch(error => {
                                    setMessageBox({open: true, severity: 'error', title: 'Error', message: error});
                                    reject();
                                })
                                .then((project) => {
                                    if (project != null) {
                                        editProject(convertNullValuesInObject(project, getManageProjectMapper()));
                                        if (!arraysEquals(newData, oldData, 'usersIds'))
                                            notifyUsers('PROJECT', project.id, project['usersIds']);
                                        resolve();
                                    }
                                });
                        }),

                    onRowDelete: oldData =>
                        new Promise((resolve) => {
                            setConfirmRemove({open: true, id: oldData.id, name: oldData.name});
                            resolve();
                        }),
                }}
            />
            <MessageBox
                config={messageBox}
                onClose={() => setMessageBox({...messageBox, open: false})}
            />
            <ConfirmRemovingDialog
                open={confirmRemove.open}
                name={confirmRemove.name}
                message={'Remove project:'}
                onClose={(remove) => onRemove(remove)}
            />
        </div>
    )
}