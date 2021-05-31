import React from 'react';
import PropTypes from 'prop-types';
import SelectMultiDialog from "../utils/SelectMultiDialog";
import {
    getColumnDescription,
    getColumnName, getColumnModifiedAt, getColumnModifiedBy
} from "../utils/StandardColumns";

SelectTeamsDialog.propTypes = {
    teams: PropTypes.array.isRequired,
    rowData: PropTypes.object.isRequired,
    onChange: PropTypes.func.isRequired
}

export default function SelectTeamsDialog(props) {

    const columns = [
        getColumnName(),
        getColumnDescription(),
        getColumnModifiedAt(),
        getColumnModifiedBy()
    ]

    return (
        <SelectMultiDialog
            columns={columns}
            data={props.teams}
            ids={props.rowData['teamsIds'] != null ? props.rowData['teamsIds'] : []}
            tooltipTitle={'Select teams'}
            dialogTitle={'Select teams'}
            tableTitle={(props.rowData['name'] || props.rowData['username'] || '-')}
            maxWidth='md' //'xs' | 'sm' | 'md' | 'lg' | 'xl' | false
            onChange={props.onChange}
        />
    );
}