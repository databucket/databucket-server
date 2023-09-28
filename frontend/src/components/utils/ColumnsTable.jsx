import {
    getPageSizeOptionsOnDialog,
    getTableBodyHeight,
    getTableHeaderBackgroundColor,
    getTableRowBackgroundColor,
    moveDown,
    moveUp
} from "../../utils/MaterialTableHelper";
import {
    getPropertyTitle,
    isItemChanged,
    validateItem
} from "../../utils/JsonHelper";
import {ArrowDropDown, ArrowDropUp} from "@mui/icons-material";
import MaterialTable from "material-table";
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
                columns={[
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
                ]}
                data={columns}
                onChangeRowsPerPage={onChangeRowsPerPage}
                options={{
                    paging: false,
                    pageSize: pageSize,
                    paginationType: 'stepped',
                    pageSizeOptions: getPageSizeOptionsOnDialog(),
                    actionsColumnIndex: -1,
                    sorting: true,
                    selection: false,
                    filtering: false,
                    padding: 'dense',
                    headerStyle: {
                        position: 'sticky',
                        top: 0,
                        backgroundColor: getTableHeaderBackgroundColor(theme)
                    },
                    maxBodyHeight: getBodyHeight(),
                    minBodyHeight: getBodyHeight(),
                    rowStyle: rowData => ({
                        backgroundColor: getTableRowBackgroundColor(rowData,
                            theme)
                    })
                }}
                components={{
                    Container: props => <div {...props} />
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

                            const updated = columns.map(column => {
                                if (column.tableData.id
                                    === oldData.tableData.id) {
                                    return newData;
                                }
                                return column;
                            });
                            setData(updated);
                            resolve();
                        }),
                    onRowDelete: oldData =>
                        new Promise((resolve) => {
                            setData(columns.filter(column => column.tableData.id
                                !== oldData.tableData.id))
                            resolve();
                        }),
                }}
                actions={[
                    rowData => ({
                        icon: () => <ArrowDropDown/>,
                        tooltip: 'Move down',
                        onClick: (event, rowData) => setData(
                            moveDown(columns, rowData.tableData.id)),
                        disabled: (rowData.tableData.id === columns.length - 1)
                    }),
                    rowData => ({
                        icon: () => <ArrowDropUp/>,
                        tooltip: 'Move up',
                        onClick: (event, rowData) => setData(
                            moveUp(columns, rowData.tableData.id)),
                        disabled: (rowData.tableData.id === 0)
                    })
                ]}
            />
            <MessageBox
                config={messageBox}
                onClose={() => setMessageBox({...messageBox, open: false})}
            />
        </div>
    );
}
