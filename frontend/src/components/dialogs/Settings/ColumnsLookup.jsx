import React from 'react';
import TextField from '@material-ui/core/TextField';
import MenuItem from '@material-ui/core/MenuItem';

export default class ColumnsLookup extends React.Component {

  constructor(props) {
    super(props);
    this.state = {
      data: props.allColumns.filter(c => (c.bucket_id === null | c.bucket_id === props.rowData.bucket_id)),
      rowData: props.rowData
    };
  }

  handleChange = name => event => {
      let rowData = this.state.rowData;
      rowData.columns_id = event.target.value;
      this.setState({rowData: rowData});
      this.props.onChange(event.target.value);
  };

  render() {
    return (
      <div>
        <TextField
          id="standard-select-columns"
          select
          onChange={this.handleChange('columns')}
          value={this.state.rowData.columns_id}
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
