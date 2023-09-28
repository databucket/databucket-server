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
import {convertNullValuesInObject, getArrayLengthStr, isItemChanged, validateItem} from "../../utils/JsonHelper";
import {MessageBox} from "../utils/MessageBox";
import {
    getColumnCreatedAt,
    getColumnCreatedBy,
    getColumnDescription,
    getColumnModifiedAt,
    getColumnModifiedBy,
    getColumnName
} from "../utils/StandardColumns";
import ClassesContext from "../../context/classes/ClassesContext";
import {getClassMapper} from "../../utils/NullValueMappers";
import EditClassFieldsDialog from "../dialogs/EditClassFieldsDialog";
import {getBaseUrl} from "../../utils/UrlBuilder";
import EnumsContext from "../../context/enums/EnumsContext";

export default function ClassesTab() {

    const theme = useTheme();
    const tableRef = React.createRef();
    const [messageBox, setMessageBox] = useState({open: false, severity: 'error', title: '', message: ''});
    const [pageSize, setPageSize] = useState(getLastPageSize);
    const [filtering, setFiltering] = useState(false);
    const classesContext = useContext(ClassesContext);
    const {classes, fetchClasses, addClass, editClass, removeClass} = classesContext;
    const enumsContext = useContext(EnumsContext);
    const {enums, fetchEnums} = enumsContext;
    const changeableFields = ['name', 'description', 'configuration'];
    const fieldsSpecification = {
        name: {title: 'Name', check: ['notEmpty', 'min1', 'max30']},
        description: {title: 'Description', check: ['max250']}
    };

    useEffect(() => {
        if (enums == null)
            fetchEnums();
    }, []);

    useEffect(() => {
        if (classes == null)
            fetchClasses();
    }, [classes, fetchClasses]);

    const onChangeRowsPerPage = (pageSize) => {
        setPageSize(pageSize);
        setLastPageSize(pageSize);
    }

    return (
        <div>
            <MaterialTable

                title='Classes'
                tableRef={tableRef}
                columns={[
                    getColumnName("20%"),
                    getColumnDescription("20%"),
                    {
                        title: 'Properties',
                        field: 'configuration',
                        filtering: false,
                        searchable: false,
                        sorting: false,
                        initialEditValue: [],
                        render: rowData => getArrayLengthStr(rowData['configuration']),
                        editComponent: props => (
                            <EditClassFieldsDialog
                                configuration={props.rowData.configuration != null ? props.rowData.configuration : []}
                                name={props.rowData.name != null ? props.rowData.name : ''}
                                description={props.rowData.description != null && props.rowData.description.length > 0 ? "(" + props.rowData.description + ")" : ''}
                                onChange={props.onChange}
                            />
                        )
                    },
                    getColumnCreatedBy(),
                    getColumnCreatedAt(),
                    getColumnModifiedBy(),
                    getColumnModifiedAt()
                ]}
                data={classes != null ? classes : []}
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
                        onClick: () => fetchClasses()
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

                            fetch(getBaseUrl('classes'), getPostOptions(newData))
                                .then(handleErrors)
                                .catch(error => {
                                    reject();
                                    setMessageBox({open: true, severity: 'error', title: 'Error', message: error});
                                })
                                .then((dataClass) => {
                                    if (dataClass != null) {
                                        addClass(convertNullValuesInObject(dataClass, getClassMapper()));
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

                            fetch(getBaseUrl('classes'), getPutOptions(newData))
                                .then(handleErrors)
                                .catch(error => {
                                    setMessageBox({open: true, severity: 'error', title: 'Error', message: error});
                                    reject();
                                })
                                .then((dataClass) => {
                                    if (dataClass != null) {
                                        editClass(convertNullValuesInObject(dataClass, getClassMapper()));
                                        resolve();
                                    }
                                });
                        }),

                    onRowDelete: oldData =>
                        new Promise((resolve, reject) => {
                            setTimeout(() => {
                                let e = false;
                                fetch(getBaseUrl(`classes/${oldData.id}`), getDeleteOptions())
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
                                            removeClass(oldData.id);
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
    )
}
