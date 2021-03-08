import React from 'react';
import PropTypes from 'prop-types';
import SelectMultiDialog from "../../utils/SelectMultiDialog";
import {
    getColumnAccess,
    getColumnCreatedBy,
    getColumnCreatedDate,
    getColumnDescription,
    getColumnName
} from "../../utils/StandardColumns";

SelectViewsDialog.propTypes = {
    views: PropTypes.array.isRequired,
    rowData: PropTypes.object.isRequired,
    onChange: PropTypes.func.isRequired
}

export default function SelectViewsDialog(props) {

    const columns = [
        getColumnName(),
        getColumnDescription(),
        getColumnAccess(),
        getColumnCreatedDate(),
        getColumnCreatedBy()
    ]

    return (
        <SelectMultiDialog
            columns={columns}
            data={props.views}
            ids={props.rowData['viewsIds'] != null ? props.rowData['viewsIds'] : []}
            tooltipTitle={'Select views'}
            dialogTitle={'Select views'}
            tableTitle={'Name: ' + (props.rowData['name'] || props.rowData['username'])}
            maxWidth='md' //'xs' | 'sm' | 'md' | 'lg' | 'xl' | false
            onChange={props.onChange}
        />
    );
}