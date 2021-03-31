import MaterialTable from "material-table";
import React, {useContext, useEffect, useState} from "react";
import Refresh from "@material-ui/icons/Refresh";
import FilterList from "@material-ui/icons/FilterList";
import {useTheme} from "@material-ui/core/styles";
import {getLastPageSize, setLastPageSize} from "../../utils/ConfigurationStorage";
import {
    getDeleteOptions,
    getPageSizeOptions, getPostOptions, getPutOptions, getSettingsTableHeight,
    getTableHeaderBackgroundColor,
    getTableIcons, getTableRowBackgroundColor
} from "../../utils/MaterialTableHelper";
import {handleErrors} from "../../utils/FetchHelper";
import {
    convertNullValuesInObject, getArrayLengthStr,
    isItemChanged,
    validateItem
} from "../../utils/JsonHelper";
import {MessageBox} from "../utils/MessageBox";
import {
    getColumnDescription,
    getColumnModifiedBy, getColumnModifiedAt,
    getColumnName
} from "../utils/StandardColumns";
import {getEnumMapper} from "../../utils/NullValueMappers";
import EditEnumDialog from "../dialogs/EditEnumDialog";
import EnumsContext from "../../context/enums/EnumsContext";
import {useWindowDimension} from "../utils/UseWindowDimension";
import {getBaseUrl} from "../../utils/UrlBuilder";

export default function EnumsTab() {

    const theme = useTheme();
    const [height] = useWindowDimension();
    const tableRef = React.createRef();
    const [messageBox, setMessageBox] = useState({open: false, severity: 'error', title: '', message: ''});
    const [pageSize, setPageSize] = useState(getLastPageSize);
    const [filtering, setFiltering] = useState(false);
    const enumsContext = useContext(EnumsContext);
    const {enums, fetchEnums, addEnum, editEnum, removeEnum} = enumsContext;
    const changeableFields = ['name', 'description', 'iconsEnabled', 'items'];
    const fieldsSpecification = {
        name: {title: 'Name', check: ['notEmpty', 'min1', 'max30']},
        description: {title: 'Description', check: ['max250']},
        items: {title: 'Items', check: ['notEmpty', 'custom-check-enum-items']}
    };

    useEffect(() => {
        if (enums == null)
            fetchEnums();
    }, [enums, fetchEnums]);

    const onChangeRowsPerPage = (pageSize) => {
        setPageSize(pageSize);
        setLastPageSize(pageSize);
    }

    return (
        <div>
            <MaterialTable
                icons={getTableIcons()}
                title='Enums'
                tableRef={tableRef}
                columns={[
                    getColumnName(),
                    getColumnDescription(),
                    {
                        title: 'Icons enabled',
                        field: 'iconsEnabled',
                        type: 'boolean',
                        initialEditValue: false
                    },
                    {
                        title: 'Items', field: 'items',
                        render: rowData => getArrayLengthStr(rowData['items']),
                        editComponent: props => (
                            <EditEnumDialog
                                name={props.rowData['name'] != null ? props.rowData['name'] : ''}
                                iconsEnabled={props.rowData['iconsEnabled'] === true || props.rowData['iconsEnabled'] === 'true'}
                                items={props.rowData['items'] != null ? props.rowData['items'] : []}
                                onChange={props.onChange}
                            />
                        )
                    },
                    // getColumnCreatedBy(),
                    // getColumnCreatedAt(),
                    getColumnModifiedBy(),
                    getColumnModifiedAt()
                ]}
                data={enums != null ? enums : []}
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
                        onClick: () => fetchEnums()
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

                            fetch(getBaseUrl('enums'), getPostOptions(newData))
                                .then(handleErrors)
                                .catch(error => {
                                    reject();
                                    setMessageBox({open: true, severity: 'error', title: 'Error', message: error});
                                })
                                .then((dataClass) => {
                                    if (dataClass != null) {
                                        addEnum(convertNullValuesInObject(dataClass, getEnumMapper()));
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

                            fetch(getBaseUrl('enums'), getPutOptions(newData))
                                .then(handleErrors)
                                .catch(error => {
                                    setMessageBox({open: true, severity: 'error', title: 'Error', message: error});
                                    reject();
                                })
                                .then((dataClass) => {
                                    if (dataClass != null) {
                                        editEnum(convertNullValuesInObject(dataClass, getEnumMapper()));
                                        resolve();
                                    }
                                });
                        }),

                    onRowDelete: oldData =>
                        new Promise((resolve, reject) => {
                            setTimeout(() => {
                                fetch(getBaseUrl(`enums/${oldData.id}`), getDeleteOptions())
                                    .then(handleErrors)
                                    .catch(error => {
                                        setMessageBox({open: true, severity: 'error', title: 'Error', message: error});
                                        reject();
                                    })
                                    .then(() => {
                                        removeEnum(oldData.id);
                                        resolve();
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