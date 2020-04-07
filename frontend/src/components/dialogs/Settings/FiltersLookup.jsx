import React from 'react';
import TextField from '@material-ui/core/TextField';
import MenuItem from '@material-ui/core/MenuItem';

const FILTER_DEFAULT = 'none';
const BUCKET_DEFAULT = 'every';
const CLASS_DEFAULT = 'none';

export default class FiltersLookup extends React.Component {

    constructor(props) {
        super(props);
        this.state = {
            data: null,
            selected_filter_id: 'none'
        };
    };

    static getDerivedStateFromProps(props, state) {
        let bucketId = props.rowData.bucket_id === BUCKET_DEFAULT ? null : parseInt(props.rowData.bucket_id);
        let classId = props.rowData.class_id === CLASS_DEFAULT ? null : parseInt(props.rowData.class_id);
        let filteredFilters = props.allFilters.filter(f => ((f.bucket_id === null || f.bucket_id === bucketId) && (f.class_id === null || f.class_id === classId)));

        // check if selected is still on filtered filters
        let newSelectedFilterId = props.rowData.filter_id;
        if (!filteredFilters.some(f => f.filter_id.toString() === newSelectedFilterId))
            newSelectedFilterId = FILTER_DEFAULT;

        return {
            data: filteredFilters,
            selected_filter_id: newSelectedFilterId
        };
    }

    handleChange = name => event => {
        let newSelectedFilterId = event.target.value;
        this.setState({selected_filter_id: newSelectedFilterId});
        this.props.onChange(newSelectedFilterId);
    };

    render() {
        return (
            <div>
                <TextField
                    id="filter-select"
                    select
                    onChange={this.handleChange('filter')}
                    value={this.state.selected_filter_id}
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