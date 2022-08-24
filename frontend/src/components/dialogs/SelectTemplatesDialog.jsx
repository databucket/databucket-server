import React from 'react';
import SelectMultiDialog from "../utils/SelectMultiDialog";
import PropTypes from 'prop-types';
import {getColumnDescription, getColumnName, getColumnModifiedAt, getColumnModifiedBy} from "../utils/StandardColumns";

SelectTemplatesDialog.propTypes = {
    templates: PropTypes.array.isRequired,
    rowData: PropTypes.object.isRequired,
    onChange: PropTypes.func.isRequired
}

export default function SelectTemplatesDialog(props) {

    const columns = [
        getColumnName(),
        getColumnDescription(),
        getColumnModifiedAt(),
        getColumnModifiedBy()
    ]

    return (
        <SelectMultiDialog
            columns={columns}
            data={props.templates}
            ids={props.rowData['templatesIds'] != null ? props.rowData['templatesIds'] : []}
            tooltipTitle={'Select templates'}
            dialogTitle={'Select templates'}
            tableTitle={(props.rowData['name'] || '-')}
            maxWidth='lg' //'xs' | 'sm' | 'md' | 'lg' | 'xl' | false
            onChange={props.onChange}
        />
    );
}