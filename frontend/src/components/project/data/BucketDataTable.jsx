import React, {useContext, useState} from 'react';
import MaterialTable, {MTableToolbar} from 'material-table';
import {getPageSizeOptionsOnDialog, getTableHeaderBackgroundColor, getTableHeight, getTableRowBackgroundColor} from "../../../utils/MaterialTableHelper";
import {useTheme} from "@material-ui/core/styles";
import {getLastPageSize, setLastPageSize} from "../../../utils/ConfigurationStorage";
import {useWindowDimension} from "../../utils/UseWindowDimension";
import {Grid} from "@material-ui/core";
import ViewMenuSelector from "./ViewMenuSelector";
import AccessTreeContext from "../../../context/accessTree/AccessTreeContext";
import Typography from "@material-ui/core/Typography";


export default function BucketDataTable() {

    const theme = useTheme();
    const [pageSize, setPageSize] = useState(getLastPageSize);
    const [height] = useWindowDimension();
    const accessTreeContext = useContext(AccessTreeContext);
    const {activeBucket} = accessTreeContext;

    const onChangeRowsPerPage = (pageSize) => {
        setPageSize(pageSize);
        setLastPageSize(pageSize);
    }

    const [columns] = useState([
        {title: 'Name', field: 'name'},
        {title: 'Surname', field: 'surname', initialEditValue: 'initial edit value'},
    ]);

    const onViewSelected = (id) => {
        console.log("Menu -> selected view id: " + id);
    }

    const [data] = useState([
        {name: 'John', surname: 'Baran', birthYear: 1987, birthCity: 63},
        {name: 'Ann', surname: 'Jonson', birthYear: 2017, birthCity: 34}
    ]);

    if (activeBucket != null)
        return (
            <div style={{paddingTop: 0, paddingLeft: 0, paddingRight: 0}}>
                <MaterialTable
                    title={activeBucket.name}
                    columns={columns}
                    data={data}
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
                                            views={[
                                                {id: 1, name: 'exmple view 1', description: 'This is example view for all data 1'},
                                                {id: 2, name: 'exmple view 2', description: 'This is example view for all data 2'},
                                                {id: 3, name: 'exmple view 3', description: 'This is example view for all data 3'}
                                            ]}
                                            selectedId={1}
                                            onChange={id => onViewSelected(id)}
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
        return <div><Typography>Welcome in Databucket :)</Typography></div>
}

// const useStyles = makeStyles((theme) => ({
//     root: {
//         paddingRight: theme.spacing(1),
//     }
// }));