import {getPropertiesTableHeight, getPageSizeOptionsOnDialog, getTableHeaderBackgroundColor, getTableRowBackgroundColor, moveDown, moveUp} from "../../utils/MaterialTableHelper";
import SelectEnumDialog from "../dialogs/SelectEnumDialog";
import {isItemChanged, uuidV4, validateItem} from "../../utils/JsonHelper";
import ArrowDropDown from "@material-ui/icons/ArrowDropDown";
import ArrowDropUp from "@material-ui/icons/ArrowDropUp";
import MaterialTable from "material-table";
import React, {useContext, useState} from "react";
import {getLastPageSizeOnDialog, setLastPageSizeOnDialog} from "../../utils/ConfigurationStorage";
import {useTheme} from "@material-ui/core/styles";
import {MessageBox} from "./MessageBox";
import {useWindowDimension} from "./UseWindowDimension";
import PropTypes from "prop-types";
import AccessContext from "../../context/access/AccessContext";

PropertiesTable.propTypes = {
    data: PropTypes.array.isRequired,
    title: PropTypes.string,
    pageSize: PropTypes.number,
    customTableWidth: PropTypes.number,
    onChange: PropTypes.func.isRequired
};

export default function PropertiesTable(props) {

    const theme = useTheme();
    const tableRef = React.createRef();
    const [height] = useWindowDimension();
    const accessContext = useContext(AccessContext);
    const [messageBox, setMessageBox] = useState({open: false, severity: 'error', title: '', message: ''});
    const [pageSize, setPageSize] = useState(getLastPageSizeOnDialog);
    const data = props.data;
    const changeableFields = ['title', 'path', 'type', 'enumId'];
    const fieldsSpecification = {
        title: {title: 'Title', check: ['notEmpty', 'min1', 'max30']},
        path: {title: 'Path', check: ['notEmpty', 'validJsonPath']},
        type: {title: 'Type', check: ['validClassPropertyType']}
    };

    const setData = (newData) => {
        props.onChange(newData);
    }

    const onChangeRowsPerPage = (pageSize) => {
        setPageSize(pageSize);
        setLastPageSizeOnDialog(pageSize);
    }

    const allowEnum = (rowData) => {
        return rowData.type === 'select';
    }

    const getEnumName = (rowData) => {
        if (allowEnum(rowData)) {
            const found = accessContext.enums.find(en => en.id === rowData['enumId']);
            if (found)
                return found.name;
        }
        return '';
    }

    return (
        <div>
            <MaterialTable
                title={props.title}
                tableRef={tableRef}
                columns={[
                    {title: '#', cellStyle: {width: '1%'}, render: (rowData) => rowData ? rowData.tableData.id + 1 : ''},
                    {title: 'Title', field: 'title', type: 'string', emptyValue: '', initialEditValue: ''},
                    {title: 'Path', field: 'path', type: 'string', emptyValue: '', initialEditValue: ''},
                    {
                        title: 'Type',
                        field: 'type',
                        lookup: {
                            'string': 'String',
                            'numeric': 'Numeric',
                            'datetime': 'Datetime',
                            'date': 'Date',
                            'time': 'Time',
                            'boolean': 'Boolean',
                            'select': 'Enum',
                        },
                        initialEditValue: 'string'
                    },
                    {
                        title: 'Enum',
                        field: 'enumId',
                        render: rowData => getEnumName(rowData),
                        editComponent: props => allowEnum(props.rowData) ? (
                            <SelectEnumDialog
                                enums={accessContext.enums}
                                rowData={props.rowData}
                                onChange={props.onChange}
                            />
                        ) : <div/>
                    },
                    {title: 'UUID', field: 'uuid', hidden: true, type: 'string'}
                ]}
                data={data}
                onChangeRowsPerPage={onChangeRowsPerPage}
                options={{
                    paging: props.pageSize != null,
                    pageSize: props.pageSize != null ? props.pageSize : pageSize,
                    paginationType: 'stepped',
                    pageSizeOptions: getPageSizeOptionsOnDialog(),
                    actionsColumnIndex: -1,
                    sorting: true,
                    selection: false,
                    filtering: false,
                    padding: 'dense',
                    headerStyle: {backgroundColor: getTableHeaderBackgroundColor(theme)},
                    maxBodyHeight: getPropertiesTableHeight(height, props.customTableWidth != null ? props.customTableWidth : 5),
                    minBodyHeight: getPropertiesTableHeight(height, props.customTableWidth != null ? props.customTableWidth : 5),
                    rowStyle: rowData => ({backgroundColor: getTableRowBackgroundColor(rowData, theme)})
                }}
                components={{
                    Container: props => <div {...props} />
                }}
                editable={{
                    onRowAdd: newData =>
                        new Promise((resolve, reject) => {
                            let message = validateItem(newData, fieldsSpecification);
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

                            newData['uuid'] = uuidV4();

                            setData([...data, newData]);
                            resolve();
                        }),
                    onRowUpdate: (newData, oldData) =>
                        new Promise((resolve, reject) => {
                            if (!isItemChanged(oldData, newData, changeableFields)) {
                                setMessageBox({
                                    open: true,
                                    severity: 'info',
                                    title: 'Nothing changed',
                                    message: ''
                                });
                                reject();
                                return;
                            }

                            let message = validateItem(newData, fieldsSpecification);
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

                            const updated = data.map(column => {
                                if (column.tableData.id === oldData.tableData.id)
                                    return newData;
                                return column;
                            });
                            setData(updated);
                            resolve();
                        }),
                    onRowDelete: oldData =>
                        new Promise((resolve, reject) => {
                            if (props.used != null && props.used.includes(oldData.uuid)) {
                                setMessageBox({
                                    open: true,
                                    severity: 'error',
                                    title: 'The property can not be removed',
                                    message: 'This property is used!'
                                });
                                reject();
                            } else {
                                setData(data.filter(column => column.tableData.id !== oldData.tableData.id));
                                resolve();
                            }
                        }),
                }}
                actions={[
                    rowData => ({
                        icon: () => <ArrowDropDown/>,
                        tooltip: 'Move down',
                        onClick: (event, rowData) => setData(moveDown(data, rowData.tableData.id)),
                        disabled: (rowData.tableData.id === data.length - 1)
                    }),
                    rowData => ({
                        icon: () => <ArrowDropUp/>,
                        tooltip: 'Move up',
                        onClick: (event, rowData) => setData(moveUp(data, rowData.tableData.id)),
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
};

export const mergeProperties = (properties, dataClass) => {
    let newProperties = properties != null ? properties : [];

    if (dataClass != null && dataClass.configuration != null) {
        dataClass.configuration.forEach(property => {
            if (!newProperties.find(f => f.path === property.path)) {
                newProperties = [...newProperties, property];
            }
        });
    }
    return newProperties;
}