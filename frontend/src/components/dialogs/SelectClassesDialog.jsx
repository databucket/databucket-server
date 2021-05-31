import React from 'react';
import PropTypes from 'prop-types';
import SelectMultiDialog from "../utils/SelectMultiDialog";
import {
    getColumnDescription,
    getColumnName, getColumnModifiedAt, getColumnModifiedBy
} from "../utils/StandardColumns";

SelectClassesDialog.propTypes = {
    classes: PropTypes.array.isRequired,
    rowData: PropTypes.object.isRequired,
    onChange: PropTypes.func.isRequired
}

export default function SelectClassesDialog(props) {

    const columns = [
        getColumnName(),
        getColumnDescription(),
        getColumnModifiedAt(),
        getColumnModifiedBy()
    ]

    return (
        <SelectMultiDialog
            columns={columns}
            data={props.classes}
            ids={props.rowData['classesIds'] != null ? props.rowData['classesIds'] : []}
            tooltipTitle={'Select classes'}
            dialogTitle={'Select classes'}
            tableTitle={(props.rowData['name'] || '-')}
            maxWidth='md' //'xs' | 'sm' | 'md' | 'lg' | 'xl' | false
            onChange={props.onChange}
        />
    );
}