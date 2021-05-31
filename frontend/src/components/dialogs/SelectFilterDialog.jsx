import React from 'react';
import PropTypes from 'prop-types';
import {
    getColumnDescription,
    getColumnName, getColumnModifiedAt, getColumnModifiedBy
} from "../utils/StandardColumns";
import SelectSingleDialog from "../utils/SelectSingleDialog";

SelectFilterDialog.propTypes = {
    filters: PropTypes.array.isRequired,
    rowData: PropTypes.object.isRequired,
    onChange: PropTypes.func.isRequired
}

export default function SelectFilterDialog(props) {

    const columns = [
        getColumnName(),
        getColumnDescription(),
        getColumnModifiedAt(),
        getColumnModifiedBy()
    ]

    return (
        <SelectSingleDialog
            columns={columns}
            data={props.filters}
            id={props.rowData['filterId'] != null ? props.rowData['filterId'] : -1}
            tooltipTitle={'Select filter'}
            dialogTitle={'Select filter'}
            tableTitle={(props.rowData['name'] || '-')}
            maxWidth='md' //'xs' | 'sm' | 'md' | 'lg' | 'xl' | false
            onChange={props.onChange}
        />
    );
}