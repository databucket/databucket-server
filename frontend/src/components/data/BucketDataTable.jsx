import React, {createRef, useContext, useEffect, useState} from 'react';
import MaterialTable, {MTableToolbar} from 'material-table';
import {getPageSizeOptionsOnDialog, getTableHeaderBackgroundColor, getTableHeight, getTableRowBackgroundColor} from "../../utils/MaterialTableHelper";
import {useTheme} from "@material-ui/core/styles";
import {getLastActiveView, getLastPageSize, setLastActiveView, setLastPageSize} from "../../utils/ConfigurationStorage";
import {useWindowDimension} from "../utils/UseWindowDimension";
import {Grid} from "@material-ui/core";
import ViewMenuSelector from "./ViewMenuSelector";
import AccessContext from "../../context/access/AccessContext";
import MissingBucketTable from "./MissingBucketTable";
import {isFeatureEnabled, FEATURE_SEARCH, FEATURE_MODIFYING, FEATURE_DETAILS, FEATURE_HISTORY, FEATURE_CREATION, FEATURE_REMOVAL} from "../utils/ViewFeatures";
import prepareViewColumns, {getActiveView, getBucketTags, getBucketViews} from "./BucketDataTableHelper";
import MissingActiveView from "./MissingActiveView";
import EnumsContext from "../../context/enums/EnumsContext";
import Refresh from "@material-ui/icons/Refresh";
import FilterList from "@material-ui/icons/FilterList";
import Edit from "@material-ui/icons/Edit";
import RateReviewOutlined from "@material-ui/icons/RateReviewOutlined";
import History from "@material-ui/icons/History";

const sampleData = [
    {id: 1, reserved: true, owner: 'kslysz', prop_color: 'black', prop_status:'ok'},
    {id: 2},
    {id: 3},
    {id: 4, reserved: true, owner: 'kslysz and this will be', prop_color: 'white', prop_status:'ok'},
    {id: 5, reserved: true, owner: 'kslysz', prop_color: 'red', prop_status:'ko'},
    {id: 6},
    {id: 7},
    {id: 8},
    {id: 9},
];

export default function BucketDataTable() {

    const theme = useTheme();
    const tableRef = createRef();
    const [pageSize, setPageSize] = useState(getLastPageSize);
    const [height] = useWindowDimension();
    const [filtering, setFiltering] = useState(false);
    const accessContext = useContext(AccessContext);
    const {buckets, activeBucket, views, columns, tags} = accessContext;
    const enumsContext = useContext(EnumsContext);
    const {enums} = enumsContext;
    const [state, setState] = useState({bucketViews: [], bucketTags: [], activeView: null, tableColumns: [], data: sampleData});

    useEffect(() => {
        const bucketViews = getBucketViews(activeBucket, views);
        if (bucketViews.length > 0) {
            const bucketTags = getBucketTags(activeBucket, tags);
            const lastActiveViewId = activeBucket != null ? getLastActiveView(activeBucket.id) : null;
            const activeView = getActiveView(bucketViews, lastActiveViewId);
            const columnsDef = columns.filter(col => col.id === activeView.columnsId)[0];
            const tableColumns = prepareViewColumns(columnsDef, bucketTags, enums);

            setState({
                bucketViews: bucketViews,
                bucketTags: bucketTags,
                activeView: activeView,
                tableColumns: tableColumns,
                data: sampleData
            });
        } else
            setState({bucketViews: [], bucketTags: [], activeView: null, tableColumns: [], data: sampleData});

    }, [activeBucket, enums, tags, views, columns]);

    const onViewSelected = (view) => {
        setLastActiveView(activeBucket.id, view.id);
        const columnsDef = columns.filter(col => col.id === view.columnsId)[0];
        const tableColumns = prepareViewColumns(columnsDef, state.bucketTags, enums);
        setState({
            ...state,
            activeView: view,
            tableColumns: tableColumns
        });
    }

    const onChangeRowsPerPage = (pageSize) => {
        setPageSize(pageSize);
        setLastPageSize(pageSize);
    }

    const showTaskExecutionEditorDialog = () => {
        console.log('showTaskExecutionEditorDialog:');
    }

    const showDataDetailsDialog = (rowData) => {
        console.log('showDataDetailsDialog:');
        console.log(rowData);
    }

    const showDataHistoryDialog = (rowData) => {
        console.log('showDataHistoryDialog:');
        console.log(rowData);
    }

    const getActions = (rowData) => {
        let actions = [];
        actions.push({
            icon: () => <Refresh/>,
            tooltip: 'Refresh',
            isFreeAction: true,
            onClick: () => {
                tableRef.current !== null && tableRef.current.onQueryChange()
            }
        });

        actions.push({
            icon: () => <FilterList/>,
            tooltip: 'Enable/disable filter',
            isFreeAction: true,
            onClick: () => setFiltering(!filtering)
        });

        if (isFeatureEnabled(FEATURE_MODIFYING, state.activeView))
            actions.push({
                icon: () => <Edit/>,
                tooltip: 'Remove/Modify',
                isFreeAction: true,
                onClick: () => {
                    showTaskExecutionEditorDialog()
                }
            });

        if (isFeatureEnabled(FEATURE_DETAILS, state.activeView))
            actions.push({
                icon: () => <RateReviewOutlined/>,
                tooltip: 'Data details',
                onClick: (event, rowData) => {
                    showDataDetailsDialog(rowData);
                }
            });

        if (isFeatureEnabled(FEATURE_HISTORY, state.activeView))
            actions.push(rowData => ({
                icon: () => <History/>,
                tooltip: 'Data history',
                onClick: (event, rowData) => {
                    showDataHistoryDialog(rowData);
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
                        setState({...state, data: [...state.data, newData]});
                        resolve();
                    })
            };

        if (isFeatureEnabled(FEATURE_MODIFYING, state.activeView))
            editable = {
                ...editable,
                onRowUpdate: (newData, oldData) =>
                    new Promise((resolve, reject) => {
                        resolve();
                    }),
            };

        if (isFeatureEnabled(FEATURE_REMOVAL, state.activeView))
            editable = {
                ...editable,
                onRowDelete: oldData =>
                    new Promise((resolve, reject) => {
                        setState({...state, data: state.data.filter(row => row.id !== oldData.id)});
                        resolve();
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
                    columns={state.tableColumns}
                    data={state.data}
                    options={{
                        paging: true,
                        pageSize: pageSize,
                        paginationType: 'stepped',
                        pageSizeOptions: getPageSizeOptionsOnDialog(),
                        actionsColumnIndex: -1,
                        sorting: true,
                        selection: false,
                        filtering: false,
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
                                    <Grid item xs={3}>
                                        <ViewMenuSelector
                                            views={state.bucketViews}
                                            activeView={state.activeView}
                                            onChange={view => onViewSelected(view)}
                                        />
                                    </Grid>
                                    <Grid item xs={9}>
                                        <MTableToolbar {...propsCopy} />
                                    </Grid>
                                </Grid>
                            );
                        }
                    }}
                    onChangeRowsPerPage={onChangeRowsPerPage}
                    actions={getActions()}
                    editable={getEditable()}
                />


                {/*<DataDetailsDialog*/}
                {/*    dataRow={this.state.dataRow}*/}
                {/*    open={this.state.openDataDetailsDialog}*/}
                {/*    onChange={(dataRow, changed) => this.onCloseDataDetailsDialog(dataRow, changed)}*/}
                {/*/>*/}

                {/*<DataHistoryDialog*/}
                {/*    bucket={this.state.bucket}*/}
                {/*    dataRowId={this.state.dataRowId}*/}
                {/*    history={this.state.history}*/}
                {/*    tags={this.state.tags}*/}
                {/*    open={this.state.openDataHistoryDialog}*/}
                {/*    onClose={() => this.onCloseDataHistoryDialog()}*/}
                {/*/>*/}
            </div>
        );
}

// const useStyles = makeStyles((theme) => ({
//     root: {
//         paddingRight: theme.spacing(1),
//     }
// }));