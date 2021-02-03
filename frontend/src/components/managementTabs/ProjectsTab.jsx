import MaterialTable from "material-table";
import React, {useState} from "react";
import {MessageBox} from "../MessageBox";
import {
    getBaseUrl, getDeleteOptions,
    getGetOptions,
    getPageableUlr,
    getPageSizeOptions, getPostOptions, getPutOptions, getTableHeaderBackgroundColor,
    getTableIcons, getTableRowBackgroundColor
} from "../../utils/MaterialTableHelper";
import {getLastPageSize, setLastPageSize} from "../../utils/ConfigurationStorage";
import {convertNullValues, isItemChanged, validateItem} from "../../utils/JsonHelper";
import Refresh from "@material-ui/icons/Refresh";
import FilterList from "@material-ui/icons/FilterList";
import {useTheme} from "@material-ui/core/styles";
import {handleErrors} from "../../utils/FetchHelper";

export default function ProjectsTab() {

    const theme = useTheme();
    const [messageBox, setMessageBox] = useState({open: false, severity: 'error', title: '', message: ''});
    const [pageSize, setPageSize] = useState(getLastPageSize);
    const [filtering, setFiltering] = useState(false);
    const tableRef = React.createRef();
    const projectSpecification = {
        name: {title: 'Name', check: ['notEmpty', 'min3', 'max50']},
        description: {title: 'Description', check: ['max250']}
    };

    return (
        <div>
            <MaterialTable
                icons={getTableIcons()}
                title='Projects'
                tableRef={tableRef}
                columns={[
                    {title: 'Id', field: 'id', type: 'numeric', editable: 'never', filtering: true, defaultSort: 'asc'},
                    {title: 'Enabled', field: 'enabled', type: 'boolean'},
                    {title: 'Name', field: 'name', type: 'string', editable: 'always', filtering: true},
                    {title: 'Description', field: 'description', type: 'string', editable: 'always', filtering: true},
                    {
                        title: 'Expiration date',
                        field: 'expirationDate',
                        type: 'datetime',
                        editable: 'always',
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
                    {title: 'Last modified by', field: 'lastModifiedBy', editable: 'never'},
                ]}
                data={query =>
                    new Promise((resolve) => {
                        if (pageSize !== query.pageSize) {
                            setPageSize(query.pageSize);
                            setLastPageSize(query.pageSize);
                        }

                        fetch(getPageableUlr("projects", query, filtering), getGetOptions())
                            .then(handleErrors)
                            .catch(error => {
                                setMessageBox({open: true, severity: 'error', title: 'Error', message: error});
                                resolve();
                            })
                            .then(result => {
                                resolve({
                                    data: convertNullValues(result.projects, ['description']),
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

                            fetch(getBaseUrl('projects'), getPostOptions(newData))
                                .then(handleErrors)
                                .catch(error => {
                                    reject();
                                    setMessageBox({open: true, severity: 'error', title: 'Error', message: error});
                                })
                                .then(() => {
                                    resolve();
                                });
                        }),

                    onRowUpdate: (newData, oldData) =>
                        new Promise((resolve, reject) => {

                            if (!isItemChanged(oldData, newData, ['name', 'description', 'enabled', 'expirationDate'])) {
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
                                    severity: 'Item is not valid',
                                    title: '',
                                    message: message
                                });
                                reject();
                                return;
                            }

                            fetch(getBaseUrl('projects'), getPutOptions(newData))
                                .then(handleErrors)
                                .catch(error => {
                                    setMessageBox({open: true, severity: 'error', title: 'Error', message: message});
                                    reject();
                                })
                                .then(() => {
                                    resolve();
                                });


                        }),

                    onRowDelete: oldData =>
                        new Promise((resolve, reject) => {
                            setTimeout(() => {
                                fetch(getBaseUrl(`projects/${oldData.id}`), getDeleteOptions())
                                    .then(handleErrors)
                                    .catch(error => {
                                        setMessageBox({open: true, severity: 'error', title: 'Error', message: error});
                                        reject();
                                    })
                                    .then(() => {
                                        resolve();
                                    });

                            }, 300);
                        }),
                }}
            />
            <MessageBox
                config={messageBox}
                onClose={() => setMessageBox({...messageBox, open: false})}
            />
        </div>
    )
}