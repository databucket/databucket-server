import MaterialTable from "material-table";
import React, {useEffect, useState} from "react";
import {MessageBox} from "../MessageBox";
import {
    getBaseUrl,
    getGetOptions,
    getPageableUlr,
    getPageSizeOptions, getPostOptions, getPutOptions, getTableHeaderBackgroundColor,
    getTableIcons, getTableRowBackgroundColor
} from "../../utils/MaterialTableHelper";
import {getLastPageSize, setLastPageSize} from "../../utils/ConfigurationStorage";
import {
    convertNullValues,
    getProjectsIdsStr, getRolesNames,
    getSelectedValues,
    isItemChanged,
    validateItem
} from "../../utils/JsonHelper";
import Refresh from "@material-ui/icons/Refresh";
import FilterList from "@material-ui/icons/FilterList";
import ResetPasswordIcon from "@material-ui/icons/VpnKey";
import RolesLookup from "../lookup/RolesLookup";
import ProjectsDialog from "./ProjectsDialog";
import {useTheme} from "@material-ui/core/styles";
import {handleErrors} from "../../utils/FetchHelper";
import ResetPasswordDialog from "./ResetPasswordDialog";

export default function UsersTab(props) {

    const theme = useTheme();
    const [projects, setProjects] = useState(null);
    const [messageBox, setMessageBox] = useState({open: false, severity: 'error', title: '', message: ''});
    const [resetPasswordDialog, setResetPasswordDialog] = useState({open: false, username: null});
    const [pageSize, setPageSize] = useState(getLastPageSize);
    const [filtering, setFiltering] = useState(false);
    const tableRef = React.createRef();
    const roles = props.roles;
    const userSpecification = {
        username: {title: 'Username', check: ['notEmpty', 'min3', 'max50']}
    };

    useEffect(() => {
        fetch(getBaseUrl("projects/all"), getGetOptions())
            .then(handleErrors)
            .catch(error => {
                setMessageBox({open: true, severity: 'error', title: 'Fetch projects error', message: error});
            })
            .then(result => {
                setProjects(convertNullValues(result, ['description']));
            });
    }, []);

    return (
        <div>
            <MaterialTable
                icons={getTableIcons()}
                title='Users'
                tableRef={tableRef}
                columns={[
                    {title: 'Enabled', field: 'enabled', type: 'boolean'},
                    {title: 'Name', field: 'username', editable: 'onAdd', filtering: true},
                    {
                        title: 'Roles', field: 'rolesIds', filtering: false,
                        render: rowData => getRolesNames(roles, rowData['rolesIds']),
                        editComponent: props => <RolesLookup rowData={props.rowData} roles={roles}
                                                             onChange={props.onChange}/>
                    },
                    {
                        title: 'Projects', field: 'projectsIds', filtering: false, searchable: false, sorting: false,
                        render: rowData => getProjectsIdsStr(rowData['projectsIds']),
                        editComponent: props => <ProjectsDialog userRowData={props.rowData}
                                                                projects={projects}
                                                                onChange={props.onChange}/>
                    },
                    {
                        title: 'Expiration date',
                        field: 'expirationDate',
                        type: 'datetime',
                        editable: 'always',
                        emptyValue: '',
                        filtering: false,
                        render: rowData =>
                            <div>{rowData != null ? rowData['expirationDate'] != null ? new Date(rowData['expirationDate']).toLocaleString() : null : null}</div>,
                    },
                    {
                        title: 'Created date',
                        field: 'createdDate',
                        type: 'datetime',
                        editable: 'never',
                        filtering: false,
                        defaultSort: 'asc',
                        render: rowData =>
                            <div>{rowData != null ? rowData['createdDate'] != null ? new Date(rowData['createdDate']).toLocaleString() : null : null}</div>,
                    },
                    {title: 'Created by', field: 'createdBy', editable: 'never'},
                    {
                        title: 'Last modified date',
                        field: 'lastModifiedDate',
                        type: 'datetime',
                        editable: 'never',
                        filtering: false,
                        render: rowData =>
                            <div>{rowData != null ? rowData['lastModifiedDate'] != null ? new Date(rowData['lastModifiedDate']).toLocaleString() : null : null}</div>,
                    },
                    {title: 'Last modified by', field: 'lastModifiedBy', editable: 'never'}
                ]}
                data={query =>
                    new Promise((resolve) => {
                        if (pageSize !== query.pageSize) {
                            setPageSize(query.pageSize);
                            setLastPageSize(query.pageSize);
                        }

                        fetch(getPageableUlr("users", query, filtering), getGetOptions())
                            .then(handleErrors)
                            .catch(error => {
                                setMessageBox({open: true, severity: 'error', title: 'Error', message: error});
                                resolve();
                            })
                            .then(result => {
                                resolve({
                                    data: result['users'],
                                    page: result.page,
                                    totalCount: result.total,
                                })
                            });
                    })
                }
                options={{
                    pageSize: pageSize,
                    pageSizeOptions: getPageSizeOptions(),
                    paginationType: 'stepped',
                    actionsColumnIndex: -1,
                    sorting: true,
                    search: false,
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
                    rowData => ({
                        icon: () => <ResetPasswordIcon/>,
                        tooltip: 'Reset Password',
                        onClick: (event, rowData)  => {
                            setResetPasswordDialog({open: true, username: rowData.username});
                        }
                    }),
                    {
                        icon: () => <Refresh/>,
                        tooltip: 'Refresh',
                        isFreeAction: true,
                        onClick: (query) => {
                            tableRef.current !== null && tableRef.current.onQueryChange(query);
                        }
                    },
                    {
                        icon: () => <FilterList/>,
                        tooltip: 'Enable/disable filter',
                        isFreeAction: true,
                        onClick: (query) => {
                            setFiltering(!filtering);

                            // after switch filtering off/on
                            if (tableRef.current.state.query.filters.length > 0) {
                                if (tableRef.current)
                                    tableRef.current.onQueryChange(query);
                            }
                        }
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

                            let result_ok = true;
                            fetch(getBaseUrl('users'), getPostOptions(newData))
                                .then(handleErrors)
                                .catch(error => {
                                    result_ok = false;
                                    setMessageBox({open: true, severity: 'error', title: 'Error', message: error});
                                    reject();
                                })
                                .then(() => {
                                    result_ok === true ? resolve() : reject();
                                });
                        }),
                    onRowUpdate: (newData, oldData) =>
                        new Promise((resolve, reject) => {
                            const keys = ['username', 'enabled', 'expirationDate', 'rolesIds', 'projectsIds'];
                            if (!isItemChanged(oldData, newData, keys)) {
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

                            const payload = getSelectedValues(newData, keys);

                            fetch(getBaseUrl('users'), getPutOptions(payload))
                                .then(handleErrors)
                                .catch(error => {
                                    setMessageBox({open: true, severity: 'error', title: 'Error', message: error});
                                    reject();
                                })
                                .then(() => {
                                    resolve();
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