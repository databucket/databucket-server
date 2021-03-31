import React from 'react';
import PropTypes from 'prop-types';
import SelectMultiDialog from "../utils/SelectMultiDialog";
import {
    getColumnDescription,
    getColumnName, getColumnModifiedAt, getColumnModifiedBy
} from "../utils/StandardColumns";
import DynamicIcon from "../utils/DynamicIcon";
import EditIconDialog from "./EditIconDialog";

SelectBucketsDialog.propTypes = {
    buckets: PropTypes.array.isRequired,
    rowData: PropTypes.object.isRequired,
    onChange: PropTypes.func.isRequired
}

export default function SelectBucketsDialog(props) {

    const columns = [
        {
            title: 'Icon', sorting: false, field: 'iconName', searchable: false, filtering: false, initialEditValue: 'PanoramaFishEye',
            render: rowData => <DynamicIcon iconName={rowData.iconName} color='action' />,
            editComponent: props => <EditIconDialog value={props.value} onChange={props.onChange} />
        },
        getColumnName(),
        getColumnDescription(),
        { title: 'Class', field: 'classId' },
        getColumnModifiedAt(),
        getColumnModifiedBy()
    ]

    return (
        <SelectMultiDialog
            columns={columns}
            data={props.buckets}
            ids={props.rowData['bucketsIds'] != null ? props.rowData['bucketsIds'] : []}
            tooltipTitle={'Select buckets'}
            dialogTitle={'Select buckets'}
            tableTitle={(props.rowData['name'] || props.rowData['username'] || '-')}
            maxWidth='md' //'xs' | 'sm' | 'md' | 'lg' | 'xl' | false
            onChange={props.onChange}
        />
    );
}