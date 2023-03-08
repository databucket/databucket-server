import React from 'react';
import SelectMultiDialog from "../utils/SelectMultiDialog";
import PropTypes from 'prop-types';
import {
    getColumnDescription,
    getColumnEnabled,
    getColumnExpirationDate,
    getColumnId,
    getColumnModifiedAt,
    getColumnModifiedBy,
    getColumnName
} from "../utils/StandardColumns";

SelectProjectsDialog.propTypes = {
    projects: PropTypes.array.isRequired,
    rowData: PropTypes.object.isRequired,
    onChange: PropTypes.func.isRequired
}

export default function SelectProjectsDialog(props) {

    const columns = [
        getColumnId(),
        getColumnEnabled(),
        getColumnName(),
        getColumnDescription(),
        getColumnExpirationDate(),
        getColumnModifiedAt(),
        getColumnModifiedBy()
    ]

    return (
        <SelectMultiDialog
            columns={columns}
            data={props.projects}
            ids={props.rowData['projectsIds'] != null ? props.rowData['projectsIds'] : []}
            tooltipTitle={'Select projects'}
            dialogTitle={'Select projects'}
            tableTitle={(props.rowData['username'] || '-')}
            maxWidth='lg' //'xs' | 'sm' | 'md' | 'lg' | 'xl' | false
            onChange={props.onChange}
        />
    );
}
