import React, {createRef, useContext, useEffect, useState} from 'react';
import MaterialTable, {MTableToolbar} from 'material-table';
import {
    getDeleteOptions,
    getGetOptions,
    getPageSizeOptionsOnDialog,
    getPostOptions,
    getPutOptions,
    getTableHeaderBackgroundColor,
    getTableHeight,
    getTableRowBackgroundColor
} from "../../utils/MaterialTableHelper";
import {useTheme} from "@material-ui/core/styles";
import {getLastActiveView, getLastPageSize, getUsername, setLastActiveView, setLastPageSize} from "../../utils/ConfigurationStorage";
import {useWindowDimension} from "../utils/UseWindowDimension";
import {Grid} from "@material-ui/core";
import ViewMenuSelector from "./ViewMenuSelector";
import AccessContext from "../../context/access/AccessContext";
import MissingBucketTable from "./MissingBucketTable";
import {
    isFeatureEnabled,
    FEATURE_SEARCH,
    FEATURE_MODIFYING,
    FEATURE_DETAILS,
    FEATURE_HISTORY,
    FEATURE_CREATION,
    FEATURE_REMOVAL,
    FEATURE_RESERVATION,
    FEATURE_EXPORT, FEATURE_TASKS
} from "../utils/ViewFeatures";
import prepareTableColumns, {
    convertDataBeforeAdd,
    convertDataBeforeModify,
    getActiveView,
    getBucketTags, getBucketTasks,
    getBucketViews,
    getColumnSource,
    getFetchColumns
} from "./BucketDataTableHelper";
import MissingActiveView from "./MissingActiveView";
import Refresh from "@material-ui/icons/Refresh";
import RateReviewOutlined from "@material-ui/icons/RateReviewOutlined";
import History from "@material-ui/icons/History";
import FilterList from "@material-ui/icons/FilterList";
import {getDataByIdUrl, getDataHistoryUrl, getDataReserveUrl, getDataUrl} from "../../utils/UrlBuilder";
import {handleErrors} from "../../utils/FetchHelper";
import {MessageBox} from "../utils/MessageBox";
import DataDetailsDialog from "../dialogs/DataDetailsDialog";
import DataHistoryDialog from "../dialogs/DataHistoryDialog";
import ReserveDataDialog from "../dialogs/ReserveDataDialog";
import TaskExecutionDialog from "../dialogs/TaskExecutionDialog";

// declared as a global because of component bug: https://github.com/mbrn/material-table/issues/2432
const tableRef = createRef();

export default function BucketDataTable() {

    const theme = useTheme();
    const [pageSize, setPageSize] = useState(getLastPageSize);
    const [messageBox, setMessageBox] = useState({open: false, severity: 'error', title: '', message: ''});
    const [height] = useWindowDimension();
    const [filtering, setFiltering] = useState(false);
    const accessContext = useContext(AccessContext);
    const {buckets, activeBucket, views, columns, filters, tags, tasks, enums} = accessContext;
    const [detailsState, setDetailsState] = useState({
        open: false,
        dataRow: null,
    });
    const [historyState, setHistoryState] = useState({
        open: false,
        dataRowId: 0,
        history: []
    });
    const [taskState, setTaskState] = useState({
        open: false
    });
    const [state, setState] = useState({
        bucketViews: [],    // all views available for active bucket that user has access
        bucketTags: [],
        bucketTasks: [],
        activeView: null,
        columnsDef: [],     // pure columns definition
        filterDef: null,    // pure filter definition
        tableColumns: [],   // columns prepared for material table
    });

    // active bucket is changed
    useEffect(() => {
        const bucketViews = getBucketViews(activeBucket, views);
        if (bucketViews.length > 0 && tags != null && enums != null && views != null && columns != null) {
            const bucketTags = getBucketTags(activeBucket, tags);
            const bucketTasks = getBucketTasks(activeBucket, tasks);
            const lastActiveViewId = activeBucket != null ? getLastActiveView(activeBucket.id) : null;
            const activeView = getActiveView(bucketViews, lastActiveViewId);
            const columnsDef = columns.filter(c => c.id === activeView.columnsId)[0];
            const tableColumns = prepareTableColumns(columnsDef, bucketTags, enums);
            const filterDef = filters.filter(f => f.id === activeView.filterId)[0];
            setState({
                ...state,
                bucketViews: bucketViews,
                bucketTags: bucketTags,
                bucketTasks: bucketTasks,
                activeView: activeView,
                columnsDef: columnsDef,
                filterDef: filterDef,
                tableColumns: tableColumns
            });
            reloadData();
        } else
            setState({
                ...state,
                bucketViews: [],
                bucketTags: [],
                bucketTasks: [],
                activeView: null,
                columnsDef: [],
                filterDef: null,
                tableColumns: []
            });

    }, [activeBucket, enums, tags, views, columns, filters]);

    // active view is changed
    const onViewSelected = (view) => {
        setLastActiveView(activeBucket.id, view.id);
        const columnsDef = columns.filter(col => col.id === view.columnsId)[0];
        const filterDef = filters.filter(f => f.id === view.filterId)[0];
        const tableColumns = prepareTableColumns(columnsDef, state.bucketTags, enums);
        setState({
            ...state,
            activeView: view,
            columnsDef: columnsDef,
            filterDef: filterDef,
            tableColumns: tableColumns
        });
        reloadData();
    }

    const onOpenTaskExecutionEditorDialog = () => {
        setTaskState({...taskState, open: true});
    }

    const onCloseTaskExecutionEditorDialog = () => {
        setTaskState({...taskState, open: false});
    }

    const getRowDataId = (rowData) => {
        return rowData['Id'];
    }

    const onOpenDataDetailsDialog = (rowData) => {
        let resultOk = true;
        fetch(getDataByIdUrl(activeBucket, getRowDataId(rowData)), getGetOptions())
            .then(handleErrors)
            .catch(error => {
                setMessageBox({open: true, severity: 'error', title: 'Error', message: error});
                resultOk = false;
            })
            .then(dataRow => {
                if (resultOk) {
                    setDetailsState({
                        ...detailsState,
                        dataRow: dataRow,
                        open: true
                    });
                }
            });
    }

    const onCloseDataDetailsDialog = (dataRow, changed) => {
        if (changed) {
            let resultOk = true;
            fetch(getDataByIdUrl(activeBucket, dataRow.id), getPutOptions({properties: dataRow.properties}))
                .then(handleErrors)
                .catch(error => {
                    setMessageBox({open: true, severity: 'error', title: 'Error', message: error});
                    resultOk = false;
                })
                .then(() => {
                    resultOk && reloadData();
                });
        }
        setDetailsState({...detailsState, open: false});
    }

    const onOpenDataHistoryDialog = (rowData) => {
        let resultOk = true;
        fetch(getDataHistoryUrl(activeBucket, getRowDataId(rowData)), getGetOptions())
            .then(handleErrors)
            .catch(error => {
                setMessageBox({open: true, severity: 'error', title: 'Error', message: error});
                resultOk = false;
            })
            .then(resultHistory => {
                if (resultOk) {
                    setHistoryState({
                        ...historyState,
                        dataRowId: getRowDataId(rowData),
                        history: resultHistory,
                        open: true
                    });
                }
            });
    }

    const onCloseDataHistoryDialog = () => {
        setHistoryState({...historyState, open: false});
    }

    const getActiveViewFilterLogic = () => {
        if (state.activeView.filterId != null) {
            const filterDef = filters.filter(f => f.id === state.activeView.filterId);
            if (filterDef.length > 0)
                return filterDef[0].configuration.logic;
            else {
                console.error(`The linked filter (id=${state.activeView.filterId}) doesn't exist in the filter list!`)
                return null;
            }
        } else
            return null;
    }

    const onDataReserve = ({random, number, username}) => {
        const query = tableRef.current.state.query;
        let payload = {
            targetOwnerUsername: username !== getUsername() ? username : null,
            conditions: consolidateAllConditions(query.search, query.filters),
            logic: getActiveViewFilterLogic()
        };

        let resultOk = true;
        fetch(getDataReserveUrl(activeBucket, number, random), getPostOptions(payload))
            .then(handleErrors)
            .catch(error => {
                setMessageBox({open: true, severity: 'error', title: 'Error', message: error});
                resultOk = false;
            })
            .then((response) => {
                if (resultOk)
                    if (response.hasOwnProperty("message"))
                        setMessageBox({open: true, severity: 'info', title: 'Info', message: response.message});
                    else
                        reloadData();
            });
    }

    const consolidateAllConditions = (tableSearch, tableFilters) => {
        let allConditions = [];
        if (tableSearch != null && tableSearch.length > 0)
            allConditions.push({left_source: 'field', left_value: 'properties', operator: 'like', right_source: 'const', right_value: '%' + tableSearch + '%'});

        if (filtering && tableFilters.length > 0) {
            for (const filter of tableFilters) {
                if (filter.column.source.startsWith('$')) {
                    const leftSource = filter.column.source.endsWith('()') ? 'function' : 'property';

                    if (filter.column.type === 'numeric')
                        if (Array.isArray(filter.value)) {
                            if (filter.value.length > 0)
                                allConditions.push({left_source: leftSource, left_value: filter.column.source, operator: 'in', right_source: 'const', right_value: filter.value});
                        } else
                            allConditions.push({left_source: leftSource, left_value: filter.column.source, operator: '=', right_source: 'const', right_value: parseFloat(filter.value)});
                    else if (filter.column.type === 'boolean')
                        allConditions.push({left_source: leftSource, left_value: filter.column.source, operator: '=', right_source: 'const', right_value: (filter.value === 'checked')});
                    else if (Array.isArray(filter.value))
                        allConditions.push({left_source: leftSource, left_value: filter.column.source, operator: 'in', right_source: 'const', right_value: filter.value});
                    else
                        allConditions.push({left_source: leftSource, left_value: filter.column.source, operator: 'like', right_source: 'const', right_value: '%' + filter.value + '%'});

                } else {
                    if (filter.column.type === 'numeric')
                        if (Array.isArray(filter.value)) {
                            if (filter.value.length > 0) {
                                const numericList = filter.value.map(value => parseFloat(value));
                                allConditions.push({left_source: 'field', left_value: filter.column.source, operator: 'in', right_source: 'const', right_value: numericList});
                            }
                        } else
                            allConditions.push({left_source: 'field', left_value: filter.column.source, operator: '=', right_source: 'const', right_value: parseFloat(filter.value)});
                    else if (filter.column.type === 'boolean')
                        allConditions.push({left_source: 'field', left_value: filter.column.source, operator: '=', right_source: 'const', right_value: (filter.value === 'checked')});
                    else {
                        const filterValue = (filter.value === '@currentUser') ? getUsername() : '%' + filter.value + '%';
                        allConditions.push({left_source: 'field', left_value: filter.column.source, operator: 'like', right_source: 'const', right_value: filterValue});
                    }
                }
            }
        }

        return allConditions;
    }

    const reloadData = () => {
        tableRef.current !== null && tableRef.current.onQueryChange();
    }

    const tasksAction = {
        icon: () => <span className="material-icons">edit_note</span>,
        tooltip: 'Task execution',
        isFreeAction: true,
        onClick: () => {
            onOpenTaskExecutionEditorDialog()
        }
    };

    const filterAction = {
        icon: () => filtering ? <FilterList color={'primary'}/> : <FilterList/>,
        tooltip: 'Enable/disable filter',
        isFreeAction: true,
        onClick: () => {
            setFiltering(!filtering);
            tableRef.current.state.query.filters.length > 0 && reloadData();
        }
    };

    const refreshAction = {
        icon: () => <Refresh/>,
        tooltip: 'Refresh',
        isFreeAction: true,
        onClick: () => {
            reloadData();
        }
    };

    const detailsAction = {
        icon: () => <RateReviewOutlined/>,
        tooltip: 'Data details',
        onClick: (event, rowData) => {
            onOpenDataDetailsDialog(rowData);
        }
    };

    const historyAction = {
        icon: () => <History/>,
        tooltip: 'Data history',
        onClick: (event, rowData) => {
            onOpenDataHistoryDialog(rowData);
        }
    };

    const getActions = () => {
        let actions = [];
        actions.push(refreshAction);
        if (isFeatureEnabled(FEATURE_TASKS, state.activeView)) actions.push(tasksAction);
        actions.push(filterAction);
        if (isFeatureEnabled(FEATURE_DETAILS, state.activeView)) actions.push(detailsAction);
        if (isFeatureEnabled(FEATURE_HISTORY, state.activeView)) actions.push(historyAction);
        return actions;
    }

    const onRowAddAction = (newData) => new Promise((resolve, reject) => {
        let resultOk = true;
        fetch(getDataUrl(activeBucket), getPostOptions(convertDataBeforeAdd(state.tableColumns, newData)))
            .then(handleErrors)
            .catch(error => {
                setMessageBox({open: true, severity: 'error', title: 'Error', message: error});
                resultOk = false;
            })
            .then(() => {
                resultOk ? resolve() : reject();
            });
    });

    const onRowUpdateAction = (newData, oldData) => new Promise((resolve, reject) => {
        let payload = convertDataBeforeModify(state.tableColumns, newData, oldData);
        let resultOk = true;
        if (Object.keys(payload).length > 0) {
            fetch(getDataByIdUrl(activeBucket, getRowDataId(newData)), getPutOptions(payload))
                .then(handleErrors)
                .catch(error => {
                    setMessageBox({open: true, severity: 'error', title: 'Error', message: error});
                    resultOk = false;
                })
                .then(() => {
                    resultOk ? resolve() : reject();
                });
        } else {
            setMessageBox({
                open: true,
                severity: 'info',
                title: 'Nothing changed',
                message: ''
            });
            reject();
        }
    });

    const onRowDeleteAction = (oldData) => new Promise((resolve, reject) => {
        let resultOk = true;
        fetch(getDataByIdUrl(activeBucket, getRowDataId(oldData)), getDeleteOptions())
            .then(handleErrors)
            .catch(error => {
                setMessageBox({open: true, severity: 'error', title: 'Error', message: error});
                resultOk = false;
            })
            .then(() => {
                resultOk ? resolve() : reject();
            });
    });

    const getEditable = () => {
        let editable = {};

        if (isFeatureEnabled(FEATURE_CREATION, state.activeView))
            editable = {...editable, onRowAdd: (newData) => onRowAddAction(newData)};

        if (isFeatureEnabled(FEATURE_MODIFYING, state.activeView))
            editable = {...editable, onRowUpdate: (newData, oldData) => onRowUpdateAction(newData, oldData)};

        if (isFeatureEnabled(FEATURE_REMOVAL, state.activeView))
            editable = {...editable, onRowDelete: oldData => onRowDeleteAction(oldData)};

        return editable;
    }

    if (activeBucket == null && buckets.length === 0)
        return <MissingBucketTable/>
    else if (activeBucket == null && buckets.length > 0)
        return <div/>
    else if (state.activeView == null)
        return <MissingActiveView/>
    else
        return (
            <div style={{paddingTop: 0, paddingLeft: 0, paddingRight: 0}}>
                <MaterialTable
                    tableRef={tableRef}
                    title={`[${activeBucket.name}] ${state.activeView.description}`}
                    columns={state.tableColumns}
                    data={query =>
                        new Promise((resolve) => {
                            try {
                                if (pageSize !== query.pageSize) {
                                    setPageSize(query.pageSize);
                                    setLastPageSize(query.pageSize);
                                }

                                let url = getDataUrl(activeBucket) + '/get?';
                                url += 'limit=' + query.pageSize;
                                url += '&page=' + (query.page + 1);

                                if (query.orderBy != null) {
                                    const source = getColumnSource(state.tableColumns, query.orderBy.field);
                                    if (query.orderDirection === 'desc')
                                        url += '&sort=desc(' + source + ')';
                                    else
                                        url += '&sort=' + source;
                                }

                                let payload = {
                                    columns: getFetchColumns(state.tableColumns),
                                    conditions: consolidateAllConditions(query.search, query.filters),
                                    logic: getActiveViewFilterLogic()
                                }

                                let resultOk = true;
                                fetch(url, getPostOptions(payload))
                                    .then(handleErrors)
                                    .catch(error => {
                                        setMessageBox({open: true, severity: 'error', title: 'Error', message: error});
                                        resultOk = false;
                                    })
                                    .then(result => {
                                        if (resultOk)
                                            resolve({
                                                data: result.customData,
                                                page: result.page - 1,
                                                totalCount: result.total,
                                            })
                                        else
                                            resolve({data: []});
                                    });
                            } catch (error) {
                                setMessageBox({open: true, severity: 'error', title: 'Error', message: error});
                            }
                        })
                    }
                    options={{
                        paging: true,
                        pageSize: pageSize,
                        paginationType: 'stepped',
                        pageSizeOptions: getPageSizeOptionsOnDialog(),
                        actionsColumnIndex: -1,
                        debounceInterval: 700,
                        sorting: true,
                        selection: false,
                        filtering: filtering,
                        exportButton: isFeatureEnabled(FEATURE_EXPORT, state.activeView),
                        padding: 'dense',
                        search: isFeatureEnabled(FEATURE_SEARCH, state.activeView),
                        searchFieldStyle: {width: 500},
                        headerStyle: {position: 'sticky', top: 0, backgroundColor: getTableHeaderBackgroundColor(theme)},
                        maxBodyHeight: getTableHeight(height),
                        minBodyHeight: getTableHeight(height),
                        rowStyle: rowData => ({backgroundColor: getTableRowBackgroundColor(rowData, theme)})
                    }}
                    localization={{
                        body: {
                            addTooltip: 'Add'
                        },
                        toolbar: {
                            searchTooltip: 'Search properties',
                            searchPlaceholder: 'Search properties'
                        }
                    }}
                    components={{
                        Container: props => <div {...props} />,
                        Toolbar: props => {
                            const propsCopy = {...props};
                            propsCopy.showTitle = false;
                            return (
                                <Grid container direction="row">
                                    <Grid container direction={"row"} item xs={3} alignItems="center">
                                        <Grid item>
                                            {isFeatureEnabled(FEATURE_RESERVATION, state.activeView) && <ReserveDataDialog onReserve={onDataReserve}/>}
                                        </Grid>
                                        <Grid item>
                                            <ViewMenuSelector
                                                views={state.bucketViews}
                                                activeView={state.activeView}
                                                onChange={view => onViewSelected(view)}
                                            />
                                        </Grid>
                                    </Grid>
                                    <Grid item xs={9}>
                                        <MTableToolbar {...propsCopy} />
                                    </Grid>
                                </Grid>
                            );
                        }
                    }}
                    actions={getActions()}
                    editable={getEditable()}
                />

                <MessageBox
                    config={messageBox}
                    onClose={() => setMessageBox({...messageBox, open: false})}
                />

                <DataDetailsDialog
                    open={detailsState.open}
                    dataRow={detailsState.dataRow}
                    tags={tags}
                    onChange={(dataRow, changed) => onCloseDataDetailsDialog(dataRow, changed)}
                />

                <DataHistoryDialog
                    bucket={activeBucket}
                    dataRowId={historyState.dataRowId}
                    history={historyState.history}
                    tags={tags}
                    open={historyState.open}
                    onClose={() => onCloseDataHistoryDialog()}
                />

                <TaskExecutionDialog
                    bucket={activeBucket}
                    open={taskState.open}
                    onClose={onCloseTaskExecutionEditorDialog}
                    reload={reloadData}
                />
            </div>
        );
}