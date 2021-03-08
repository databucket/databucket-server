import React from 'react';
import TextField from '@material-ui/core/TextField';

export default class PropertyValueLookup extends React.Component {

  constructor(props) {
    super(props);
    this.state = {
      value: props.rowData.value
    };
  }

  handleChange = name => event => {
    this.setState({ value: event.target.value });
    this.props.onChange(event.target.value);
  };

  render() {
    return (
      <div>
        <TextField
          id="property-value"
          onChange={this.handleChange()}
          value={this.state.value ? this.state.value : ''}
        />
      </div>
    );
  }
}