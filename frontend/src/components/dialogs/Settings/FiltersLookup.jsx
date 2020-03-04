import React from 'react';
import TextField from '@material-ui/core/TextField';
import MenuItem from '@material-ui/core/MenuItem';

const FILTER_DEFAULT = 'none';

export default class FiltersLookup extends React.Component {

  constructor(props) {
    super(props);
    this.state = {
      data: props.allFilters.filter(f => (f.bucket_id === null | f.bucket_id === props.rowData.bucket_id)),
      rowData: props.rowData
    };
  }

  handleChange = name => event => {
      let rowData = this.state.rowData;
      rowData.filter_id = event.target.value;
      this.setState({rowData: rowData});
      this.props.onChange(event.target.value);
  };

  render() {
    return (
      <div>
        <TextField
          id="filter-select"
          select
          onChange={this.handleChange('filter')}
          value={this.state.rowData.filter_id}
        >
          <MenuItem key={FILTER_DEFAULT} value={FILTER_DEFAULT}>- none -</MenuItem>
          {this.state.data.map(filter => (
            <MenuItem key={filter.filter_id.toString()} value={filter.filter_id.toString()}>
              {filter.filter_name}
            </MenuItem>
          ))}
        </TextField>
      </div>
    );
  }
}