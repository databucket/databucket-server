import React, { forwardRef } from 'react';
import MaterialTable from 'material-table';
import AddBox from '@material-ui/icons/AddBox';
import ArrowUpward from '@material-ui/icons/ArrowUpward';
import Check from '@material-ui/icons/Check';
import ChevronLeft from '@material-ui/icons/ChevronLeft';
import ChevronRight from '@material-ui/icons/ChevronRight';
import Clear from '@material-ui/icons/Clear';
import DeleteOutline from '@material-ui/icons/DeleteOutline';
import Delete from '@material-ui/icons/Delete';
import Edit from '@material-ui/icons/Edit';
import FilterList from '@material-ui/icons/FilterList';
import FirstPage from '@material-ui/icons/FirstPage';
import LastPage from '@material-ui/icons/LastPage';
import Remove from '@material-ui/icons/Remove';
import Refresh from '@material-ui/icons/Refresh';
import SaveAlt from '@material-ui/icons/SaveAlt';
import Search from '@material-ui/icons/Search';
import ViewColumn from '@material-ui/icons/ViewColumn';
import Cookies from 'universal-cookie';

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

class EventsLogTab extends React.Component {

    constructor(props) {
        super(props);
        this.tableRef = React.createRef();
        this.pageSize = this.getLastPageSize();

        this.state = {
            columns: [
                { title: 'Id', field: 'event_log_id', type: 'numeric', editable: 'never', filtering: false, defaultSort: 'desc'},
                { title: 'Bucket', field: 'bucket_id', lookup: props.bucketsLookup, emptyValue: '- removed -'},
                { title: 'Event', field: 'event_id', lookup: props.eventsLookup, emptyValue: '- removed -'},
                { title: 'Task', field: 'task_id', lookup: props.tasksLookup, emptyValue: '- removed -'},                
                { title: 'Affected', field: 'affected', editable: 'never'},
                {
                    title: 'At', field: 'created_at', type: 'datetime', editable: 'never', filtering: false,
                    render: rowData => <div>{rowData != null ? rowData.created_at != null ? new Date(rowData.created_at).toLocaleString() : null : null}</div>,
                }
            ],
            filtering: false
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

    clearLog() {
        new Promise((resolve, reject) => {
            let url = window.API + '/events/log';
            fetch(url, { method: 'DELETE' })
                .catch(error => {
                    window.alert(error.message);
                    reject();
                })
                .then(this.tableRef.current !== null && this.tableRef.current.onQueryChange())
                .then(resolve());
        });
    }

    buildFilter(url, filters) {
        let filterUrl = '&filter=';
        let addFilter = false;
        for (const filter of filters)
            if (filter.value.length > 0) {
                addFilter = true;
                if (filter.column.field === 'bucket_id')
                    filterUrl += '(' + filter.column.field + ';in;numeric_array;' + filter.value + ')';
                else if (filter.column.type === 'numeric')
                    filterUrl += '(' + filter.column.field + ';' + filter.operator + ';numeric;' + filter.value + ')';
                else if (filter.column.type === 'boolean')
                    filterUrl += '(' + filter.column.field + ';=;boolean;' + (filter.value === 'checked') + ')';
                else {
                    if (Array.isArray(filter.value)) {
                        filterUrl += '(' + filter.column.field + ';in;numeric_array;' + filter.value + ')';
                    } else
                        filterUrl += escape('(' + filter.column.field + ';like;text;%' + filter.value + '%)');
                }
                    
            }
        if (addFilter)
            return url + filterUrl;
        else 
            return url;
    }

    render() {
        return (
            <MaterialTable
                icons={tableIcons}
                title='Events log'
                tableRef={this.tableRef}
                columns={this.state.columns}
                data={query =>
                    new Promise((resolve, reject) => {
                        if (this.pageSize !== query.pageSize) {
                            this.pageSize = query.pageSize;
                            this.setLastPageSize(query.pageSize);
                        } 

                        let url = window.API + '/events/log?';
                        url += 'limit=' + query.pageSize;
                        url += '&page=' + (query.page + 1);
                        if (query.orderBy != null && query.orderBy.field != null)
                            if (query.orderDirection === 'desc')
                                url += '&sort=desc(' + query.orderBy.field + ')';
                            else
                                url += '&sort=' + query.orderBy.field;

                        if (this.state.filtering && query.filters.length > 0)
                            url = this.buildFilter(url, query.filters);

                        fetch(url)
                            .then(response => response.json())
                            .then(result => {
                                resolve({
                                    data: result.events_log,
                                    page: result.page - 1,
                                    totalCount: result.total,
                                })
                            });
                    })
                }
                options={{
                    pageSize: this.pageSize,
                    pageSizeOptions: [15, 20, 25, 30, 35, 40, 45, 50],
                    // paginationType: 'stepped',
                    actionsColumnIndex: -1,
                    sorting: true,
                    search: false,
                    filtering: this.state.filtering,
                    debounceInterval: 700,
                    padding: 'dense',
                    headerStyle:{backgroundColor:'#eeeeee'},
                    rowStyle: rowData => ({ backgroundColor: rowData.tableData.id % 2 === 1 ? '#fafafa' : '#FFF' })
                }}
                components={{
                    Container: props => <div {...props} />
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
                                    this.tableRef.current.onQueryChange()
                            }
                        }
                    },
                    {
                        icon: () => <Delete />,
                        tooltip: 'Clear',
                        isFreeAction: true,
                        onClick: () => { this.clearLog() }
                    },
                ]}
            />
        );
    }
}

export default EventsLogTab;