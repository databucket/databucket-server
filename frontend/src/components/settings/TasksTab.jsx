import MaterialTable from "material-table";
import React, {useContext, useEffect, useState} from "react";
import {
    FilterList,
    Refresh,
    ViewStream as CloneIcon
} from "@mui/icons-material";
import {useTheme} from "@mui/material";
import {
    getLastPageSize,
    setLastPageSize
} from "../../utils/ConfigurationStorage";
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
    getClassById,
    isItemChanged,
    validateItem
} from "../../utils/JsonHelper";
import {MessageBox} from "../utils/MessageBox";
import {
    getColumnBuckets,
    getColumnClass,
    getColumnClasses,
    getColumnDescription,
    getColumnFilter,
    getColumnModifiedAt,
    getColumnModifiedBy,
    getColumnName
} from "../utils/StandardColumns";
import {getTasksMapper} from "../../utils/NullValueMappers";
import TasksContext from "../../context/tasks/TasksContext";
import {getBaseUrl} from "../../utils/UrlBuilder";
import BucketsContext from "../../context/buckets/BucketsContext";
import ClassesContext from "../../context/classes/ClassesContext";
import TaskEditConfigDialog, {
    getActionsType
} from "../dialogs/TaskEditConfigDialog";
import FiltersContext from "../../context/filters/FiltersContext";
import TagsContext from "../../context/tags/TagsContext";

export default function TasksTab() {

    const theme = useTheme();
    const tableRef = React.createRef();
    const [messageBox, setMessageBox] = useState({open: false, severity: 'error', title: '', message: ''});
    const [pageSize, setPageSize] = useState(getLastPageSize);
    const [filtering, setFiltering] = useState(false);
    const classesContext = useContext(ClassesContext);
    const {classes, fetchClasses, classesLookup} = classesContext;
    const bucketsContext = useContext(BucketsContext);
    const {buckets, fetchBuckets} = bucketsContext;
    const filtersContext = useContext(FiltersContext);
    const {filters, fetchFilters} = filtersContext;
    const tagsContext = useContext(TagsContext);
    const {tags, fetchTags} = tagsContext;
    const tasksContext = useContext(TasksContext);
    const {tasks, fetchTasks, addTask, editTask, removeTask} = tasksContext;
    const changeableFields = ['name', 'description', 'configuration', 'filterId', 'classId', 'classesIds', 'bucketsIds'];
    const fieldsSpecification = {
        name: {title: 'Name', check: ['notEmpty', 'min1', 'max30']},
        description: {title: 'Description', check: ['max250']}
    };

    useEffect(() => {
        if (tags == null)
            fetchTags();
    }, [tags, fetchTags]);

    useEffect(() => {
        if (classes == null)
            fetchClasses();
    }, [classes, fetchClasses]);

    useEffect(() => {
        if (buckets == null)
            fetchBuckets();
    }, [buckets, fetchBuckets]);

    useEffect(() => {
        if (tasks == null)
            fetchTasks();
    }, [tasks, fetchTasks]);

    useEffect(() => {
        if (filters == null)
            fetchFilters();
    }, [filters, fetchFilters]);

    const onChangeRowsPerPage = (pageSize) => {
        setPageSize(pageSize);
        setLastPageSize(pageSize);
    }

    const cloneItem = (rowData) => {
        fetch(getBaseUrl('tasks'), getPostOptions(rowData))
            .then(handleErrors)
            .catch(error => {
                setMessageBox({open: true, severity: 'error', title: 'Error', message: error});
            })
            .then((tasks) => {
                if (tasks != null) {
                    addTask(convertNullValuesInObject(tasks, getTasksMapper()));
                }
            });
    }

    return (
        <div>
            <MaterialTable
                title='Tasks'
                tableRef={tableRef}
                columns={[
                    getColumnName("20%"),
                    getColumnDescription("20%"),
                    getColumnClass(classesLookup, 'Class support'),
                    getColumnFilter(filters),
                    getColumnBuckets(buckets, 'Show in buckets'),
                    getColumnClasses(classes, 'Show by classes'),
                    {
                        title: 'Action',
                        field: 'configuration',
                        filtering: false,
                        searchable: false,
                        sorting: false,
                        initialEditValue: {
                            properties: [],
                            actions: {
                                type: 'remove',
                                setTag: false,
                                tagId: 0,
                                setReserved: false,
                                reserved: false,
                                properties: []
                            }
                        },
                        render: rowData => getActionsType(rowData.configuration.actions),
                        editComponent: props => (
                            <TaskEditConfigDialog
                                rowData={props.rowData}
                                configuration={props.rowData.configuration}
                                name={props.rowData.name != null ? props.rowData.name : ''}
                                dataClass={getClassById(classes, props.rowData.classId)}
                                onChange={props.onChange}
                            />
                        )
                    },
                    getColumnModifiedBy(),
                    getColumnModifiedAt()
                ]}
                data={tasks != null ? tasks : []}
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
                        onClick: () => fetchTasks()
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

                            fetch(getBaseUrl('tasks'), getPostOptions(newData))
                                .then(handleErrors)
                                .catch(error => {
                                    reject();
                                    setMessageBox({open: true, severity: 'error', title: 'Error', message: error});
                                })
                                .then((task) => {
                                    if (task != null) {
                                        addTask(convertNullValuesInObject(task, getTasksMapper()));
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

                            fetch(getBaseUrl('tasks'), getPutOptions(newData))
                                .then(handleErrors)
                                .catch(error => {
                                    setMessageBox({open: true, severity: 'error', title: 'Error', message: error});
                                    reject();
                                })
                                .then((task) => {
                                    if (task != null) {
                                        editTask(convertNullValuesInObject(task, getTasksMapper()));
                                        resolve();
                                    }
                                });
                        }),

                    onRowDelete: oldData =>
                        new Promise((resolve, reject) => {
                            setTimeout(() => {
                                let e = false;
                                fetch(getBaseUrl(`tasks/${oldData.id}`), getDeleteOptions())
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
                                            removeTask(oldData.id);
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
