import React, { forwardRef } from 'react';
import MaterialTable from 'material-table';
import AddBox from '@material-ui/icons/AddBox';
import ArrowUpward from '@material-ui/icons/ArrowUpward';
import Check from '@material-ui/icons/Check';
import ChevronLeft from '@material-ui/icons/ChevronLeft';
import ChevronRight from '@material-ui/icons/ChevronRight';
import Clear from '@material-ui/icons/Clear';
import History from '@material-ui/icons/History';
import DeleteOutline from '@material-ui/icons/DeleteOutline';
import Edit from '@material-ui/icons/Edit';
import FilterList from '@material-ui/icons/FilterList';
import FirstPage from '@material-ui/icons/FirstPage';
import LastPage from '@material-ui/icons/LastPage';
import Remove from '@material-ui/icons/Remove';
import SaveAlt from '@material-ui/icons/SaveAlt';
import Search from '@material-ui/icons/Search';
import ViewColumn from '@material-ui/icons/ViewColumn';
import RateReviewOutlined from '@material-ui/icons/RateReviewOutlined';
import Refresh from '@material-ui/icons/Refresh';
import Cookies from 'universal-cookie';
import DataDetailsDialog from './_deprecated_/DataDetailsDialog';
import DataHistoryDialog from './_deprecated_/DataHistoryDialog';
import TaskExecutionEditorDialog from './_deprecated_/TaskExecutionEditorDialog';
// import ImportIcon from '@material-ui/icons/ExitToApp';
// import ExportIcon from '@material-ui/icons/OpenInNew'

const cookies = new Cookies();

const tableIcons = {
    Add: forwardRef((props, ref) => <AddBox {...props} ref={ref} />),
    Check: forwardRef((props, ref) => <Check {...props} ref={ref} />),
    Clear: forwardRef((props, ref) => <Clear {...props} ref={ref} />),
    Delete: forwardRef((props, ref) => <DeleteOutline {...props} ref={ref} />),
    DetailPanel: forwardRef((props, ref) => <ChevronRight {...props} ref={ref} />),
    Edit: forwardRef((props, ref) => <Edit {...props} ref={ref} />),
    Export: forwardRef((props, ref) => <SaveAlt {...props} ref={ref} />),
    Filter: forwardRef((props, ref) => <FilterList {...props} ref={ref} />),
    FirstPage: forwardRef((props, ref) => <FirstPage {...props} ref={ref} />),
    LastPage: forwardRef((props, ref) => <LastPage {...props} ref={ref} />),
    NextPage: forwardRef((props, ref) => <ChevronRight {...props} ref={ref} />),
    PreviousPage: forwardRef((props, ref) => <ChevronLeft {...props} ref={ref} />),
    ResetSearch: forwardRef((props, ref) => <Clear {...props} ref={ref} />),
    Search: forwardRef((props, ref) => <Search {...props} ref={ref} />),
    SortArrow: forwardRef((props, ref) => <ArrowUpward {...props} ref={ref} />),
    ThirdStateCheck: forwardRef((props, ref) => <Remove {...props} ref={ref} />),
    ViewColumn: forwardRef((props, ref) => <ViewColumn {...props} ref={ref} />),
};

function handleErrors(res) {
    if (res.ok) {
        return res.json();
    } else {
        return res.json().then(err => { throw err; });
    }
}

class DatabucketTable extends React.Component {

    constructor(props) {
        super(props);
        this.allowRender = false;
        this.tableRef = React.createRef();
        this.pageSize = this.getLastPageSize();
        this.state = {
            dataRow: null,
            history: null,
            openDataDetailsDialog: false,
            openDataHistoryDialog: false,
            openTaskExecutionEditorDialog: false,
            filtering: false,
            bucket: props.selected.bucket,
            view: props.selected.view,
            filters: props.selected.filters,
            columns: []
        };
    }

    getLastPageSize() {
        const lastPageSize = cookies.get('last_page_size');
        if (lastPageSize != null) {
            return parseInt(lastPageSize);
        } else
            return 15;
    }

    setLastPageSize(pageSize) {
        const current = new Date();
        const nextYear = new Date();
        nextYear.setFullYear(current.getFullYear() + 1);
        cookies.set('last_page_size', pageSize, { path: window.location.href, expires: nextYear });
    }

    static getDerivedStateFromProps(props, state) {
        if (props.selected != null && props.selected.view != null) {
            return {
                bucket: props.selected.bucket,
                view: props.selected.view,
                filters: props.selected.filters,
                columns: props.selected.view.columns,
                tasks: props.allTasks,
                tags: props.allTags
            };
        } else
            return {
                bucket: null,
                view: null,
                filters: null,
                columns: null,
                tasks: null,
                tags: null
            };
    }

    shouldComponentUpdate(nextProps, nextState) {
        if (nextProps.selected !== null && nextProps.view !== null && nextState.view !== null) {

            if (this.tableRef.current !== null && this.state.view !== null && nextState.view !== this.state.view) {
                // this.tableRef.current.state.orderBy = -1;
                // this.tableRef.current.state.orderDirection = "";
                // this.tableRef.current.state.query.orderBy = undefined;
                // this.tableRef.current.state.query.orderDirection = "";
                this.tableRef.current.state.query.filters = [];
                this.tableRef.current.state.query.page = 0;

                this.tableRef.current.onQueryChange();
            }

            return true;
        } else
            return false;
    }

    getTableTitle(view) {
        if (view !== null) {
            if (view.description !== null && view.description.length > 0)
                return view.description;
            else
                return view.view_name;
        } else return '';
    }

    getColumnsForRequest() {
        var reqColumns = [];
        for (var i = 0; i < this.state.view.columns.length; i++) {
            const colDef = this.state.view.columns[i];
            const colReq = { field: colDef.source, title: colDef.title }
            reqColumns.push(colReq);
        }
        return reqColumns;
    }

    getFieldType(fieldName) {
        const columns = this.state.columns.filter(column => (column.field === fieldName));
        return columns[0].type;
    }

    setJsonValueByPath(path, val, obj) {
        var fields = path.split('.');
        var result = obj;
        for (var i = 0, n = fields.length; i < n && result !== undefined; i++) {
            var field = fields[i];
            if (i === n - 1) {
                result[field] = val;
            } else {
                if (typeof result[field] === 'undefined') {
                    result[field] = {};
                }
                result = result[field];
            }
        }
    }

    toIsoString(aDate) {
        var tzo = -aDate.getTimezoneOffset(),
            dif = tzo >= 0 ? '+' : '-',
            pad = function (num) {
                var norm = Math.floor(Math.abs(num));
                return (norm < 10 ? '0' : '') + norm;
            };
        return aDate.getFullYear() +
            '-' + pad(aDate.getMonth() + 1) +
            '-' + pad(aDate.getDate()) +
            'T' + pad(aDate.getHours()) +
            ':' + pad(aDate.getMinutes()) +
            ':' + pad(aDate.getSeconds()) +
            ".000" +
            dif + pad(tzo / 60) + pad(tzo % 60);
    }

    // based on column title find and return source value (field name or property json path)
    getColumnSource(fieldName) {
        let column = this.state.columns.filter(c => (c.field === fieldName))[0];
        return column.source;
    }

    getDataRowId(data) {
        let dataIdColumn = this.state.columns.filter(c => (c.source === 'data_id'))[0];
        return data[dataIdColumn.field];
    }

    // before add data
    convertDataBeforeAdd(inputDataRow) {
        var dataRow = { properties: {} };
        var hasProperties = false;
        try {
            for (var key in inputDataRow) {
                if (inputDataRow.hasOwnProperty(key)) {
                    let source = this.getColumnSource(key);
                    const fieldType = this.getFieldType(key);
                    if (fieldType === 'numeric') {
                        dataRow[source] = parseInt(inputDataRow[key]);
                    } else if (fieldType === 'datetime' || fieldType === 'date' || fieldType === 'time') {
                        dataRow[source] = this.toIsoString(inputDataRow[key]);
                    } else {
                        dataRow[source] = inputDataRow[key];
                    }

                    if (source.startsWith('$')) {
                        hasProperties = true;
                        const path = source.substring(2);
                        this.setJsonValueByPath(path, dataRow[source], dataRow.properties);
                        delete dataRow[source];
                    }
                }
            }
        } catch (error) {
            //console.error(error);
        }

        if (!hasProperties)
            delete dataRow['properties'];

        return dataRow;
    }

    // before modify data
    convertDataBeforeModify(newData, oldDataRow) {
        const readOnlyColumns = ['data_id', 'created_at', 'created_by', 'updated_at', 'updated_by', 'locked_by'];

        var payload = { update_properties: {}, remove_properties: [] };
        var hasUpdateProperties = false;
        var hasRemoveProperties = false;

        for (var key in newData) {
            if (newData.hasOwnProperty(key)) {
                let source = this.getColumnSource(key);
                if (readOnlyColumns.indexOf(source) < 0) {
                    const newItem = newData[key];
                    const oldItem = oldDataRow[key];

                    if (newItem !== oldItem) {
                        const fieldType = this.getFieldType(key);
                        if (newItem != null) {
                            let value = newItem;
                            if (fieldType === 'numeric')
                                value = parseInt(newData[key]);
                            else if (fieldType === 'datetime' || fieldType === 'date' || fieldType === 'time')
                                value = this.toIsoString(newData[key]);

                            if (source.startsWith('$')) {
                                payload.update_properties[source] = value;
                                hasUpdateProperties = true;
                            } else
                                payload[source] = value;
                        } else {
                            // We do not want to remove properties istead of putting null value
                            // if (source.startsWith('$')) {
                            //     payload.remove_properties.push(source);
                            //     hasRemoveProperties = true;
                            // } else
                            if (source.startsWith('$')) {
                                payload.update_properties[source] = null;
                                hasUpdateProperties = true;
                            } else
                                payload[source] = null;
                        }
                    }
                }
            }
        }

        if (!hasUpdateProperties)
            delete payload['update_properties'];

        if (!hasRemoveProperties)
            delete payload['remove_properties'];

        return payload;
    }

    showTaskExecutionEditorDialog() {
        this.setState({
            openTaskExecutionEditorDialog: true,
        });
    }

    showDataDetailsDialog(dataRow) {
        new Promise((resolve, reject) => {
            const dataId = this.getDataRowId(dataRow);
            let url = window.API + '/bucket/' + this.state.bucket.bucket_name + '/data/' + dataId;

            fetch(url)
                .then(response => response.json())
                .then(result => {
                    this.setState({
                        dataRow: result.data[0],
                        openDataDetailsDialog: true,
                    });
                    resolve();
                });
        });
    }

    onCloseDataDetailsDialog(dataRow, changed) {
        if (changed) {
            new Promise((resolve, reject) => {
                const payload = {};
                payload.properties = dataRow.properties;
                let result_ok = true;

                let url = window.API + '/bucket/' + this.state.bucket.bucket_name + '/data/' + dataRow.data_id + '?userName=' + window.USER;
                fetch(url, {
                    method: 'PUT',
                    body: JSON.stringify(payload),
                    headers: {
                        'Content-Type': 'application/json'
                    }
                })
                    .then(handleErrors)
                    .catch(error => {
                        result_ok = false;
                        window.alert(error.message);
                        reject();
                    })
                    .then(() => {
                        if (result_ok) {
                            this.tableRef.current !== null && this.tableRef.current.onQueryChange();
                            resolve();
                        } else
                            reject();
                    });
            });
        }
        this.setState({ openDataDetailsDialog: false });
    }

    showDataHistoryDialog(rowData) {
        new Promise((resolve, reject) => {
            const dataRowId = this.getDataRowId(rowData);
            let url = window.API + '/bucket/' + this.state.bucket.bucket_name + '/data/' + dataRowId + '/history';

            fetch(url)
                .then(response => response.json())
                .then(result => {
                    this.setState({
                        dataRowId: dataRowId,
                        history: result.history,
                        openDataHistoryDialog: true,
                    });
                    resolve();
                });
        });
    }

    onCloseDataHistoryDialog() {
        this.setState({ openDataHistoryDialog: false });
    }

    onCloseTaskExecutionEditorDialog() {
        this.setState({ openTaskExecutionEditorDialog: false });
        if (this.tableRef.current !== null)
            this.tableRef.current.onQueryChange();
    }

    consolidateAllConditions(tableSearch, tableFilters) {
        let allConditions = [];
        if (tableSearch != null && tableSearch.length > 0)
            allConditions.push({ left_source: 'field', left_value: 'properties', operator: 'like', right_source: 'const', right_value: '%' + tableSearch + '%' });

        if (this.state.filtering && tableFilters.length > 0) {
            for (const filter of tableFilters) {
                if (filter.column.source.startsWith('$')) {
                    var leftSource = 'property';
                    if (filter.column.source.endsWith('()'))
                        leftSource = 'function'

                    if (filter.column.type === 'numeric')
                        allConditions.push({ left_source: leftSource, left_value: filter.column.source, operator: 'in', right_source: 'const', right_value: filter.value });
                    else if (filter.column.type === 'boolean')
                        allConditions.push({ left_source: leftSource, left_value: filter.column.source, operator: '=', right_source: 'const', right_value: (filter.value === 'checked') });
                    else if (Array.isArray(filter.value))
                        allConditions.push({ left_source: leftSource, left_value: filter.column.source, operator: 'in', right_source: 'const', right_value: filter.value });
                    else
                        allConditions.push({ left_source: leftSource, left_value: filter.column.source, operator: 'like', right_source: 'const', right_value: '%' + filter.value + '%' });


                } else {
                    if (filter.column.type === 'numeric')
                        if (Array.isArray(filter.value)) {
                            if (filter.value.length > 0)
                                allConditions.push({ left_source: 'field', left_value: filter.column.source, operator: 'in', right_source: 'const', right_value: filter.value });
                        } else
                            allConditions.push({ left_source: 'field', left_value: filter.column.source, operator: '=', right_source: 'const', right_value: filter.value });
                    else if (filter.column.type === 'boolean')
                        allConditions.push({ left_source: 'field', left_value: filter.column.source, operator: '=', right_source: 'const', right_value: (filter.value === 'checked') });
                    else {
                        var filterValue = '%' + filter.value + '%';
                        if (filterValue === '%@currentUser%')
                            filterValue = window.USER;
                        allConditions.push({ left_source: 'field', left_value: filter.column.source, operator: 'like', right_source: 'const', right_value: filterValue });
                    }
                }
            }
        }

        if (this.state.view.filter_id !== null) {
            let viewFiltersArray = this.state.filters.filter(d => (d.filter_id === this.state.view.filter_id));
            let viewFilters = viewFiltersArray[0];
            let viewFiltersString = JSON.stringify(viewFilters.conditions).replace('@currentUser', window.USER);
            allConditions = allConditions.concat(JSON.parse(viewFiltersString));
        }

        return allConditions;
    }

    render() {
        if (this.state.bucket !== null && this.state.view !== null) {
            return (
                <div style={{ paddingTop: 0, paddingLeft: 0, paddingRight: 0 }}>
                    <MaterialTable
                        icons={tableIcons}
                        title={this.getTableTitle(this.state.view)}
                        tableRef={this.tableRef}
                        columns={this.state.columns !== null ? this.state.columns : []}
                        data={query =>
                            new Promise((resolve, reject) => {
                                try {
                                    if (this.pageSize !== query.pageSize) {
                                        this.pageSize = query.pageSize;
                                        this.setLastPageSize(query.pageSize);
                                    }

                                    let url = window.API + '/bucket/' + this.state.bucket.bucket_name + '/data/custom?';
                                    url += 'limit=' + query.pageSize;
                                    url += '&page=' + (query.page + 1);

                                    if (query.orderBy != null) {
                                        const source = this.getColumnSource(query.orderBy.field);
                                        if (query.orderDirection === 'desc')
                                            url += '&sort=desc(' + source + ')';
                                        else
                                            url += '&sort=' + source;
                                    }

                                    let payload = {
                                        columns: this.getColumnsForRequest(),
                                        conditions: this.consolidateAllConditions(query.search, query.filters)
                                    }

                                    fetch(url, {
                                        method: 'POST',
                                        body: JSON.stringify(payload),
                                        headers: {
                                            'Content-Type': 'application/json'
                                        }
                                    })
                                        .then(response => response.json())
                                        .then(result => {
                                            resolve({
                                                data: result.data,
                                                page: result.page - 1,
                                                totalCount: result.total,
                                            })
                                        });
                                } catch (error) {
                                    console.error(error);
                                }
                            })
                        }
                        // onRowClick={((evt, selectedRow) => this.setState({ selectedRow }))}
                        options={{
                            pageSize: this.pageSize,
                            pageSizeOptions: [15, 20, 25, 30, 35, 40, 45, 50],
                            sorting: true,
                            search: true,
                            // selection: true,
                            filtering: this.state.filtering,
                            debounceInterval: 700,
                            padding: 'dense',
                            actionsColumnIndex: -1,
                            searchFieldStyle: { width: 600 },
                            headerStyle: { backgroundColor: '#eeeeee' },
                            // tableLayout: 'fixed',
                            // rowStyle: rowData => ({ backgroundColor: (this.state.selectedRow && this.state.selectedRow.tableData.id === rowData.tableData.id) ? '#EEE' : '#FFF' }),
                            rowStyle: rowData => ({ backgroundColor: rowData.tableData.id % 2 === 1 ? '#fafafa' : '#FFF' })
                        }}
                        components={{
                            Container: props => (<div {...props} />)
                        }}
                        actions={[
                            {
                                icon: () => <Refresh />,
                                tooltip: 'Refresh',
                                isFreeAction: true,
                                onClick: () => { this.tableRef.current !== null && this.tableRef.current.onQueryChange() }
                            },
                            {
                                icon: () => <FilterList />,
                                tooltip: 'Enable/disable filter',
                                isFreeAction: true,
                                onClick: () => {
                                    this.setState({ filtering: !this.state.filtering });

                                    // after switch filtering off/on
                                    if (this.tableRef.current.state.query.filters.length > 0) {
                                        if (this.tableRef.current)
                                            this.tableRef.current.onQueryChange();
                                    }
                                }
                            },
                            {
                                icon: () => <Edit />,
                                tooltip: 'Remove/Modify',
                                isFreeAction: true,
                                onClick: () => {
                                    this.showTaskExecutionEditorDialog();
                                }
                            },
                            // {
                            //     icon: () => <ImportIcon />,
                            //     tooltip: 'Import',
                            //     isFreeAction: true,
                            //     onClick: () => { window.alert("Import") }
                            // },
                            // {
                            //     icon: () => <ExportIcon />,
                            //     tooltip: 'Export',
                            //     isFreeAction: true,
                            //     onClick: () => { window.alert("Export") }
                            // },
                            rowData => ({
                                icon: () => <RateReviewOutlined />,
                                tooltip: 'Data details',
                                onClick: (event, rowData) => {
                                    this.showDataDetailsDialog(rowData);
                                }
                            }),
                            rowData => ({
                                icon: () => <History />,
                                tooltip: 'Data history',
                                onClick: (event, rowData) => {
                                    this.showDataHistoryDialog(rowData);
                                }
                            })
                        ]}
                        localization={{
                            body: {
                                addTooltip: 'Add'
                            },
                            toolbar: {
                                searchTooltip: 'Search properties',
                                searchPlaceholder: 'Search properties'
                            }
                        }}
                        editable={{
                            onRowAdd: newData =>
                                new Promise((resolve, reject) => {
                                    const payload = this.convertDataBeforeAdd(newData);

                                    payload.created_by = window.USER;

                                    let url = window.API + '/bucket/' + this.state.bucket.bucket_name + '/data?userName=' + window.USER;

                                    let result_ok = true;
                                    fetch(url, {
                                        method: 'POST',
                                        body: JSON.stringify(payload),
                                        headers: {
                                            'Content-Type': 'application/json'
                                        }
                                    })
                                        .then(handleErrors)
                                        .catch(error => {
                                            result_ok = false;
                                            window.alert(error.message);
                                            reject();
                                        })
                                        .then(() => {
                                            result_ok === true ? resolve() : reject();
                                        });
                                }),
                            onRowUpdate: (newData, oldData) =>
                                new Promise((resolve, reject) => {
                                    // console.log(JSON.parse(JSON.stringify(newData)));
                                    var payload = this.convertDataBeforeModify(newData, oldData);
                                    let changed = false;
                                    // console.log(JSON.parse(JSON.stringify(newData)));
                                    // console.log(payload);
                                    if (Object.keys(payload).length > 0)
                                        changed = true;

                                    let result_ok = true;
                                    if (changed) {
                                        const dataRowId = this.getDataRowId(newData);
                                        let url = window.API + '/bucket/' + this.state.bucket.bucket_name + '/data/' + dataRowId + '?userName=' + window.USER;
                                        fetch(url, {
                                            method: 'PUT',
                                            body: JSON.stringify(payload),
                                            headers: {
                                                'Content-Type': 'application/json'
                                            }
                                        })
                                            .then(handleErrors)
                                            .catch(error => {
                                                result_ok = false;
                                                window.alert(error.message);
                                                reject();
                                            })
                                            .then(() => {
                                                result_ok === true ? resolve() : reject();
                                            });
                                    } else
                                        reject();
                                }),
                            onRowDelete: oldData =>
                                new Promise((resolve, reject) => {
                                    const dataRowId = this.getDataRowId(oldData);
                                    let url = window.API + '/bucket/' + this.state.bucket.bucket_name + '/data/' + dataRowId;
                                    fetch(url, { method: 'DELETE' })
                                        .then(handleErrors)
                                        .catch(error => {
                                            window.alert(error.message);
                                            reject();
                                        })
                                        .then(resolve());
                                }),
                        }}
                    />
                    <DataDetailsDialog
                        dataRow={this.state.dataRow}
                        open={this.state.openDataDetailsDialog}
                        onChange={(dataRow, changed) => this.onCloseDataDetailsDialog(dataRow, changed)} />

                    <DataHistoryDialog
                        bucket={this.state.bucket}
                        dataRowId={this.state.dataRowId}
                        history={this.state.history}
                        tags={this.state.tags}
                        open={this.state.openDataHistoryDialog}
                        onClose={() => this.onCloseDataHistoryDialog()} />

                    <TaskExecutionEditorDialog
                        open={this.state.openTaskExecutionEditorDialog}
                        bucket={this.state.bucket}
                        tags={this.state.tags}
                        tasks={this.state.tasks}
                        onClose={() => this.onCloseTaskExecutionEditorDialog()} />
                </div>)
        } else
            return (<div />);
    }
}

export default DatabucketTable;