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
import {
    getLastActiveView, getLastBucketOrder,
    getLastBucketSearchedText,
    getLastPageSize,
    getUsername,
    setLastActiveView,
    setLastBucketOrder,
    setLastBucketSearchedText,
    setLastPageSize
} from "../../utils/ConfigurationStorage";
import {useWindowDimension} from "../utils/UseWindowDimension";
import {Grid, Icon} from "@material-ui/core";
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
    FEATURE_EXPORT, FEATURE_TASKS, FEATURE_DUPLICATE, FEATURE_RICH_FILTER, FEATURE_FILTER
} from "../utils/ViewFeatures";
import prepareTableColumns, {
    convertDataBeforeAdd,
    convertDataBeforeModify,
    getActiveView,
    getBucketTags, getBucketTasks,
    getBucketViews,
    getFetchColumns
} from "./BucketDataTableHelper";
import MissingActiveView from "./MissingActiveView";
import Refresh from "@material-ui/icons/Refresh";
import RateReviewOutlined from "@material-ui/icons/RateReviewOutlined";
import History from "@material-ui/icons/History";
import FilterList from "@material-ui/icons/FilterList";
import DuplicateIcon from '@material-ui/icons/ViewStream'
import {getDataByIdUrl, getDataHistoryUrl, getDataReserveUrl, getDataUrl} from "../../utils/UrlBuilder";
import {handleErrors} from "../../utils/FetchHelper";
import {MessageBox} from "../utils/MessageBox";
import DataDetailsDialog from "../dialogs/DataDetailsDialog";
import DataHistoryDialog from "../dialogs/DataHistoryDialog";
import ReserveDataDialog from "../dialogs/ReserveDataDialog";
import TaskExecutionDialog from "../dialogs/TaskExecutionDialog";
import RichFilterDialog from "../dialogs/RichFilterDialog";


// declared as a global because of component bug: https://github.com/mbrn/material-table/issues/2432
const tableRef = createRef();

export default function BucketDataTable() {

    const theme = useTheme();
    const [pageSize, setPageSize] = useState(getLastPageSize);
    const [messageBox, setMessageBox] = useState({open: false, severity: 'error', title: '', message: ''});
    const [height] = useWindowDimension();
    const [filtering, setFiltering] = useState(false);
    const accessContext = useContext(AccessContext);
    const {buckets, activeBucket, views, columns, filters, tags, tasks, enums, users} = accessContext;
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
    const [richFilterState, setRichFilterState] = useState({
        open: false
    });
    const [state, setState] = useState({
        bucketViews: [],    // all views available for active bucket that user has access
        bucketTags: [],
        bucketTasks: [],
        activeView: null,
        columnsDef: [],     // pure columns definition
        activeLogic: null,    // the logic from active view or from rich filter
        tableColumns: [],   // columns prepared for material table,
        resetPage: false
    });
    let searchText = activeBucket != null ? getLastBucketSearchedText(activeBucket.id) : "";
    const [changedBucket, setChangedBucket] = useState(false);

    // active bucket has been changed
    useEffect(() => {
        setChangedBucket(true);
        setFiltering(false);
        const bucketViews = getBucketViews(activeBucket, views);
        if (bucketViews.length > 0 && tags != null && enums != null && views != null && columns != null) {
            const orderBy = getLastBucketOrder(activeBucket.id);

            if (tableRef !== null && tableRef.current !== null) {
                tableRef.current.dataManager.changeSearchText(searchText);
                tableRef.current.dataManager.orderBy = -1;
                tableRef.current.dataManager.orderDirection = "";
                tableRef.current.setState({searchText: searchText});
                tableRef.current.setState(tableRef.current.dataManager.getRenderState());
            }

            const bucketTags = getBucketTags(activeBucket, tags);
            const bucketTasks = getBucketTasks(activeBucket, tasks);
            const lastActiveViewId = getLastActiveView(activeBucket.id);
            const activeView = getActiveView(bucketViews, lastActiveViewId);
            const columnsDef = columns.filter(c => c.id === activeView.columnsId)[0];
            const tableColumns = prepareTableColumns(columnsDef, bucketTags, enums, users, orderBy);
            const filteredFilters = filters.filter(f => f.id === activeView.filterId);
            const activeLogic = filteredFilters.length > 0 ? filteredFilters[0].configuration.logic : null;

            setState({
                ...state,
                bucketViews: bucketViews,
                bucketTags: bucketTags,
                bucketTasks: bucketTasks,
                activeView: activeView,
                columnsDef: columnsDef,
                activeLogic: activeLogic,
                tableColumns: tableColumns,
                resetPage: true
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
                activeLogic: null,
                tableColumns: [],
                resetPage: true
            });

    }, [activeBucket, enums, tags, views, columns, filters]);

    const handleSearchChange = (text) => {
        searchText = text;
        setLastBucketSearchedText(activeBucket.id, text);
    }

    const resetPageAndReload = () => {
        setState({...state, resetPage: true});
        reloadData();
    }

    const setActiveLogic = (logic) => {
        setState({...state, activeLogic: logic, resetPage: true});
        reloadData();
    }

    // active view has been changed
    const onViewSelected = (view) => {
        setFiltering(false);
        setLastActiveView(activeBucket.id, view.id);
        const columnsDef = columns.filter(col => col.id === view.columnsId)[0];
        const filteredFilters = filters.filter(f => f.id === view.filterId);
        const activeLogic = filteredFilters.length > 0 ? filteredFilters[0].configuration.logic : null;
        const tableColumns = prepareTableColumns(columnsDef, state.bucketTags, enums, users, getLastBucketOrder(activeBucket.id));

        setState({
            ...state,
            activeView: view,
            columnsDef: columnsDef,
            activeLogic: activeLogic,
            tableColumns: tableColumns,
            resetPage: true
        });
        reloadData();
    }

    const onOpenTaskExecutionEditorDialog = () => {
        setTaskState({...taskState, open: true});
    }

    const onCloseTaskExecutionEditorDialog = () => {
        setTaskState({...taskState, open: false});
    }

    const onOpenRichFilterDialog = () => {
        setRichFilterState({...richFilterState, open: true});
    }

    const onCloseRichFilterDialog = () => {
        setRichFilterState({...richFilterState, open: false});
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

    const onDuplicateData = (rowData) => {
        let resultOk = true;
        fetch(getDataByIdUrl(activeBucket, getRowDataId(rowData)), getGetOptions())
            .then(handleErrors)
            .catch(error => {
                setMessageBox({open: true, severity: 'error', title: 'Error', message: error});
                resultOk = false;
            })
            .then(dataRow => {
                if (resultOk) {
                    const duplicatedData = {
                        tagId: dataRow.tagId,
                        reserved: dataRow.reserved,
                        properties: dataRow.properties
                    };
                    fetch(getDataUrl(activeBucket), getPostOptions(duplicatedData))
                        .then(handleErrors)
                        .catch(error => {
                            setMessageBox({open: true, severity: 'error', title: 'Error', message: error});
                            resultOk = false;
                        })
                        .then(() => {
                            if (resultOk) {
                                reloadData();
                            }
                    });
                }
            });
    }

    const onCloseDataHistoryDialog = () => {
        setHistoryState({...historyState, open: false});
    }

    const onDataReserve = ({random, number, username}) => {
        const query = tableRef.current.state.query;
        let payload = {
            targetOwnerUsername: username !== getUsername() ? username : null,
            conditions: consolidateAllConditions(query.search, query.filters),
            logic: state.activeLogic
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
                        // Omit when number of arrays items is 0. Do not put the following condition into parent condition!
                        if (filter.value.length > 0)
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

    const richFilterAction = {
        icon: () => state.activeLogic != null ? <Icon color={'secondary'}><span className="material-icons" >filter_alt</span></Icon> : <span className="material-icons">filter_alt</span>,
        tooltip: 'Rich filter',
        isFreeAction: true,
        onClick: () => {
            onOpenRichFilterDialog();
        }
    };

    const filterAction = {
        icon: () => filtering && tableRef.current.state.query.filters.length > 0 ? <FilterList color={'secondary'}/> : <FilterList/>,
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

    const duplicateAction = {
        icon: () => <DuplicateIcon/>,
        tooltip: 'Duplicate data',
        onClick: (event, rowData) => {
            onDuplicateData(rowData);
        }
    };

    const getActions = () => {
        let actions = [];
        actions.push(refreshAction);
        if (isFeatureEnabled(FEATURE_TASKS, state.activeView)) actions.push(tasksAction);
        if (isFeatureEnabled(FEATURE_FILTER, state.activeView)) actions.push(filterAction);
        if (isFeatureEnabled(FEATURE_RICH_FILTER, state.activeView)) actions.push(richFilterAction);
        if (isFeatureEnabled(FEATURE_DETAILS, state.activeView)) actions.push(detailsAction);
        if (isFeatureEnabled(FEATURE_HISTORY, state.activeView)) actions.push(historyAction);
        if (isFeatureEnabled(FEATURE_DUPLICATE, state.activeView)) actions.push(duplicateAction);
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
    else {
        return (
            <div style={{paddingTop: 0, paddingLeft: 0, paddingRight: 0}}>
                <MaterialTable
                    tableRef={tableRef}
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
                                if (state.resetPage) {
                                    url += '&page=1';
                                    setState({...state, resetPage: false});
                                } else
                                    url += '&page=' + (query.page + 1);

                                // take sorting from parameter
                                if (changedBucket === true) {
                                    setChangedBucket(false);
                                    const orderBy = getLastBucketOrder(activeBucket.id);
                                    if (orderBy != null && state.tableColumns.length > 0) {
                                        let source = state.tableColumns[0].source;
                                        if (state.tableColumns.length > orderBy.colId)
                                            source = state.tableColumns[orderBy.colId].source;
                                        if (orderBy.ord === 'desc')
                                            url += '&sort=desc(' + source + ')';
                                        else
                                            url += '&sort=' + source;
                                    }
                                } else {
                                    if (query.orderBy != null) {
                                        if (query.orderDirection === 'desc')
                                            url += '&sort=desc(' + query.orderBy.source + ')';
                                        else
                                            url += '&sort=' + query.orderBy.source;
                                    }
                                }

                                let payload = {
                                    columns: getFetchColumns(state.tableColumns),
                                    conditions: consolidateAllConditions(searchText, query.filters),
                                    logic: state.activeLogic
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
                                        <MTableToolbar
                                            {...props}
                                            showTitle={false}
                                            onSearchChanged={text => {
                                                handleSearchChange(text);
                                                props.onSearchChanged(text);
                                            }}
                                        />
                                    </Grid>
                                </Grid>
                            );
                        }
                    }}
                    onOrderChange={(colId, ord) => {
                        let order = (colId >= 0) ? {colId, ord} : null;
                        setLastBucketOrder(activeBucket.id, order);
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
                    reload={resetPageAndReload}
                    activeLogic={state.activeLogic}
                />

                <RichFilterDialog
                    bucket={activeBucket}
                    open={richFilterState.open}
                    onClose={onCloseRichFilterDialog}
                    activeLogic={state.activeLogic}
                    setActiveLogic={setActiveLogic}
                />
            </div>
        );
    }
}