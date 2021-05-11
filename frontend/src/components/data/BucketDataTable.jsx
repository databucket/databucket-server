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
    FEATURE_TASKS,
    FEATURE_EXPORT
} from "../utils/ViewFeatures";
import prepareViewColumns, {
    convertDataBeforeAdd,
    convertDataBeforeModify,
    getActiveView,
    getBucketTags,
    getBucketViews,
    getColumnSource,
    getFetchColumns
} from "./BucketDataTableHelper";
import MissingActiveView from "./MissingActiveView";
import EnumsContext from "../../context/enums/EnumsContext";
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

export default function BucketDataTable() {

    const theme = useTheme();
    const tableRef = createRef();
    const [pageSize, setPageSize] = useState(getLastPageSize);
    const [messageBox, setMessageBox] = useState({open: false, severity: 'error', title: '', message: ''});
    const [height] = useWindowDimension();
    const [filtering, setFiltering] = useState(false);
    const accessContext = useContext(AccessContext);
    const {buckets, activeBucket, views, columns, filters, tags} = accessContext;
    const enumsContext = useContext(EnumsContext);
    const {enums} = enumsContext;
    const [state, setState] = useState(
        {
            bucketViews: [],    // all views available for active bucket that user has access
            bucketTags: [],
            activeView: null,
            columnsDef: [],     // pure columns definition
            filterDef: null,    // pure filter definition
            tableColumns: [],   // columns prepared for material table
            isDataDetailsDialogOpened: false,
            isHistoryDialogOpened: false,
            isTaskExecutionDialogOpened: false,
            dataRow: null,
            dataRowId: 0,
            history: []
        }
    );

    useEffect(() => {
        const bucketViews = getBucketViews(activeBucket, views);
        if (bucketViews.length > 0 && tags != null && enums != null && views != null && columns != null) {
            const bucketTags = getBucketTags(activeBucket, tags);
            const lastActiveViewId = activeBucket != null ? getLastActiveView(activeBucket.id) : null;
            const activeView = getActiveView(bucketViews, lastActiveViewId);
            const columnsDef = columns.filter(c => c.id === activeView.columnsId)[0];
            const tableColumns = prepareViewColumns(columnsDef, bucketTags, enums);
            const filterDef = filters.filter(f => f.id === activeView.filterId)[0];
            setState({
                ...state,
                bucketViews: bucketViews,
                bucketTags: bucketTags,
                activeView: activeView,
                columnsDef: columnsDef,
                filterDef: filterDef,
                tableColumns: tableColumns
            });
        } else
            setState({
                ...state,
                bucketViews: [],
                bucketTags: [],
                activeView: null,
                columnsDef: [],
                filterDef: null,
                tableColumns: [],
                isDataDetailsDialogOpened: false,
                isHistoryDialogOpened: false,
                isTaskExecutionDialogOpened: false,
                dataRow: null
            });

    }, [activeBucket, enums, tags, views, columns, filters]);

    const onViewSelected = (view) => {
        setLastActiveView(activeBucket.id, view.id);
        const columnsDef = columns.filter(col => col.id === view.columnsId)[0];
        const filterDef = filters.filter(f => f.id === view.filterId)[0];
        const tableColumns = prepareViewColumns(columnsDef, state.bucketTags, enums);
        setState({
            ...state,
            activeView: view,
            columnsDef: columnsDef,
            filterDef: filterDef,
            tableColumns: tableColumns
        });
    }

    const showTaskExecutionEditorDialog = () => {
        console.log('showTaskExecutionEditorDialog:');
    }

    const onOpenDataDetailsDialog = (rowData) => {
        let resultOk = true;
        fetch(getDataByIdUrl(activeBucket, rowData.Id), getGetOptions())
            .then(handleErrors)
            .catch(error => {
                setMessageBox({open: true, severity: 'error', title: 'Error', message: error});
                resultOk = false;
            })
            .then(result => {
                if (resultOk) {
                    setState({
                        ...state,
                        dataRow: result,
                        isDataDetailsDialogOpened: true
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
                    resultOk && refresh();
                });
        }
        setState({...state, isDataDetailsDialogOpened: false});
    }

    const onOpenDataHistoryDialog = (rowData) => {
        let resultOk = true;
        fetch(getDataHistoryUrl(activeBucket, rowData.Id), getGetOptions())
            .then(handleErrors)
            .catch(error => {
                setMessageBox({open: true, severity: 'error', title: 'Error', message: error});
                resultOk = false;
            })
            .then(result => {
                if (resultOk) {
                    setState({
                        ...state,
                        dataRowId: rowData.Id,
                        history: result,
                        isHistoryDialogOpened: true
                    });
                }
            });
    }

    const onCloseDataHistoryDialog = () => {
        setState({...state, isHistoryDialogOpened: false});
    }

    const onDataReserve = ({random, number, username}) => {
        let payload = {conditions: null};
        if (username !== getUsername()) {
            console.log(username);
            payload.targetOwnerUsername = username;
        }

        let resultOk = true;
        fetch(getDataReserveUrl(activeBucket, number, random), getPostOptions(payload))
            .then(handleErrors)
            .catch(error => {
                setMessageBox({open: true, severity: 'error', title: 'Error', message: error});
                resultOk = false;
            })
            .then(() => {
                if (resultOk)
                    refresh();
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
                        allConditions.push({left_source: leftSource, left_value: filter.column.source, operator: 'in', right_source: 'const', right_value: filter.value});
                    else if (filter.column.type === 'boolean')
                        allConditions.push({left_source: leftSource, left_value: filter.column.source, operator: '=', right_source: 'const', right_value: (filter.value === 'checked')});
                    else if (Array.isArray(filter.value))
                        allConditions.push({left_source: leftSource, left_value: filter.column.source, operator: 'in', right_source: 'const', right_value: filter.value});
                    else
                        allConditions.push({left_source: leftSource, left_value: filter.column.source, operator: 'like', right_source: 'const', right_value: '%' + filter.value + '%'});

                } else {
                    if (filter.column.type === 'numeric')
                        if (Array.isArray(filter.value)) {
                            if (filter.value.length > 0)
                                allConditions.push({left_source: 'field', left_value: filter.column.source, operator: 'in', right_source: 'const', right_value: filter.value});
                        } else
                            allConditions.push({left_source: 'field', left_value: filter.column.source, operator: '=', right_source: 'const', right_value: filter.value});
                    else if (filter.column.type === 'boolean')
                        allConditions.push({left_source: 'field', left_value: filter.column.source, operator: '=', right_source: 'const', right_value: (filter.value === 'checked')});
                    else {
                        const filterValue = (filter.value === '@currentUser') ? getUsername() : '%' + filter.value + '%';
                        allConditions.push({left_source: 'field', left_value: filter.column.source, operator: 'like', right_source: 'const', right_value: filterValue});
                    }
                }
            }
        }

        // TODO: convert new filter into old structure
        // if (state.view.filter_id !== null) {
        //     let viewFiltersArray = this.state.filters.filter(d => (d.filter_id === this.state.view.filter_id));
        //     let viewFilters = viewFiltersArray[0];
        //     let viewFiltersString = JSON.stringify(viewFilters.conditions).replace('@currentUser', window.USER);
        //     allConditions = allConditions.concat(JSON.parse(viewFiltersString));
        // }

        return allConditions;
    }

    const refresh = () => {
        console.log('Refresh');
        tableRef.current !== null && tableRef.current.onQueryChange();
    }

    const getActions = (rowData) => {
        let actions = [];

        // if (isFeatureEnabled(FEATURE_RESERVATION, state.activeView))
        //     actions.push({
        //         icon: () => <span className="material-icons">add_task</span>,
        //         tooltip: 'Reserve data',
        //         isFreeAction: true,
        //         onClick: () => {
        //             handleOpenReserveDataDialog()
        //         }
        //     });

        if (isFeatureEnabled(FEATURE_TASKS, state.activeView))
            actions.push({
                icon: () => <span className="material-icons">edit_note</span>,
                tooltip: 'Task execution',
                isFreeAction: true,
                onClick: () => {
                    showTaskExecutionEditorDialog()
                }
            });

        actions.push({
            icon: () => <FilterList/>,
            tooltip: 'Enable/disable filter',
            isFreeAction: true,
            onClick: () => setFiltering(!filtering)
        });

        actions.push({
            icon: () => <Refresh/>,
            tooltip: 'Refresh',
            isFreeAction: true,
            onClick: () => {
                refresh();
            }
        });

        if (isFeatureEnabled(FEATURE_DETAILS, state.activeView))
            actions.push({
                icon: () => <RateReviewOutlined/>,
                tooltip: 'Data details',
                onClick: (event, rowData) => {
                    onOpenDataDetailsDialog(rowData);
                }
            });

        if (isFeatureEnabled(FEATURE_HISTORY, state.activeView))
            actions.push(rowData => ({
                icon: () => <History/>,
                tooltip: 'Data history',
                onClick: (event, rowData) => {
                    onOpenDataHistoryDialog(rowData);
                }
            }));

        return actions;
    }

    const getEditable = () => {
        let editable = {};

        if (isFeatureEnabled(FEATURE_CREATION, state.activeView))
            editable = {
                ...editable,
                onRowAdd: newData =>
                    new Promise((resolve, reject) => {
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
                    })
            };

        if (isFeatureEnabled(FEATURE_MODIFYING, state.activeView))
            editable = {
                ...editable,
                onRowUpdate: (newData, oldData) =>
                    new Promise((resolve, reject) => {
                        let payload = convertDataBeforeModify(state.tableColumns, newData, oldData);
                        let resultOk = true;
                        if (Object.keys(payload).length > 0) {
                            fetch(getDataByIdUrl(activeBucket, newData.Id), getPutOptions(payload))
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

                    })
            };

        if (isFeatureEnabled(FEATURE_REMOVAL, state.activeView))
            editable = {
                ...editable,
                onRowDelete: oldData =>
                    new Promise((resolve, reject) => {
                        let resultOk = true;
                        fetch(getDataByIdUrl(activeBucket, oldData.Id), getDeleteOptions())
                            .then(handleErrors)
                            .catch(error => {
                                setMessageBox({open: true, severity: 'error', title: 'Error', message: error});
                                resultOk = false;
                            })
                            .then(() => {
                                resultOk ? resolve() : reject();
                            });
                    })
            };

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
                                    conditions: consolidateAllConditions(query.search, query.filters)
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
                                                data: result.data,
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
                    open={state.isDataDetailsDialogOpened}
                    dataRow={state.dataRow}
                    tags={tags}
                    onChange={(dataRow, changed) => onCloseDataDetailsDialog(dataRow, changed)}
                />

                <DataHistoryDialog
                    bucket={activeBucket}
                    dataRowId={state.dataRowId}
                    history={state.history}
                    tags={state.tags}
                    open={state.isHistoryDialogOpened}
                    onClose={() => onCloseDataHistoryDialog()}
                />
            </div>
        );
}