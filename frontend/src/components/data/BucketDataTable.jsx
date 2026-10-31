import React, {createContext, createRef, useContext, useEffect, useState} from 'react';
import MaterialTable, {MTableActions, MTableToolbar} from '@material-table/core';
import {
    getDeleteOptions,
    getGetOptions,
    getPageSizeOptionsOnDialog,
    getPostOptions,
    getPutOptions,
    getTableHeaderBackgroundColor,
    getTableHeight,
    getTableRowBackgroundColor,
    getTableToolbarBackgroundColor
} from "../../utils/MaterialTableHelper";
import {
    clearBucketColumns,
    clearBucketTableState,
    getBucketColumns,
    getBucketPageSize,
    getBucketTableState,
    getLastActiveView,
    getLastPageSize,
    getUsername,
    setBucketColumns,
    setBucketPageSize,
    setBucketTableState,
    setLastActiveView
} from "../../utils/ConfigurationStorage";
import {
    Checkbox,
    Divider,
    Grid,
    Icon,
    IconButton,
    ListItemIcon,
    Menu,
    MenuItem,
    Tooltip,
    useTheme
} from "@mui/material";
import ViewMenuSelector from "./ViewMenuSelector";
import AccessContext from "../../context/access/AccessContext";
import MissingBucketTable from "./MissingBucketTable";
import {
    FEATURE_AVAILABLE_TAGS,
    FEATURE_COMPACT_ROW_ACTIONS,
    FEATURE_CREATION,
    FEATURE_DETAILS,
    FEATURE_DUPLICATE,
    FEATURE_EXPORT,
    FEATURE_FILTER,
    FEATURE_HISTORY,
    FEATURE_MODIFYING,
    FEATURE_REMOVAL,
    FEATURE_RESERVATION,
    FEATURE_RICH_FILTER,
    FEATURE_SEARCH,
    FEATURE_TASKS,
    isFeatureEnabled
} from "../utils/ViewFeatures";
import prepareTableColumns, {
    convertDataBeforeAdd,
    convertDataBeforeModify,
    getActiveView,
    getBucketTags,
    getBucketTasks,
    getBucketViews,
    getColumnsLayout,
    getFetchColumns
} from "./BucketDataTableHelper";
import MissingActiveView from "./MissingActiveView";
import Refresh from "@mui/icons-material/Refresh";
import RateReviewOutlined from "@mui/icons-material/RateReviewOutlined";
import History from "@mui/icons-material/History";
import FilterList from "@mui/icons-material/FilterList";
import DuplicateIcon from '@mui/icons-material/ViewStream'
import SettingsBackupRestore from "@mui/icons-material/SettingsBackupRestore";
import ViewColumn from "@mui/icons-material/ViewColumn";
import MoreVert from "@mui/icons-material/MoreVert";
import SaveAlt from "@mui/icons-material/SaveAlt";
import {exportCsv} from "../../utils/CsvExport";
import {selectCellText} from "../../utils/TableCellSelect";
import "./BucketDataTable.css";
import {
    getDataByIdUrl,
    getDataHistoryUrl,
    getDataReserveUrl,
    getDataUrl
} from "../../utils/UrlBuilder";
import {handleErrors} from "../../utils/FetchHelper";
import {MessageBox} from "../utils/MessageBox";
import DataDetailsDialog from "../dialogs/DataDetailsDialog";
import DataHistoryDialog from "../dialogs/DataHistoryDialog";
import ReserveDataDialog from "../dialogs/ReserveDataDialog";
import TaskExecutionDialog from "../dialogs/TaskExecutionDialog";
import RichFilterDialog from "../dialogs/RichFilterDialog";
import AvailableTagsDialog from "../dialogs/AvailableTagsDialog";

// declared as a global because of component bug: https://github.com/mbrn/material-table/issues/2432
const tableRef = createRef();

// the maximum number of rows exported at once
const EXPORT_LIMIT = 10000;

// the table's columns and export buttons are always the first toolbar buttons, so they are rendered here, after Refresh
const ColumnsMenu = ({columns, onColumnsChanged, columnsArranged, onResetColumns}) => {
    const [anchorEl, setAnchorEl] = useState(null);
    return (
        <span>
            <Tooltip title='Show columns'>
                <IconButton color="inherit" onClick={event => setAnchorEl(event.currentTarget)}>
                    <ViewColumn/>
                </IconButton>
            </Tooltip>
            <Menu anchorEl={anchorEl} open={Boolean(anchorEl)} onClose={() => setAnchorEl(null)}>
                <MenuItem disabled style={{opacity: 1, fontWeight: 600, fontSize: 12}}>
                    Add or remove columns
                </MenuItem>
                {[...columns].sort((a, b) => a.tableData.columnOrder - b.tableData.columnOrder).map(column =>
                    <MenuItem key={column.tableData.id} onClick={() => onColumnsChanged(column, !column.hidden)}>
                        <Checkbox checked={!column.hidden}/>
                        <span>{column.title}</span>
                    </MenuItem>
                )}
                <Divider/>
                <MenuItem
                    disabled={!columnsArranged}
                    onClick={() => {
                        setAnchorEl(null);
                        onResetColumns();
                    }}
                >
                    <ListItemIcon><SettingsBackupRestore fontSize="small"/></ListItemIcon>
                    Reset columns
                </MenuItem>
            </Menu>
        </span>
    );
}

const ExportMenu = ({onExport}) => {
    const [anchorEl, setAnchorEl] = useState(null);
    const exportRows = (allRows) => {
        setAnchorEl(null);
        onExport(allRows);
    }
    return (
        <span>
            <Tooltip title='Export'>
                <IconButton color="inherit" onClick={event => setAnchorEl(event.currentTarget)}>
                    <SaveAlt/>
                </IconButton>
            </Tooltip>
            <Menu anchorEl={anchorEl} open={Boolean(anchorEl)} onClose={() => setAnchorEl(null)}>
                <MenuItem onClick={() => exportRows(false)}>Export CSV - current page</MenuItem>
                <MenuItem onClick={() => exportRows(true)}>Export CSV - all matching rows</MenuItem>
            </Menu>
        </span>
    );
}

// the icon of a table action, as the table renders it
const actionIcon = (action) => {
    switch (typeof action.icon) {
        case 'string':
            return <Icon {...action.iconProps}>{action.icon}</Icon>;
        case 'function':
            return action.icon({...action.iconProps});
        case 'undefined':
            return null;
        default:
            return <action.icon {...action.iconProps}/>;
    }
}

// The actions of a row under one button (the view's Compact row actions feature)
const RowActionsMenu = ({actions, data, disabled}) => {
    const [anchorEl, setAnchorEl] = useState(null);
    // an action may be a function of the row (the table's edit and delete actions)
    const rowActions = actions
        .map(action => typeof action === 'function' ? action(data) : action)
        .filter(action => action != null && !action.hidden);
    return (
        <>
            <Tooltip title='Actions'>
                <span>
                    <IconButton size="small" color="inherit" disabled={disabled}
                                onClick={event => setAnchorEl(event.currentTarget)}>
                        <MoreVert/>
                    </IconButton>
                </span>
            </Tooltip>
            <Menu anchorEl={anchorEl} open={Boolean(anchorEl)} onClose={() => setAnchorEl(null)}>
                {rowActions.map((action, index) =>
                    <MenuItem
                        key={index}
                        disabled={action.disabled}
                        onClick={event => {
                            setAnchorEl(null);
                            action.onClick(event, data);
                        }}
                    >
                        <ListItemIcon>{actionIcon(action)}</ListItemIcon>
                        {action.tooltip}
                    </MenuItem>
                )}
            </Menu>
        </>
    );
}

// The toolbar and its actions get the state of BucketDataTable from this context. They are components declared once,
// so that a render of BucketDataTable doesn't create them anew (which would close an open menu).
const DataTableContext = createContext(null);

// toolbar actions come with the columns, row actions without them
const TableActions = (props) => {
    const {columnsArranged, onResetColumns, exportEnabled, onExport, compactRowActions} = useContext(DataTableContext);
    if (props.columns == null) {
        // a row being edited (its save and cancel actions) comes without "disabled", and keeps its buttons
        if (compactRowActions && props.disabled !== undefined)
            return <RowActionsMenu actions={props.actions} data={props.data} disabled={props.disabled}/>;
        return <MTableActions {...props}/>;
    }
    const refresh = props.actions.filter(action => action.tooltip === 'Refresh');
    const others = props.actions.filter(action => action.tooltip !== 'Refresh');
    return (
        <div style={{display: 'flex'}}>
            <MTableActions {...props} actions={refresh}/>
            <ColumnsMenu
                columns={props.columns}
                onColumnsChanged={props.onColumnsChanged}
                columnsArranged={columnsArranged}
                onResetColumns={onResetColumns}
            />
            {exportEnabled && <ExportMenu onExport={onExport}/>}
            <MTableActions {...props} actions={others}/>
        </div>
    );
}

const richFilterIcon = (activeLogic) => () => activeLogic != null ?
    <Icon color={'secondary'}><span className="material-icons">filter_alt</span></Icon>
    :
    <span className="material-icons">filter_alt</span>;

const taskActionIcon = () => <span className="material-icons">edit_note</span>;
// tableRef is empty while a new table (another bucket or view) is being created
const filterIcon = (filtering) => () => filtering
&& tableRef.current != null && tableRef.current.state.query.filters.length > 0 ?
    <FilterList color={'secondary'}/> :
    <FilterList/>;

const TableToolbar = (props) => {
    const {state, onViewSelected, onDataReserve, handleSearchChange, theme} = useContext(DataTableContext);
    return (
        <div style={{backgroundColor: getTableToolbarBackgroundColor(theme)}}>
            <Grid container direction="row">
                <Grid container item direction="row" xs={3} alignItems="center">
                    {isFeatureEnabled(FEATURE_RESERVATION, state.activeView) &&
                    <Grid item>
                        <ReserveDataDialog onReserve={onDataReserve}/>
                    </Grid>
                    }
                    {isFeatureEnabled(FEATURE_AVAILABLE_TAGS, state.activeView)
                    && <Grid item>
                        <AvailableTagsDialog bucketTags={state.bucketTags}/>
                    </Grid>
                    }
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
        </div>
    );
};
export default function BucketDataTable() {

    const theme = useTheme();
    const [pageSize, setPageSize] = useState(getLastPageSize);
    const [messageBox, setMessageBox] = useState({open: false, severity: 'error', title: '', message: ''});
    const [filtering, setFiltering] = useState(false);
    const accessContext = useContext(AccessContext);
    const {
        buckets,
        activeBucket,
        views,
        columns,
        filters,
        tags,
        tasks,
        enums,
        users
    } = accessContext;
    const [detailsState, setDetailsState] = useState({
        open: false,
        dataRow: null
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
        bucketId: null,     // the bucket the state below was prepared for
        bucketViews: [],    // all views available for active bucket that user has access
        bucketTags: [],
        bucketTasks: [],
        activeView: null,
        columnsDef: [],     // pure columns definition
        activeLogic: null,    // the logic from active view or from rich filter
        tableColumns: [],   // columns prepared for material table,
        resetPage: false
    });
    // the table restored for the active bucket: search text, page, sort and filters it starts with
    const [tableState, setTableState] = useState({});
    // columns arranged by the user (order, visibility) are saved; a new version recreates the table with the view's columns
    const [columnsArranged, setColumnsArranged] = useState(false);
    const [columnsVersion, setColumnsVersion] = useState(0);

    const getViewLogic = (view) => {
        const filteredFilters = filters.filter(f => f.id === view.filterId);
        return filteredFilters.length > 0 ? filteredFilters[0].configuration.logic : null;
    }

    // active bucket has been changed
    useEffect(() => {
        const bucketViews = getBucketViews(activeBucket, views);
        if (bucketViews.length > 0 && tags != null && enums != null && views
            != null && columns != null) {
            const bucketTags = getBucketTags(activeBucket, tags);
            const bucketTasks = getBucketTasks(activeBucket, tasks);
            const lastActiveViewId = getLastActiveView(activeBucket.id);
            const activeView = getActiveView(bucketViews, lastActiveViewId);

            // the saved state belongs to one view; another view starts clean
            let savedState = getBucketTableState(activeBucket.id);
            if (savedState.viewId !== activeView.id) {
                clearBucketTableState(activeBucket.id);
                setBucketTableState(activeBucket.id, {viewId: activeView.id});
                savedState = {viewId: activeView.id};
            }

            const columnsDef = columns.filter(c => c.id === activeView.columnsId)[0];
            const columnsLayout = getBucketColumns(activeBucket.id, activeView.id);
            const tableColumns = prepareTableColumns(columnsDef, bucketTags, enums, users, savedState.order, savedState.filters, columnsLayout);
            const activeLogic = savedState.logic !== undefined ? savedState.logic : getViewLogic(activeView);

            setFiltering(savedState.filtering === true);
            setColumnsArranged(columnsLayout != null);
            setPageSize(getBucketPageSize(activeBucket.id));
            setTableState(savedState);
            setState({
                ...state,
                bucketId: activeBucket.id,
                bucketViews: bucketViews,
                bucketTags: bucketTags,
                bucketTasks: bucketTasks,
                activeView: activeView,
                columnsDef: columnsDef,
                activeLogic: activeLogic,
                tableColumns: tableColumns,
                resetPage: false
            });
            // another bucket gets a new table (see key), which loads its data itself
            if (state.bucketId === activeBucket.id)
                reloadData();
        } else {
            setFiltering(false);
            setState({
                ...state,
                bucketId: activeBucket != null ? activeBucket.id : null,
                bucketViews: [],
                bucketTags: [],
                bucketTasks: [],
                activeView: null,
                columnsDef: [],
                activeLogic: null,
                tableColumns: [],
                resetPage: true
            });
        }

    }, [activeBucket, enums, tags, views, columns, filters]);

    const handleSearchChange = (text) => {
        setBucketTableState(activeBucket.id, {searchText: text});
    }

    const resetPageAndReload = () => {
        setState({...state, resetPage: true});
        reloadData();
    }

    const setActiveLogic = (logic) => {
        setBucketTableState(activeBucket.id, {logic: logic});
        setState({...state, activeLogic: logic, resetPage: true});
        reloadData();
    }

    // active view has been changed: the table starts clean with the view's filter
    const onViewSelected = (view) => {
        setFiltering(false);
        setLastActiveView(activeBucket.id, view.id);
        clearBucketTableState(activeBucket.id);
        setBucketTableState(activeBucket.id, {viewId: view.id});
        const columnsDef = columns.filter(col => col.id === view.columnsId)[0];
        const columnsLayout = getBucketColumns(activeBucket.id, view.id);
        const tableColumns = prepareTableColumns(columnsDef, state.bucketTags, enums, users, null, null, columnsLayout);

        setColumnsArranged(columnsLayout != null);
        setTableState({viewId: view.id});
        setState({
            ...state,
            activeView: view,
            columnsDef: columnsDef,
            activeLogic: getViewLogic(view),
            tableColumns: tableColumns,
            resetPage: true
        });
    }

    // the request for the data matching the query of the table: search, filters, sort and the rich filter
    const getDataRequest = (query, limit) => {
        let url = getDataUrl(activeBucket) + '/get?';
        url += 'limit=' + limit;

        // orderByCollection also holds the saved sort the table starts with
        const sorted = (query.orderByCollection || []).find(c => c.orderDirection);
        const sortColumn = sorted != null ? state.tableColumns.find(c => c.tableData != null && c.tableData.id === sorted.orderBy) : null;
        if (sortColumn != null) {
            if (sorted.orderDirection === 'desc') {
                url += '&sort=desc('
                    + sortColumn.source + ')';
            } else {
                url += '&sort='
                    + sortColumn.source;
            }
        }

        const payload = {
            columns: getFetchColumns(state.tableColumns),
            conditions: consolidateAllConditions(query.search, query.filters),
            logic: state.activeLogic
        }
        return {url, payload};
    }

    const getExportFileName = () => `${activeBucket.name}-${new Date().toISOString().slice(0, 19).replace(/[T:]/g, '-')}`;

    // the visible columns in the order of the table; values as shown (tag and enum names instead of ids)
    const exportRows = (allRows) => {
        const columns = tableRef.current.dataManager.columns
            .filter(column => !column.hidden && column.field)
            .sort((a, b) => a.tableData.columnOrder - b.tableData.columnOrder);
        const toCsvRows = data => data.map(rowData => {
            const row = {};
            columns.forEach(column => row[column.field] = tableRef.current.dataManager.getFieldValue(rowData, column));
            return row;
        });

        if (!allRows) {
            exportCsv(getExportFileName(), columns, toCsvRows(tableRef.current.state.renderData));
            return;
        }

        const {url, payload} = getDataRequest(tableRef.current.state.query, EXPORT_LIMIT);
        fetch(url + '&page=1', getPostOptions(payload))
            .then(handleErrors)
            .then(result => {
                exportCsv(getExportFileName(), columns, toCsvRows(result.customData));
                if (result.total > EXPORT_LIMIT)
                    setMessageBox({
                        open: true,
                        severity: 'warning',
                        title: 'Export',
                        message: `Exported the first ${EXPORT_LIMIT} of ${result.total} rows.`
                    });
            })
            .catch(error => {
                setMessageBox({
                    open: true,
                    severity: 'error',
                    title: 'Error',
                    message: error
                });
            });
    }

    const saveColumnsLayout = () => {
        setBucketColumns(activeBucket.id, state.activeView.id, getColumnsLayout(tableRef.current.dataManager.columns));
        setColumnsArranged(true);
    }

    // a new table with the view's columns; search, sort, filters and page stay
    const resetColumns = () => {
        clearBucketColumns(activeBucket.id, state.activeView.id);
        const savedState = getBucketTableState(activeBucket.id);
        const tableColumns = prepareTableColumns(state.columnsDef, state.bucketTags, enums, users, savedState.order, savedState.filters, null);
        setColumnsArranged(false);
        setTableState(savedState);
        setState({...state, tableColumns: tableColumns});
        setColumnsVersion(columnsVersion + 1);
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
        fetch(getDataByIdUrl(activeBucket, getRowDataId(rowData)),
            getGetOptions())
            .then(handleErrors)
            .catch(error => {
                setMessageBox({
                    open: true,
                    severity: 'error',
                    title: 'Error',
                    message: error
                });
                resultOk = false;
            })
            .then(dataRow => {
                if (resultOk) {
                    setDetailsState({
                        dataRow: dataRow,
                        open: true
                    });
                }
            });
    }

    const onCloseDataDetailsDialog = (dataRow, changed) => {
        if (changed) {
            let resultOk = true;
            fetch(getDataByIdUrl(activeBucket, dataRow.id),
                getPutOptions({properties: dataRow.properties}))
                .then(handleErrors)
                .catch(error => {
                    setMessageBox({
                        open: true,
                        severity: 'error',
                        title: 'Error',
                        message: error
                    });
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
        fetch(getDataHistoryUrl(activeBucket, getRowDataId(rowData)),
            getGetOptions())
            .then(handleErrors)
            .catch(error => {
                setMessageBox({
                    open: true,
                    severity: 'error',
                    title: 'Error',
                    message: error
                });
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
        fetch(getDataByIdUrl(activeBucket, getRowDataId(rowData)),
            getGetOptions())
            .then(handleErrors)
            .catch(error => {
                setMessageBox({
                    open: true,
                    severity: 'error',
                    title: 'Error',
                    message: error
                });
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
                            setMessageBox({
                                open: true,
                                severity: 'error',
                                title: 'Error',
                                message: error
                            });
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
        fetch(getDataReserveUrl(activeBucket, number, random),
            getPostOptions(payload))
            .then(handleErrors)
            .catch(error => {
                setMessageBox({
                    open: true,
                    severity: 'error',
                    title: 'Error',
                    message: error
                });
                resultOk = false;
            })
            .then((response) => {
                if (resultOk) {
                    if (response.hasOwnProperty("message")) {
                        setMessageBox({
                            open: true,
                            severity: 'info',
                            title: 'Info',
                            message: response.message
                        });
                    } else {
                        reloadData();
                    }
                }
            });
    }

    const consolidateAllConditions = (tableSearch, tableFilters) => {
        let allConditions = [];
        if (tableSearch != null && tableSearch.length > 0) {
            allConditions.push({
                left_source: 'field',
                left_value: 'properties',
                operator: 'like',
                right_source: 'const',
                right_value: '%' + tableSearch + '%'
            });
        }

        if (filtering && tableFilters.length > 0) {
            for (const filter of tableFilters) {
                if (filter.column.source.startsWith('$')) {
                    const leftSource = filter.column.source.endsWith('()')
                        ? 'function' : 'property';

                    if (filter.column.type === 'numeric') {
                        if (Array.isArray(filter.value)) {
                            if (filter.value.length > 0) {
                                allConditions.push({
                                    left_source: leftSource,
                                    left_value: filter.column.source,
                                    operator: 'in',
                                    right_source: 'const',
                                    right_value: filter.value
                                });
                            }
                        } else {
                            allConditions.push({
                                left_source: leftSource,
                                left_value: filter.column.source,
                                operator: '=',
                                right_source: 'const',
                                right_value: parseFloat(filter.value)
                            });
                        }
                    } else if (filter.column.type === 'boolean') {
                        allConditions.push({
                            left_source: leftSource,
                            left_value: filter.column.source,
                            operator: '=',
                            right_source: 'const',
                            right_value: (filter.value === 'checked')
                        });
                    } else if (Array.isArray(filter.value)
                        && filter.value.length
                        > 0) {
                        allConditions.push({
                            left_source: leftSource,
                            left_value: filter.column.source,
                            operator: 'in',
                            right_source: 'const',
                            right_value: filter.value
                        });
                    } else if (filter.value.length > 0) {
                        allConditions.push({
                            left_source: leftSource,
                            left_value: filter.column.source,
                            operator: 'like',
                            right_source: 'const',
                            right_value: '%' + filter.value + '%'
                        });
                    }

                } else {
                    if (filter.column.type === 'numeric') {
                        if (Array.isArray(filter.value)) {
                            if (filter.value.length > 0) {
                                const numericList = filter.value.map(
                                    value => parseFloat(value));
                                allConditions.push({
                                    left_source: 'field',
                                    left_value: filter.column.source,
                                    operator: 'in',
                                    right_source: 'const',
                                    right_value: numericList
                                });
                            }
                        } else {
                            allConditions.push({
                                left_source: 'field',
                                left_value: filter.column.source,
                                operator: '=',
                                right_source: 'const',
                                right_value: parseFloat(filter.value)
                            });
                        }
                    } else if (filter.column.type === 'boolean') {
                        allConditions.push({
                            left_source: 'field',
                            left_value: filter.column.source,
                            operator: '=',
                            right_source: 'const',
                            right_value: (filter.value === 'checked')
                        });
                    } else if (Array.isArray(filter.value)
                        && filter.value.length
                        > 0) {
                        allConditions.push({
                            left_source: 'field',
                            left_value: filter.column.source,
                            operator: 'in',
                            right_source: 'const',
                            right_value: filter.value
                        });
                    } else if (filter.value.length > 0) {
                        const filterValue = (filter.value === '@currentUser')
                            ? getUsername() : '%' + filter.value + '%';
                        allConditions.push({
                            left_source: 'field',
                            left_value: filter.column.source,
                            operator: 'like',
                            right_source: 'const',
                            right_value: filterValue
                        });
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
        icon: taskActionIcon,
        tooltip: 'Task execution',
        isFreeAction: true,
        onClick: () => {
            onOpenTaskExecutionEditorDialog()
        }
    };

    const richFilterAction = {
        icon: richFilterIcon(state.activeLogic),
        tooltip: 'Rich filter',
        isFreeAction: true,
        onClick: onOpenRichFilterDialog
    };

    const filterAction = {
        icon: filterIcon(filtering),
        tooltip: 'Enable/disable filter',
        isFreeAction: true,
        onClick: () => {
            setBucketTableState(activeBucket.id, {filtering: !filtering});
            setFiltering(!filtering);
            tableRef.current.state.query.filters.length > 0 && reloadData();
        }
    };

    const refreshAction = {
        icon: Refresh,
        tooltip: 'Refresh',
        isFreeAction: true,
        onClick: () => {
            reloadData();
        }
    };

    const detailsAction = {
        icon: RateReviewOutlined,
        tooltip: 'Data details',
        onClick: (event, rowData) => {
            onOpenDataDetailsDialog(rowData);
        }
    };

    const historyAction = {
        icon: History,
        tooltip: 'Data history',
        onClick: (event, rowData) => {
            onOpenDataHistoryDialog(rowData);
        }
    };

    const duplicateAction = {
        icon: DuplicateIcon,
        tooltip: 'Duplicate data',
        onClick: (event, rowData) => {
            onDuplicateData(rowData);
        }
    };

    const getActions = () => {
        let actions = [];
        actions.push(refreshAction);
        if (isFeatureEnabled(FEATURE_TASKS, state.activeView)) {
            actions.push(tasksAction);
        }
        if (isFeatureEnabled(FEATURE_FILTER, state.activeView)) {
            actions.push(filterAction);
        }
        if (isFeatureEnabled(FEATURE_RICH_FILTER, state.activeView)) {
            actions.push(richFilterAction);
        }
        if (isFeatureEnabled(FEATURE_DETAILS, state.activeView)) {
            actions.push(detailsAction);
        }
        if (isFeatureEnabled(FEATURE_HISTORY, state.activeView)) {
            actions.push(historyAction);
        }
        if (isFeatureEnabled(FEATURE_DUPLICATE, state.activeView)) {
            actions.push(duplicateAction);
        }
        return actions;
    }

    const onRowAddAction = (newData) => new Promise((resolve, reject) => {
        let resultOk = true;
        fetch(getDataUrl(activeBucket),
            getPostOptions(convertDataBeforeAdd(state.tableColumns, newData)))
            .then(handleErrors)
            .catch(error => {
                setMessageBox({
                    open: true,
                    severity: 'error',
                    title: 'Error',
                    message: error
                });
                resultOk = false;
            })
            .then(() => {
                resultOk ? resolve() : reject();
            });
    });

    const onRowUpdateAction = (newData, oldData) => new Promise(
        (resolve, reject) => {
            let payload = convertDataBeforeModify(state.tableColumns, newData,
                oldData);
            let resultOk = true;
            if (Object.keys(payload).length > 0) {
                fetch(getDataByIdUrl(activeBucket, getRowDataId(newData)),
                    getPutOptions(payload))
                    .then(handleErrors)
                    .catch(error => {
                        setMessageBox({
                            open: true,
                            severity: 'error',
                            title: 'Error',
                            message: error
                        });
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
        fetch(getDataByIdUrl(activeBucket, getRowDataId(oldData)),
            getDeleteOptions())
            .then(handleErrors)
            .catch(error => {
                setMessageBox({
                    open: true,
                    severity: 'error',
                    title: 'Error',
                    message: error
                });
                resultOk = false;
            })
            .then(() => {
                resultOk ? resolve() : reject();
            });
    });

    const getEditable = () => {
        let editable = {};

        if (isFeatureEnabled(FEATURE_CREATION, state.activeView)) {
            editable = {
                ...editable,
                onRowAdd: (newData) => onRowAddAction(newData)
            };
        }

        if (isFeatureEnabled(FEATURE_MODIFYING, state.activeView)) {
            editable = {
                ...editable,
                onRowUpdate: (newData, oldData) => onRowUpdateAction(newData,
                    oldData)
            };
        }

        if (isFeatureEnabled(FEATURE_REMOVAL, state.activeView)) {
            editable = {
                ...editable,
                onRowDelete: oldData => onRowDeleteAction(oldData)
            };
        }

        return editable;
    }

    if (activeBucket == null && buckets.length === 0) {
        return <MissingBucketTable/>
    } else if ((activeBucket == null && buckets.length > 0) || state.bucketId !== activeBucket.id) {
        // the state of another bucket must not be used to load this one's data
        return <div/>
    } else if (state.activeView == null) {
        return <MissingActiveView/>
    } else {
        return (
            <React.StrictMode>
                <DataTableContext.Provider value={{
                    state,
                    onViewSelected,
                    onDataReserve,
                    handleSearchChange,
                    theme,
                    columnsArranged,
                    onResetColumns: resetColumns,
                    exportEnabled: isFeatureEnabled(FEATURE_EXPORT, state.activeView),
                    onExport: exportRows,
                    compactRowActions: isFeatureEnabled(FEATURE_COMPACT_ROW_ACTIONS, state.activeView)
                }}>
                <div onDoubleClick={selectCellText}
                     className={isFeatureEnabled(FEATURE_COMPACT_ROW_ACTIONS, state.activeView) ? 'compact-row-actions' : undefined}>
                <MaterialTable
                    // a new table for each bucket and view, so it starts with the state saved for them
                    key={`${state.bucketId}-${state.activeView.id}-${columnsVersion}`}
                    title={activeBucket.name}
                    tableRef={tableRef}
                    columns={state.tableColumns}
                    data={query => new Promise((resolve) => {
                            try {
                                if (pageSize !== query.pageSize) {
                                    setPageSize(query.pageSize);
                                    setBucketPageSize(activeBucket.id, query.pageSize);
                                }

                                let page = query.page + 1;
                                if (state.resetPage) {
                                    page = 1;
                                    setState({...state, resetPage: false});
                                }

                                const {url, payload} = getDataRequest(query, query.pageSize);
                                const getPage = page => fetch(url + '&page=' + page, getPostOptions(payload)).then(handleErrors);

                                let resultOk = true;
                                getPage(page)
                                    // the page may not exist any more (rows removed in the meantime): show the last one
                                    .then(result => result.page > result.totalPages && result.totalPages > 0 ? getPage(result.totalPages) : result)
                                    .catch(error => {
                                        setMessageBox({
                                            open: true,
                                            severity: 'error',
                                            title: 'Error',
                                            message: error
                                        });
                                        resultOk = false;
                                    })
                                    .then(result => {
                                        if (resultOk) {
                                            setBucketTableState(activeBucket.id, {page: result.page - 1});
                                            resolve({
                                                data: result.customData,
                                                page: result.page - 1,
                                                totalCount: result.total,
                                            })
                                        } else {
                                            resolve({data: []});
                                        }
                                    });
                            } catch (error) {
                                setMessageBox({
                                    open: true,
                                    severity: 'error',
                                    title: 'Error',
                                    message: error
                                });
                            }
                        })}
                    options={{
                        paging: true,
                        pageSize: pageSize,
                        initialPage: tableState.page || 0,
                        searchText: tableState.searchText || '',
                        pageSizeOptions: getPageSizeOptionsOnDialog(),
                        debounceInterval: 700,
                        maxColumnSort: 1,
                        selection: false,
                        filtering: filtering,
                        padding: 'dense',
                        search: isFeatureEnabled(FEATURE_SEARCH, state.activeView),
                        searchFieldStyle: {width: 350},
                        maxBodyHeight: getTableHeight(),
                        minBodyHeight: getTableHeight(),
                        headerStyle: {
                            position: 'sticky',
                            top: 0,
                            backgroundColor: getTableHeaderBackgroundColor(theme)
                        },
                        cellStyle: {whiteSpace: 'nowrap'},
                        rowStyle: rowData => ({
                            backgroundColor: getTableRowBackgroundColor(rowData, theme)
                        }),
                        showTitle: false,
                        columnsButton: false,   // see TableActions
                        // one button in the actions column (the width of its header: BucketDataTable.css)
                        actionsCellStyle: isFeatureEnabled(FEATURE_COMPACT_ROW_ACTIONS, state.activeView) ? {width: 40} : undefined,
                        // doubleHorizontalScroll: true
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
                        Toolbar: TableToolbar,
                        Actions: TableActions
                    }}
                    onOrderChange={(colId, ord) => {
                        let order = (colId >= 0) ? {colId, ord} : null;
                        setBucketTableState(activeBucket.id, {order: order});
                    }}
                    onFilterChange={appliedFilters => {
                        const savedFilters = {};
                        appliedFilters.forEach(filter => savedFilters[filter.column.field] = filter.value);
                        setBucketTableState(activeBucket.id, {filters: savedFilters});
                    }}
                    onColumnDragged={saveColumnsLayout}
                    onChangeColumnHidden={saveColumnsLayout}
                    actions={getActions()}
                    editable={getEditable()}
                />
                </div>
                </DataTableContext.Provider>

                <MessageBox
                    config={messageBox}
                    onClose={() => setMessageBox({...messageBox, open: false})}
                />

                <DataDetailsDialog
                    bucket={activeBucket}
                    open={detailsState.open}
                    dataRow={detailsState.dataRow}
                    tags={tags}
                    onChange={(dataRow, changed) => onCloseDataDetailsDialog(
                        dataRow, changed)}
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
            </React.StrictMode>
        );
    }
}
