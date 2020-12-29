import React from 'react';
import TextField from '@material-ui/core/TextField';
import MenuItem from '@material-ui/core/MenuItem';

const BUCKET_DEFAULT = 'every';
const CLASS_DEFAULT = 'none';

export default class ColumnsLookup extends React.Component {

    constructor(props) {
        super(props);
        this.state = {
            data: null,
            selected_columns_id: 0
        };
    }

    static getDerivedStateFromProps(props, state) {
        console.log('getDerivedStateFromProps');
        let bucketId = props.rowData.bucket_id === BUCKET_DEFAULT ? null : parseInt(props.rowData.bucket_id);
        let classId = props.rowData.class_id === CLASS_DEFAULT ? null : parseInt(props.rowData.class_id);
        let filteredColumns = props.allColumns.filter(c => ((c.bucket_id === null || c.bucket_id === bucketId) && (c.class_id === null || c.class_id === classId)));

        // check if selected is still on filtered columns
        let newSelectedColumnId = props.rowData.columns_id;
        if (!filteredColumns.some(c => c.columns_id === newSelectedColumnId))
            newSelectedColumnId = filteredColumns[0].columns_id;

        return {
            data: filteredColumns,
            selected_columns_id: newSelectedColumnId
        };
    }

    handleChange = name => event => {
        let new_selected_columns_id = event.target.value;
        this.setState({selected_columns_id: new_selected_columns_id});
        this.props.onChange(new_selected_columns_id);
    };

    render() {
        return (
            <div>
                <TextField
                    id="standard-select-columns"
                    select
                    onChange={this.handleChange('columns')}
                    value={this.state.selected_columns_id}
                >
                    {this.state.data.map(columns => (
                        <MenuItem key={columns.columns_id} value={columns.columns_id}>
                            {columns.columns_name}
                        </MenuItem>
                    ))}
                </TextField>
            </div>
        );
    }
}
