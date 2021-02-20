import React from 'react';
import PropTypes from 'prop-types';
import {
    getColumnCreatedBy,
    getColumnCreatedDate,
    getColumnDescription,
    getColumnName
} from "../../utils/StandardColumns";
import SelectSingleDialog from "../../utils/SelectSingleDialog";

SelectEnumDialog.propTypes = {
    enums: PropTypes.array.isRequired,
    rowData: PropTypes.object.isRequired,
    onChange: PropTypes.func.isRequired
}

export default function SelectEnumDialog(props) {

    const columns = [
        getColumnName(),
        getColumnDescription(),
        getColumnCreatedDate(),
        getColumnCreatedBy()
    ]

    return (
        <SelectSingleDialog
            columns={columns}
            data={props.enums}
            id={props.rowData['enumId'] != null ? props.rowData['enumId'] : -1}
            tooltipTitle={'Select enum'}
            dialogTitle={'Select enum'}
            tableTitle={'Name: ' + props.rowData['name']}
            maxWidth='md' //'xs' | 'sm' | 'md' | 'lg' | 'xl' | false
            onChange={props.onChange}
        />
    );
}