import React from 'react';
import MaterialTable from 'material-table';
import FilterList from '@material-ui/icons/FilterList';
import Refresh from '@material-ui/icons/Refresh';
import Cookies from 'universal-cookie';

function handleErrors(res) {
    if (res.ok) {
        return res.json();
    } else {
        return res.json().then(err => { throw err; });
    }
}

class ClassesTab extends React.Component {

    constructor(props) {
        super(props);
        this.tableRef = React.createRef();
        this.pageSize = this.getLastPageSize();
        this.state = {
            bucketsLookup: props.bucketsLookup,
            columns: [
                // { title: 'Id', field: 'class_id', type: 'numeric', editable: 'never', filtering: false },
                { title: 'Name', field: 'class_name' },
                { title: 'Description', field: 'description' },
                {
                    title: 'Created at', field: 'created_at', type: 'datetime', editable: 'never', filtering: false,
                    render: rowData => <div>{rowData != null ? rowData.created_at != null ? new Date(rowData.created_at).toLocaleString() : null : null}</div>,
                },
                { title: 'Created by', field: 'created_by', editable: 'never' },
                {
                    title: 'Updated at', field: 'updated_at', type: 'datetime', editable: 'never', filtering: false,
                    render: rowData => <div>{rowData != null ? rowData.updated_at != null ? new Date(rowData.updated_at).toLocaleString() : null : null}</div>,
                },
                { title: 'Updated by', field: 'updated_by', editable: 'never' },
            ],
            filtering: false
        };
    }

    convertNullsToEmpty(inputData) {
        for (var i = 0; i < inputData.length; i++) {
            let item = inputData[i];
            
            if (item.description == null)
                item.description = '';
        }
        return inputData;
    }

    render() {
        return (
            <MaterialTable
                title='Classes'
                tableRef={this.tableRef}
                columns={this.state.columns}
                data={query =>
                    new Promise((resolve, reject) => {
                        if (this.pageSize !== query.pageSize) {
                            this.pageSize = query.pageSize;
                            this.setLastPageSize(query.pageSize);
                        } 

                        let url = window.API + '/class?';
                        url += 'limit=' + query.pageSize;
                        url += '&page=' + (query.page + 1);
                        if (query.orderBy != null && query.orderBy.field != null)
                            if (query.orderDirection === 'desc')
                                url += '&sort=desc(' + query.orderBy.field + ')';
                            else
                                url += '&sort=' + query.orderBy.field;

                        if (this.state.filtering && query.filters.length > 0) {
                            url += '&filter=';
                            for (const filter of query.filters)
                                if (filter.column.type === 'numeric')
                                    url += '(' + filter.column.field + ';' + filter.operator + ';numeric;' + filter.value + ')';
                                else if (filter.column.type === 'boolean')
                                    url += '(' + filter.column.field + ';=;boolean;' + (filter.value === 'checked') + ')';
                                else
                                    url += escape('(' + filter.column.field + ';like;text;%' + filter.value + '%)');
                        }

                        // console.log(query);
                        fetch(url)
                            .then(response => response.json())
                            .then(result => {
                                this.props.onClassesLoaded(result.classes);
                                resolve({
                                    data: this.convertNullsToEmpty(result.classes),
                                    page: result.page - 1,
                                    totalCount: result.total,
                                })
                            });
                    })
                }
                options={{
                    pageSize: this.pageSize,
                    pageSizeOptions: [15, 20, 25, 30, 35, 40, 45, 50],
                    paginationType: 'stepped',
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
                    }
                ]}
                editable={{
                    onRowAdd: newData =>
                        new Promise((resolve, reject) => {
                            let url = window.API + '/class?userName=' + window.USER;

                            let result_ok = true;
                            fetch(url, {
                                method: 'POST',
                                body: JSON.stringify(newData),
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

                            let changed = false;
                            let payload = {};
                            payload.updated_by = window.USER;

                            if (oldData.class_name !== newData.class_name) {
                                payload.class_name = newData.class_name;
                                changed = true;

                                if (payload.class_name.length === 0)
                                    payload.class_name = null;
                            }

                            if (oldData.description !== newData.description) {
                                payload.description = newData.description;
                                changed = true;
                            }

                            let result_ok = true;

                            if (changed) {
                                let url = window.API + '/class/' + oldData.class_id + '?userName=' + window.USER;
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
                            setTimeout(() => {
                                let url = window.API + '/class/' + oldData.class_id + '?userName=' + window.USER;

                                fetch(url, { method: 'DELETE' })
                                    .then(handleErrors)
                                    .catch(error => {
                                        window.alert(error.message);
                                        reject();
                                    })
                                    .then(resolve());

                            }, 300);
                        }),
                }}
            />
        );
    }
}

export default ClassesTab;