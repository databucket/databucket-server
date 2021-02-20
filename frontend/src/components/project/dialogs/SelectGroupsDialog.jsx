import React from 'react';
import PropTypes from 'prop-types';
import SelectMultiDialog from "../../utils/SelectMultiDialog";
import {
    getColumnCreatedBy,
    getColumnCreatedDate,
    getColumnDescription,
    getColumnName
} from "../../utils/StandardColumns";

SelectGroupsDialog.propTypes = {
    groups: PropTypes.array.isRequired,
    rowData: PropTypes.object.isRequired,
    onChange: PropTypes.func.isRequired
}

export default function SelectGroupsDialog(props) {

    const columns = [
        getColumnName(),
        getColumnDescription(),
        getColumnCreatedDate(),
        getColumnCreatedBy()
    ]

    return (
        <SelectMultiDialog
            columns={columns}
            data={props.groups}
            ids={props.rowData['groupsIds'] != null ? props.rowData['groupsIds'] : []}
            tooltipTitle={'Select groups'}
            dialogTitle={'Select groups'}
            tableTitle={'Name: ' + (props.rowData['name'] || props.rowData['username'])}
            maxWidth='md' //'xs' | 'sm' | 'md' | 'lg' | 'xl' | false
            onChange={props.onChange}
        />
    );
}