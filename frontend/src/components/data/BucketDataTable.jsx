import React, {useContext, useEffect, useState} from 'react';
import MaterialTable, {MTableToolbar} from 'material-table';
import {getPageSizeOptionsOnDialog, getTableHeaderBackgroundColor, getTableHeight, getTableRowBackgroundColor} from "../../utils/MaterialTableHelper";
import {useTheme} from "@material-ui/core/styles";
import {getLastActiveView, getLastPageSize, setLastActiveView, setLastPageSize} from "../../utils/ConfigurationStorage";
import {useWindowDimension} from "../utils/UseWindowDimension";
import {Grid} from "@material-ui/core";
import ViewMenuSelector from "./ViewMenuSelector";
import AccessContext from "../../context/access/AccessContext";
import MissingBucketTable from "./MissingBucketTable";


export default function BucketDataTable() {

    const theme = useTheme();
    const [pageSize, setPageSize] = useState(getLastPageSize);
    const [height] = useWindowDimension();
    const accessContext = useContext(AccessContext);
    const {activeBucket, views} = accessContext;
    const [state, setState] = useState({views: [], activeView: null, columns: [], data: []});

    useEffect(() => {
        if (views != null && activeBucket != null) {
            // console.log("Active bucket: id=" + activeBucket.id + " classId=" + activeBucket.classId);
            // console.log(activeBucket);
            // console.log('Views:');
            // console.log(views);

            const availableViews = views.filter(view => (
                (view.classesIds != null && view.classesIds.includes(activeBucket.classId))
                ||
                (view.bucketsIds != null && view.bucketsIds.includes(activeBucket.id)))
            ).sort((a, b) => {
                return a.name > b.name ? 1 : -1
            });

            // console.log('Available views:');
            // console.log(availableViews);
            let activeView = null;

            if (availableViews.length > 0) {
                const lastActiveViewId = getLastActiveView(activeBucket.id);
                if (lastActiveViewId != null && availableViews.find(view => view.id === lastActiveViewId))
                    activeView = availableViews.filter(view => view.id === lastActiveViewId)[0];
                else
                    activeView = availableViews[0];
            }

            setState(state => ({...state, views: availableViews, activeView: activeView}));
        }
    }, [activeBucket, views]);

    const onChangeRowsPerPage = (pageSize) => {
        setPageSize(pageSize);
        setLastPageSize(pageSize);
    }

    const onViewSelected = (view) => {
        setLastActiveView(activeBucket.id, view.id);
        setState({...state, activeView: view});
    }

    if (activeBucket != null)
        return (
            <div style={{paddingTop: 0, paddingLeft: 0, paddingRight: 0}}>
                <MaterialTable
                    columns={state.columns}
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
                        search: (state.activeView != null && state.activeView['enabledSearching']),
                        searchFieldStyle: {width: 600},
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
                                    <Grid item xs={6}>
                                        <ViewMenuSelector
                                            views={state.views}
                                            activeView={state.activeView}
                                            onChange={view => onViewSelected(view)}
                                        />
                                    </Grid>
                                    <Grid item xs={6}>
                                        <MTableToolbar {...propsCopy} />
                                    </Grid>
                                </Grid>
                            );
                        }
                    }}
                    onChangeRowsPerPage={onChangeRowsPerPage}
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
    else
        return <MissingBucketTable />
}

// const useStyles = makeStyles((theme) => ({
//     root: {
//         paddingRight: theme.spacing(1),
//     }
// }));