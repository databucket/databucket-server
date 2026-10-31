import MaterialTable, {MTableActions} from "@material-table/core";
import React, {createRef, useContext, useEffect, useState} from "react";
import useTableState from "../../utils/TableState";
import {selectCellText} from "../../utils/TableCellSelect";
import {MessageBox} from "../utils/MessageBox";
import {
    getManagementTableHeight,
    getPageSizeOptions,
    getPostOptions,
    getPutOptions,
    getTableHeaderBackgroundColor,
    getTableRowBackgroundColor
} from "../../utils/MaterialTableHelper";
import {
    arraysEquals,
    convertNullValuesInObject,
    getArrayLengthStr,
    getSelectedValues,
    isItemChanged,
    validateItem
} from "../../utils/JsonHelper";
import {DeleteSweep, FilterList, Refresh} from "@mui/icons-material";
import {useTheme} from "@mui/material";
import {handleErrors} from "../../utils/FetchHelper";
import ProjectsContext from "../../context/projects/ProjectsContext";
import {
    getColumnCreatedAt,
    getColumnCreatedBy,
    getColumnDescription,
    getColumnEnabled,
    getColumnExpirationDate,
    getColumnId,
    getColumnModifiedAt,
    getColumnModifiedBy,
    getColumnName
} from "../utils/StandardColumns";
import ManageUsersContext from "../../context/users/ManageUsersContext";
import SelectUsersDialog from "../dialogs/SelectUsersDialog";
import RolesContext from "../../context/roles/RolesContext";
import {getManageProjectMapper} from "../../utils/NullValueMappers";
import ProjectContentDialog from "./ProjectContentDialog";
import {getBaseUrl} from "../../utils/UrlBuilder";

// The table puts its Edit and Delete after the other row actions; here the order is Edit | Clear project | Delete
const RowActions = (props) => {
    if (props.data == null)
        return <MTableActions {...props}/>;
    const actions = [...props.actions];
    const clear = actions.findIndex(action => action.tooltip === 'Clear project');
    if (clear >= 0) {
        const [clearAction] = actions.splice(clear, 1);
        const remove = actions.findIndex(action => typeof action === 'function' && action(props.data).tooltip === 'Delete');
        actions.splice(remove >= 0 ? remove : actions.length, 0, clearAction);
    }
    return <MTableActions {...props} actions={actions}/>;
}

export default function ProjectsTab() {

    const theme = useTheme();
    const [messageBox, setMessageBox] = useState({open: false, severity: 'error', title: '', message: ''});
    const [contentDialog, setContentDialog] = useState({project: null, mode: null});
    const tableState = useTableState('management-projects');
    const tableRef = createRef();
    const {projects, fetchProjects, addProject, editProject, removeProject} = useContext(ProjectsContext);
    const {users, fetchUsers, notifyUsers} = useContext(ManageUsersContext);
    const {roles, fetchRoles} = useContext(RolesContext);
    const changeableFields = ['id', 'publicVisible', 'enabled', 'name', 'description', 'usersIds', 'expirationDate'];
    const projectSpecification = {
        name: {title: 'Name', check: ['notEmpty', 'min1', 'max30']},
        description: {title: 'Description', check: ['max250']}
    };

    useEffect(() => {
        if (users == null) {
            fetchUsers();
        }
    }, [users, fetchUsers]);

    useEffect(() => {
        if (projects == null) {
            fetchProjects();
        }
    }, [projects, fetchProjects]);

    useEffect(() => {
        if (roles == null) {
            fetchRoles();
        }
    }, [roles, fetchRoles]);
    // clearing or deleting a project, confirmed in ProjectContentDialog
    const onContentRemoved = (project) => {
        const deleted = contentDialog.mode === 'delete';
        setContentDialog({project: null, mode: null});
        if (deleted)
            removeProject(project.id);
        else
            editProject({...project, empty: true});
        setMessageBox({
            open: true,
            severity: 'success',
            title: deleted ? `Project "${project.name}" deleted` : `Project "${project.name}" cleared`,
            message: ''
        });
    }

    return (
        <div onDoubleClick={selectCellText}>
            <MaterialTable
                title='Projects'
                tableRef={tableRef}
                columns={tableState.columns([
                    getColumnId(),
                    {
                        title: 'Public',
                        field: 'publicVisible',
                        type: 'boolean',
                        width: '1%',
                        cellStyle: {width: '1%'}
                    },
                    getColumnEnabled(),
                    getColumnName(),
                    getColumnDescription('20%'),
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
                    getColumnCreatedAt(),
                    getColumnCreatedBy(),
                    getColumnModifiedAt(),
                    getColumnModifiedBy()
                ])}
                data={tableState.data(projects != null ? projects : [])}
                key={tableState.key}
                {...tableState.callbacks}
                options={{
                    ...tableState.options,
                    pageSizeOptions: getPageSizeOptions(),
                    paginationType: 'stepped',
                    actionsColumnIndex: -1,
                    maxColumnSort: 1,
                    search: true,
                    filtering: tableState.filtering,
                    debounceInterval: 700,
                    padding: 'dense',
                    cellStyle: {whiteSpace: 'nowrap'},
                    headerStyle: {
                        position: 'sticky',
                        top: 0,
                        backgroundColor: getTableHeaderBackgroundColor(theme)
                    },
                    maxBodyHeight: getManagementTableHeight(),
                    minBodyHeight: getManagementTableHeight(),
                    rowStyle: rowData => ({backgroundColor: getTableRowBackgroundColor(rowData, theme)})
                }}
                components={{
                    Container: props => <div {...props} />,
                    Actions: RowActions
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
                        onClick: tableState.toggleFiltering

                    },
                    {
                        icon: () => <DeleteSweep/>,
                        tooltip: 'Clear project',
                        onClick: (event, rowData) => setContentDialog({project: rowData, mode: 'clear'})
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
                                    if (!arraysEquals(newData, oldData, 'usersIds')) {
                                        notifyUsers('PROJECT', project.id, project['usersIds']);
                                    }
                                    resolve();
                                }
                            });
                        }),

                    // only an empty project can be deleted: clear it first (the row action next to it)
                    isDeletable: rowData => rowData.empty === true,
                    // the table only opens the dialog, which asks for the name and deletes
                    onRowDelete: oldData =>
                        new Promise((resolve) => {
                            setContentDialog({project: oldData, mode: 'delete'});
                            resolve();
                        }),
                }}
                localization={{
                    body: {
                        editRow: {deleteText: 'Delete this empty project?'}
                    }
                }}
            />
            <MessageBox
                config={messageBox}
                onClose={() => setMessageBox({...messageBox, open: false})}
            />
            <ProjectContentDialog
                project={contentDialog.project}
                mode={contentDialog.mode}
                onClose={() => setContentDialog({project: null, mode: null})}
                onDone={onContentRemoved}
            />
        </div>
    )
}
