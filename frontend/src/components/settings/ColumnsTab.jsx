import MaterialTable from "material-table";
import React, {useContext, useEffect, useState} from "react";
import Refresh from "@mui/icons-material/Refresh";
import FilterList from "@mui/icons-material/FilterList";
import {useTheme} from "@mui/material/styles";
import {getLastPageSize, setLastPageSize} from "../../utils/ConfigurationStorage";
import {
    getDeleteOptions,
    getPageSizeOptions,
    getPostOptions,
    getPutOptions,
    getSettingsTableHeight,
    getTableHeaderBackgroundColor,
    getTableRowBackgroundColor
} from "../../utils/MaterialTableHelper";
import {handleErrors} from "../../utils/FetchHelper";
import {
    convertNullValuesInObject,
    getArrayLengthStr,
    getClassById,
    isItemChanged,
    validateItem
} from "../../utils/JsonHelper";
import {MessageBox} from "../utils/MessageBox";
import {
    getColumnClass,
    getColumnCreatedAt,
    getColumnCreatedBy,
    getColumnDescription,
    getColumnModifiedAt,
    getColumnModifiedBy,
    getColumnName
} from "../utils/StandardColumns";
import {getColumnsMapper} from "../../utils/NullValueMappers";
import ColumnsContext from "../../context/columns/ColumnsContext";
import EditColumnsDialog from "../dialogs/EditColumnsDialog";
import CloneIcon from '@mui/icons-material/ViewStream'
import {getBaseUrl} from "../../utils/UrlBuilder";
import ClassesContext from "../../context/classes/ClassesContext";

export default function ColumnsTab() {

    const theme = useTheme();
    const tableRef = React.createRef();
    const [messageBox, setMessageBox] = useState({open: false, severity: 'error', title: '', message: ''});
    const [pageSize, setPageSize] = useState(getLastPageSize);
    const [filtering, setFiltering] = useState(false);
    const classesContext = useContext(ClassesContext);
    const {classes, fetchClasses, classesLookup} = classesContext;
    const columnsContext = useContext(ColumnsContext);
    const {columns, fetchColumns, addColumns, editColumns, removeColumns} = columnsContext;
    const changeableFields = ['name', 'description', 'configuration', 'classId'];
    const fieldsSpecification = {
        name: {title: 'Name', check: ['notEmpty', 'min1', 'max30']},
        description: {title: 'Description', check: ['max250']}
    };

    useEffect(() => {
        if (classes == null)
            fetchClasses();
    }, [classes, fetchClasses]);

    useEffect(() => {
        if (columns == null)
            fetchColumns();
    }, [columns, fetchColumns]);

    const onChangeRowsPerPage = (pageSize) => {
        setPageSize(pageSize);
        setLastPageSize(pageSize);
    }

    const cloneItem = (rowData) => {
        fetch(getBaseUrl('columns'), getPostOptions(rowData))
            .then(handleErrors)
            .catch(error => {
                setMessageBox({open: true, severity: 'error', title: 'Error', message: error});
            })
            .then((columns) => {
                if (columns != null) {
                    addColumns(convertNullValuesInObject(columns, getColumnsMapper()));
                }
            });
    }

    return (
        <div>
            <MaterialTable

                title='Columns'
                tableRef={tableRef}
                columns={[
                    getColumnName(),
                    getColumnDescription(),
                    getColumnClass(classesLookup, 'Class support'),
                    {
                        title: 'Columns',
                        field: 'configuration',
                        filtering: false,
                        searchable: false,
                        sorting: false,
                        initialEditValue: {fields: [], columns: []},
                        render: rowData => getArrayLengthStr(rowData['configuration']['columns']),
                        editComponent: props => (
                            <EditColumnsDialog
                                configuration={props.rowData.configuration != null ? props.rowData.configuration : {
                                    fields: [],
                                    columns: []
                                }}
                                dataClass={getClassById(classes, props.rowData.classId)}
                                name={props.rowData.name != null ? props.rowData.name : ''}
                                onChange={props.onChange}
                            />
                        )
                    },
                    getColumnCreatedBy(),
                    getColumnCreatedAt(),
                    getColumnModifiedBy(),
                    getColumnModifiedAt()
                ]}
                data={columns != null ? columns : []}
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
                    maxBodyHeight: getSettingsTableHeight(),
                    minBodyHeight: getSettingsTableHeight(),
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
                        onClick: () => fetchColumns()
                    },
                    {
                        icon: () => <FilterList/>,
                        tooltip: 'Enable/disable filter',
                        isFreeAction: true,
                        onClick: () => setFiltering(!filtering)
                    },
                    {
                        icon: () => <CloneIcon/>,
                        tooltip: 'Duplicate',
                        onClick: (event, rowData) => cloneItem(rowData)
                    }
                ]}
                editable={{
                    onRowAdd: newData =>
                        new Promise((resolve, reject) => {
                            let message = validateItem(newData, fieldsSpecification);
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

                            fetch(getBaseUrl('columns'), getPostOptions(newData))
                                .then(handleErrors)
                                .catch(error => {
                                    reject();
                                    setMessageBox({open: true, severity: 'error', title: 'Error', message: error});
                                })
                                .then((columns) => {
                                    if (columns != null) {
                                        addColumns(convertNullValuesInObject(columns, getColumnsMapper()));
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

                            let message = validateItem(newData, fieldsSpecification);
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

                            fetch(getBaseUrl('columns'), getPutOptions(newData))
                                .then(handleErrors)
                                .catch(error => {
                                    setMessageBox({open: true, severity: 'error', title: 'Error', message: error});
                                    reject();
                                })
                                .then((columns) => {
                                    if (columns != null) {
                                        editColumns(convertNullValuesInObject(columns, getColumnsMapper()));
                                        resolve();
                                    }
                                });
                        }),

                    onRowDelete: oldData =>
                        new Promise((resolve, reject) => {
                            setTimeout(() => {
                                let e = false;
                                fetch(getBaseUrl(`columns/${oldData.id}`), getDeleteOptions())
                                    .then(handleErrors)
                                    .catch(error => {
                                        e = true;
                                        if (error.includes('already used by items'))
                                            setMessageBox({
                                                open: true,
                                                severity: 'warning',
                                                title: 'Item can not be removed',
                                                message: error
                                            });
                                        else
                                            setMessageBox({
                                                open: true,
                                                severity: 'error',
                                                title: 'Error',
                                                message: error
                                            });
                                        reject();
                                    })
                                    .then(() => {
                                        if (!e) {
                                            removeColumns(oldData.id);
                                            resolve();
                                        }
                                    });

                            }, 100);
                        }),
                }}
            />
            <MessageBox
                config={messageBox}
                onClose={() => setMessageBox({...messageBox, open: false})}
            />
        </div>
    );
}
