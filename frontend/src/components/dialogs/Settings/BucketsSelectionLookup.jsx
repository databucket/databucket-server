import React from 'react';
import { MultiSelect } from 'react-material-ui-super-select';

export default class BucketsSelectionLookup extends React.Component {

  constructor(props) {
    super(props);
    this.state = {
      options: this.convertObjectIntoArray(props.bucketsLookup),
      rowData: props.rowData,
      value: this.getValues(props.bucketsLookup, props.rowData.buckets),
    };
  }

  getValues(allBuckets, selectedIds) {
    if (selectedIds != null && selectedIds.length > 0) {
      let resultArray = []
      for (var i = 0; i < selectedIds.length; i++)
        resultArray.push({ id: "" + selectedIds[i], label: allBuckets[selectedIds[i]] });

      return resultArray;
    } else
      return null;
  }

  convertObjectIntoArray(jsonObject) {
    let resultArray = []
    Object.keys(jsonObject).forEach(function (key) {
      resultArray.push({ id: key, label: jsonObject[key] });
    });
    return resultArray;
  }

  getIdsArray(inputArray) {
    if (inputArray != null && inputArray.length > 0) {
      let resultArray = [];
      for (var i = 0; i < inputArray.length; i++)
        resultArray.push(parseInt(inputArray[i].id));
      return resultArray;
    } else
      return null;
  }

  handleChange = value => {
    this.setState({ value });
    this.props.onChange(this.getIdsArray(value));
  };

  handleClearValue = () => {
    this.setState({ value: null })
    this.props.onChange(null);
  };

  render() {
    return (
      <div style={{ width: 200 }} >
        <MultiSelect
          hideLabel={true}
          stayOpenAfterSelection={true}
          options={this.state.options}
          handleChange={(value) => this.handleChange(value)}
          handleClearValue={() => this.handleClearValue()}
          selectedValue={this.state.value}
          containerClassName="select-container"
        />
      </div>
    );
  }
}
