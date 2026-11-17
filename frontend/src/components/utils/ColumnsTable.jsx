import {
    getPageSizeOptionsOnDialog,
    getTableBodyHeight,
    getTableHeaderBackgroundColor,
    getTableRowBackgroundColor,
    movedRow,
    withoutHeaderSorting,
    removeRow,
    replaceRow
} from "../../utils/MaterialTableHelper";
import {
    getPropertyTitle,
    isItemChanged,
    validateItem
} from "../../utils/JsonHelper";
import MaterialTable from '@material-table/core';
import DraggableBodyRow from "./DraggableBodyRow";
import React, {useEffect, useState} from "react";
import {
    getLastPageSizeOnDialog,
    setLastPageSizeOnDialog
} from "../../utils/ConfigurationStorage";
import {useTheme} from "@mui/material";
import {MessageBox} from "./MessageBox";
import SelectSingleFieldLookup, {
    commonFields
} from "../lookup/SelectSingleFieldLookup";

export default function ColumnsTable(props) {

    const theme = useTheme();
    const tableRef = React.createRef();
    const [messageBox, setMessageBox] = useState(
        {open: false, severity: 'error', title: '', message: ''});
    const [pageSize, setPageSize] = useState(getLastPageSizeOnDialog);
    const columns = props.columns;
    const properties = props.properties;
    const changeableFields = ['uuid', 'enabled', 'hidden', 'editable',
        'sorting', 'filtering', 'width']; // disabled: format, align, width
    const fieldsSpecification = {
        uuid: {title: 'Title', check: ['notEmpty']}
    };

    // This timeout allows to set dialog size before first rendering
    const [delay, setDelay] = useState(true);
    useEffect(() => {
        setTimeout(() => setDelay(false), 1)
    }, []);

    const getBodyHeight = () => {
        return getTableBodyHeight(props.parentContentRef, 66);
    }

    const setData = (newData) => {
        props.onChange(newData);
    }

    const onChangeRowsPerPage = (pageSize) => {
        setPageSize(pageSize);
        setLastPageSizeOnDialog(pageSize);
    }

    return !delay && (
        <div>
            <MaterialTable
                title={'Class origin or defined properties:'}
                tableRef={tableRef}
                columns={withoutHeaderSorting([
                    {
                        title: '#',
                        cellStyle: {width: '1%'},
                        render: (rowData) => rowData ? rowData.tableData.id + 1
                            : ''
                    },
                    {
                        title: 'Enabled',
                        field: 'enabled',
                        type: 'boolean',
                        initialEditValue: true,
                        cellStyle: {width: '1%'}
                    },
                    {
                        title: 'Title',
                        field: 'uuid',
                        filtering: false,
                        sorting: false,
                        type: 'string',
                        initialEditValue: "uuid_data_id",
                        render: rowData => getPropertyTitle(
                            properties.concat(commonFields), rowData.uuid),
                        editComponent: props => <SelectSingleFieldLookup
                            selected={props.rowData.uuid}
                            properties={properties}
                            onChange={props.onChange}/>
                    },
                    {
                        title: 'Hidden',
                        field: 'hidden',
                        type: 'boolean',
                        initialEditValue: false,
                        cellStyle: {width: '1%'}
                    },
                    // {
                    //     title: 'Align', field: 'align', type: 'string', editable: 'always',
                    //     lookup: {
                    //         'center': 'Center',
                    //         'inherit': 'Inherit',
                    //         'justify': 'Justify',
                    //         'left': 'Left',
                    //         'right': 'Right'
                    //     }, initialEditValue: 'center'
                    // },
                    // {title: 'Format', field: 'format', type: 'string', editable: 'always', emptyValue: ''},
                    {
                        title: 'Width',
                        field: 'width',
                        type: 'string',
                        editable: 'always',
                        emptyValue: ''
                    },
                    {
                        title: 'Editable',
                        field: 'editable',
                        lookup: {
                            'always': 'Always',
                            'never': 'Never',
                            'onAdd': 'On add',
                            'onUpdate': 'On update'
                        },
                        initialEditValue: 'always'
                    },
                    {
                        title: 'Sorting',
                        field: 'sorting',
                        type: 'boolean',
                        initialEditValue: true,
                        cellStyle: {width: '1%'}
                    },
                    {
                        title: 'Filtering',
                        field: 'filtering',
                        type: 'boolean',
                        initialEditValue: true,
                        cellStyle: {width: '1%'}
                    }
                ])}
                data={columns}
                onRowsPerPageChange={onChangeRowsPerPage}
                options={{
                    paging: false,
                    pageSize: pageSize,
                    paginationType: 'stepped',
                    pageSizeOptions: getPageSizeOptionsOnDialog(),
                    actionsColumnIndex: -1,
                    selection: false,
                    filtering: false,
                    padding: 'dense',
                    headerStyle: {
                        position: 'sticky',
                        top: 0,
                        backgroundColor: getTableHeaderBackgroundColor(theme),
                        zIndex: 2
                    },
                    maxBodyHeight: getBodyHeight(),
                    minBodyHeight: getBodyHeight(),
                    rowStyle: rowData => ({
                        backgroundColor: getTableRowBackgroundColor(rowData,
                            theme)
                    })
                }}
                components={{
                    Container: props => <div {...props} />,
                    Row: props => <DraggableBodyRow {...props}
                                                    onMoveRow={(from, to) => setData(movedRow(columns, from, to))}/>
                }}
                editable={{
                    onRowAdd: newData =>
                        new Promise((resolve, reject) => {
                            let message = validateItem(newData,
                                fieldsSpecification);
                            if (message != null) {
                                setMessageBox({
                                    open: true,
                                    severity: 'warning',
                                    title: 'Item is not valid',
                                    message: message
                                });
                                reject();
                                return;
                            }

                            setData([...columns, newData]);
                            resolve();
                        }),
                    onRowUpdate: (newData, oldData) =>
                        new Promise((resolve, reject) => {
                            if (!isItemChanged(oldData, newData,
                                changeableFields)) {
                                setMessageBox({
                                    open: true,
                                    severity: 'info',
                                    title: 'Nothing changed',
                                    message: ''
                                });
                                reject();
                                return;
                            }

                            let message = validateItem(newData,
                                fieldsSpecification);
                            if (message != null) {
                                setMessageBox({
                                    open: true,
                                    severity: 'error',
                                    title: 'Item is not valid',
                                    message: message
                                });
                                reject();
                                return;
                            }

                            setData(replaceRow(columns, oldData, newData));
                            resolve();
                        }),
                    onRowDelete: oldData =>
                        new Promise((resolve) => {
                            setData(removeRow(columns, oldData));
                            resolve();
                        }),
                }}
            />
            <MessageBox
                config={messageBox}
                onClose={() => setMessageBox({...messageBox, open: false})}
            />
        </div>
    );
}
