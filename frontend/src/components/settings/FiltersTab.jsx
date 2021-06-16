import MaterialTable from "material-table";
import React, {useContext, useEffect, useState} from "react";
import Refresh from "@material-ui/icons/Refresh";
import FilterList from "@material-ui/icons/FilterList";
import {useTheme} from "@material-ui/core/styles";
import {getLastPageSize, setLastPageSize} from "../../utils/ConfigurationStorage";
import {
    getDeleteOptions,
    getPageSizeOptions, getPostOptions, getPutOptions, getSettingsTableHeight,
    getTableHeaderBackgroundColor, getTableRowBackgroundColor
} from "../../utils/MaterialTableHelper";
import {handleErrors} from "../../utils/FetchHelper";
import {
    convertNullValuesInObject, getClassById, getObjectLengthStr,
    isItemChanged,
    validateItem
} from "../../utils/JsonHelper";
import {MessageBox} from "../utils/MessageBox";
import {
    getColumnDescription,
    getColumnModifiedBy, getColumnModifiedAt,
    getColumnName, getColumnClass, getColumnCreatedBy, getColumnCreatedAt
} from "../utils/StandardColumns";
import {getFiltersMapper} from "../../utils/NullValueMappers";
import FiltersContext from "../../context/filters/FiltersContext";
import EditFilterRulesDialog from "../dialogs/EditFilterRulesDialog";
import CloneIcon from '@material-ui/icons/ViewStream'
import {useWindowDimension} from "../utils/UseWindowDimension";
import ClassesContext from "../../context/classes/ClassesContext";
import TagsContext from "../../context/tags/TagsContext";
import UsersContext from "../../context/users/UsersContext";
import {getBaseUrl} from "../../utils/UrlBuilder";

export default function FiltersTab() {

    const theme = useTheme();
    const [height] = useWindowDimension();
    const tableRef = React.createRef();
    const [messageBox, setMessageBox] = useState({open: false, severity: 'error', title: '', message: ''});
    const [pageSize, setPageSize] = useState(getLastPageSize);
    const [filtering, setFiltering] = useState(false);
    const classesContext = useContext(ClassesContext);
    const {classes, fetchClasses, classesLookup} = classesContext;
    const tagsContext = useContext(TagsContext);
    const {tags, fetchTags} = tagsContext;
    const usersContext = useContext(UsersContext);
    const {users, fetchUsers} = usersContext;
    const filtersContext = useContext(FiltersContext);
    const {filters, fetchFilters, addFilter, editFilter, removeFilter} = filtersContext;
    const changeableFields = ['name', 'description', 'classId', 'configuration'];
    const fieldsSpecification = {
        name: {title: 'Name', check: ['notEmpty', 'min1', 'max30']},
        description: {title: 'Description', check: ['max250']}
    };

    useEffect(() => {
        if (filters == null)
            fetchFilters();
    }, [filters, fetchFilters]);

    useEffect(() => {
        if (classes == null)
            fetchClasses();
    }, [classes, fetchClasses]);

    useEffect(() => {
        if (tags == null)
            fetchTags();
    }, [tags, fetchTags]);

    useEffect(() => {
        if (users == null)
            fetchUsers();
    }, [users, fetchUsers]);

    const onChangeRowsPerPage = (pageSize) => {
        setPageSize(pageSize);
        setLastPageSize(pageSize);
    }

    const cloneItem = (rowData) => {
        fetch(getBaseUrl('filters'), getPostOptions(rowData))
            .then(handleErrors)
            .catch(error => {
                setMessageBox({open: true, severity: 'error', title: 'Error', message: error});
            })
            .then((filters) => {
                if (filters != null) {
                    addFilter(convertNullValuesInObject(filters, getFiltersMapper()));
                }
            });
    }

    return (
        <div>
            <MaterialTable
                title='Filters'
                tableRef={tableRef}
                columns={[
                    getColumnName(),
                    getColumnDescription(),
                    getColumnClass(classesLookup, 'Class support'),
                    {
                        title: 'Rules',
                        field: 'configuration',
                        filtering: false,
                        searchable: false,
                        sorting: false,
                        initialEditValue: {properties: [], logic: null},
                        render: rowData => getObjectLengthStr(rowData['configuration']),
                        editComponent: props => (
                            <EditFilterRulesDialog
                                configuration={props.rowData.configuration}
                                name={props.rowData.name != null ? props.rowData.name : ''}
                                dataClass={getClassById(classes, props.rowData.classId)}
                                tags={tags}
                                users={users}
                                onChange={props.onChange}
                            />
                        )
                    },
                    getColumnCreatedBy(),
                    getColumnCreatedAt(),
                    getColumnModifiedBy(),
                    getColumnModifiedAt()
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
                        onClick: () => fetchFilters()
                    },
                    {
                        icon: () => <FilterList/>,
                        tooltip: 'Enable/disable filter',
                        isFreeAction: true,
                        onClick: () => setFiltering(!filtering)
                    },
                    {
                        icon: () => <CloneIcon/>,
                        tooltip: 'Clone',
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
                                    severity: 'error',
                                    title: 'Item is not valid',
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
                                        editFilter(convertNullValuesInObject(filter, getFiltersMapper()));
                                        resolve();
                                    }
                                });
                        }),

                    onRowDelete: oldData =>
                        new Promise((resolve, reject) => {
                            setTimeout(() => {
                                let e = false;
                                fetch(getBaseUrl(`filters/${oldData.id}`), getDeleteOptions())
                                    .then(handleErrors)
                                    .catch(error => {
                                        e = true;
                                        if (error.includes('already used by items'))
                                            setMessageBox({open: true, severity: 'warning', title: 'Item can not be removed', message: error});
                                        else
                                            setMessageBox({open: true, severity: 'error', title: 'Error', message: error});
                                        reject();
                                    })
                                    .then(() => {
                                        if (!e) {
                                            removeFilter(oldData.id);
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