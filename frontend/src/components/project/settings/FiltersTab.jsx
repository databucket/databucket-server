import MaterialTable from "material-table";
import React, {useContext, useEffect, useState} from "react";
import Refresh from "@material-ui/icons/Refresh";
import FilterList from "@material-ui/icons/FilterList";
import {useTheme} from "@material-ui/core/styles";
import {getLastPageSize, setLastPageSize} from "../../../utils/ConfigurationStorage";
import {
    getBaseUrl, getDeleteOptions,
    getPageSizeOptions, getPostOptions, getPutOptions,
    getTableHeaderBackgroundColor,
    getTableIcons, getTableRowBackgroundColor
} from "../../../utils/MaterialTableHelper";
import {handleErrors} from "../../../utils/FetchHelper";
import {
    convertNullValuesInObject, getArrayLengthStr,
    isItemChanged,
    validateItem
} from "../../../utils/JsonHelper";
import {MessageBox} from "../../utils/MessageBox";
import {
    getColumnCreatedBy,
    getColumnCreatedDate,
    getColumnDescription,
    getColumnLastModifiedBy, getColumnLastModifiedDate,
    getColumnName
} from "../../utils/StandardColumns";
import {getColumnsMapper, getFiltersMapper} from "../../../utils/NullValueMappers";
import FiltersContext from "../../../context/filters/FiltersContext";
import EditConditionsDialog from "../dialogs/EditConditionsDialog";

export default function FiltersTab() {

    const theme = useTheme();
    const tableRef = React.createRef();
    const [messageBox, setMessageBox] = useState({open: false, severity: 'error', title: '', message: ''});
    const [pageSize, setPageSize] = useState(getLastPageSize);
    const [filtering, setFiltering] = useState(false);
    const filtersContext = useContext(FiltersContext);
    const {filters, fetchFilters, addFilter, editFilter, removeFilter} = filtersContext;
    const changeableFields = ['name', 'description', 'configuration'];
    const fieldsSpecification = {
        name: {title: 'Name', check: ['notEmpty', 'min1', 'max50']},
        description: {title: 'Description', check: ['max250']}
    };

    useEffect(() => {
        if (filters == null)
            fetchFilters();
    }, [filters, fetchFilters]);

    const onChangeRowsPerPage = (pageSize) => {
        setPageSize(pageSize);
        setLastPageSize(pageSize);
    }

    return (
        <div>
            <MaterialTable
                icons={getTableIcons()}
                title='Filters'
                tableRef={tableRef}
                columns={[
                    getColumnName(),
                    getColumnDescription(),
                    {
                        title: 'Configuration',
                        field: 'configuration',
                        filtering: false,
                        searchable: false,
                        sorting: false,
                        render: rowData => getArrayLengthStr(rowData['configuration']),
                        editComponent: props => (
                            <EditConditionsDialog
                                configuration={props.rowData.configuration != null ? props.rowData.configuration : []}
                                name={props.rowData.name != null ? props.rowData.name : ''}
                                description={props.rowData.description != null && props.rowData.description.length > 0 ? "(" + props.rowData.description + ")" : ''}
                                onChange={props.onChange}
                            />
                        )
                    },
                    getColumnCreatedDate(),
                    getColumnCreatedBy(),
                    getColumnLastModifiedDate(),
                    getColumnLastModifiedBy()
                ]}
                data={filters != null ? filters : []}
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
                        onClick: () => fetchFilters()
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

                            fetch(getBaseUrl('filters'), getPostOptions(newData))
                                .then(handleErrors)
                                .catch(error => {
                                    reject();
                                    setMessageBox({open: true, severity: 'error', title: 'Error', message: error});
                                })
                                .then((filter) => {
                                    if (filter != null) {
                                        addFilter(convertNullValuesInObject(filter, getFiltersMapper()));
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
                                    severity: 'Item is not valid',
                                    title: '',
                                    message: message
                                });
                                reject();
                                return;
                            }

                            fetch(getBaseUrl('filters'), getPutOptions(newData))
                                .then(handleErrors)
                                .catch(error => {
                                    setMessageBox({open: true, severity: 'error', title: 'Error', message: error});
                                    reject();
                                })
                                .then((filter) => {
                                    if (filter != null) {
                                        editFilter(convertNullValuesInObject(filter, getColumnsMapper()));
                                        resolve();
                                    }
                                });
                        }),

                    onRowDelete: oldData =>
                        new Promise((resolve, reject) => {
                            setTimeout(() => {
                                fetch(getBaseUrl(`filters/${oldData.id}`), getDeleteOptions())
                                    .then(handleErrors)
                                    .catch(error => {
                                        setMessageBox({open: true, severity: 'error', title: 'Error', message: error});
                                        reject();
                                    })
                                    .then(() => {
                                        removeFilter(oldData.id);
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