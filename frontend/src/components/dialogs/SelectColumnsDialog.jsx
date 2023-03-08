import React from 'react';
import PropTypes from 'prop-types';
import {getColumnDescription, getColumnModifiedAt, getColumnModifiedBy, getColumnName} from "../utils/StandardColumns";
import SelectSingleDialog from "../utils/SelectSingleDialog";

SelectColumnsDialog.propTypes = {
    columns: PropTypes.array.isRequired,
    rowData: PropTypes.object.isRequired,
    onChange: PropTypes.func.isRequired
}

export default function SelectColumnsDialog(props) {

    const columns = [
        getColumnName(),
        getColumnDescription(),
        getColumnModifiedAt(),
        getColumnModifiedBy()
    ]

    return (
        <SelectSingleDialog
            columns={columns}
            data={props.columns}
            id={props.rowData['columnsId'] != null ? props.rowData['columnsId'] : -1}
            tooltipTitle={'Select columns'}
            dialogTitle={'Select columns'}
            tableTitle={props.rowData['name'] || '-'}
            maxWidth='md' //'xs' | 'sm' | 'md' | 'lg' | 'xl' | false
            onChange={props.onChange}
        />
    );
}
