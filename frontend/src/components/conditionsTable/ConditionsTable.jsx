import React, { forwardRef } from 'react';
import MaterialTable from 'material-table';
import AddBox from '@material-ui/icons/AddBox';
import ArrowUpward from '@material-ui/icons/ArrowUpward';
import Check from '@material-ui/icons/Check';
import ChevronLeft from '@material-ui/icons/ChevronLeft';
import ChevronRight from '@material-ui/icons/ChevronRight';
import Clear from '@material-ui/icons/Clear';
import DeleteOutline from '@material-ui/icons/DeleteOutline';
import Edit from '@material-ui/icons/Edit';
import FilterList from '@material-ui/icons/FilterList';
import FirstPage from '@material-ui/icons/FirstPage';
import LastPage from '@material-ui/icons/LastPage';
import Remove from '@material-ui/icons/Remove';
import SaveAlt from '@material-ui/icons/SaveAlt';
import Search from '@material-ui/icons/Search';
import ViewColumn from '@material-ui/icons/ViewColumn';
import ConstEditor from './ConstEditor';
import FieldEditor from './FieldEditor';
import PropertyEditor from './PropertyEditor';
import FunctionEditor from './FunctionEditor';


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

const sourceLookup = {
    'const': 'Const',
    'field': 'Field',
    'property': 'Property',
    'function': 'Function'
    // 'string': 'String',
    // 'numeric': 'Numeric',
    // 'boolean': 'Boolean',
    // 'date': 'Date',
    // 'time': 'Time',
    // 'datetime': 'Datetime',
    // 'string-array': 'String array',
    // 'numeric-array': 'Numeric array',
    // 'null': 'Null'
};

const operatorLookup = {
    '=': '=',
    '>': '>',
    '>=': '>=',
    '<': '<',
    '<=': '<=',
    '<>': '<>',
    'in': 'in',
    'not in': 'not in',
    'is': 'is',
    'is not': 'is not',
    'like': 'like',
    'not like': 'not like'
};


export default class ConditionsTable extends React.Component {

    constructor(props) {
        super(props);
        this.tableRef = React.createRef();
        this.state = {
            data: [],
            columns: [
                { title: 'Left source', field: 'left_source', lookup: sourceLookup },
                {
                    title: 'Left value', field: 'left_value',
                    editComponent: props => props.rowData.left_source != null ? 
                    (
                        props.rowData.left_source === 'const' ?                     
                        <ConstEditor rowData={props.rowData} onChange={props.onChange} /> : 
                        props.rowData.left_source === 'field' ? 
                        <FieldEditor /> : 
                        props.rowData.left_source === 'property' ?
                        <PropertyEditor /> :
                        props.rowData.left_source === 'function' ?
                        <FunctionEditor /> :
                        <div />
                    ) : <div />
                },
                {
                    title: 'Operator', field: 'operator',
                    lookup: operatorLookup
                },
                { title: 'Right source', field: 'right_source', lookup: sourceLookup },
                { title: "Right value", field: 'right_value' }
            ]
        }
    }
    render() {
        return (<MaterialTable
            icons={tableIcons}
            title="Conditions"
            tableRef={this.tableConditionsRef}
            columns={this.state.columns}
            data={this.state.data}
            options={{
                paging: false,
                actionsColumnIndex: -1,
                sorting: false,
                search: false,
                filtering: false,
                padding: 'dense',
                headerStyle: { backgroundColor: '#eeeeee' },
                rowStyle: rowData => ({ backgroundColor: rowData.tableData.id % 2 === 1 ? '#fafafa' : '#FFF' })
            }}
            components={{
                Container: props => <div {...props} />
            }}
            editable={{
                onRowAdd: newData =>
                    new Promise((resolve, reject) => {
                        let newState = this.state;
                        newState.data.conditions.push(newData);
                        this.setState(newState, () => resolve());
                    }),
                onRowUpdate: (newData, oldData) =>
                    new Promise((resolve, reject) => {
                        let newState = this.state;
                        const index = newState.data.conditions.indexOf(oldData);
                        newState.data.conditions[index] = newData;
                        this.setState(newState, () => resolve());
                    }),
                onRowDelete: oldData =>
                    new Promise((resolve, reject) => {
                        let newState = this.state;
                        const index = newState.data.conditions.indexOf(oldData);
                        newState.data.conditions.splice(index, 1);
                        this.setState(newState, () => resolve());
                    }),
            }}
        />);
    }
} 