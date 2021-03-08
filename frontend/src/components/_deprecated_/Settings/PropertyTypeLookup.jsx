import React from 'react';
import TextField from '@material-ui/core/TextField';
import MenuItem from '@material-ui/core/MenuItem';

export default class DataTypesLookup extends React.Component {

  constructor(props) {
    super(props);
    this.state = {
      type: props.type,
      data: [ 
        { key: 'string', value: 'String' }, 
        { key: 'numeric', value: 'Numeric' }, 
        // { key: 'datetime', value: 'Datetime' }, 
        // { key: 'date', value: 'Date' }, 
        // { key: 'time', value: 'Time' }, 
        { key: 'boolean', value: 'Boolean' },
        { key: 'null', value: 'Null'}
      ]
    };
  }

  handleChange = name => event => {
      this.setState({type: event.target.value});
      this.props.onChange(event.target.value);
  };

  render() {
    return (
      <div>
        <TextField
          id="data-type-select"
          select
          onChange={this.handleChange()}
          value={this.state.type}
        >
          {this.state.data.map(item => (
            <MenuItem key={item.key} value={item.key}>
              {item.value}
            </MenuItem>
          ))}
        </TextField>
      </div>
    );
  }
}