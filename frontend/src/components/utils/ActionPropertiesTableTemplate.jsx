import {getTableBodyHeight, getTableHeaderBackgroundColor, getTableRowBackgroundColor, moveDown, moveUp} from "../../utils/MaterialTableHelper";
import {convertPropertiesDates, getPropertyByUuid, getPropertyTitle, isItemChanged, validateItem} from "../../utils/JsonHelper";
import ArrowDropDown from "@material-ui/icons/ArrowDropDown";
import ArrowDropUp from "@material-ui/icons/ArrowDropUp";
import MaterialTable, {MTableEditField} from "material-table";
import React, {useState} from "react";
import {useTheme} from "@material-ui/core/styles";
import {MessageBox} from "./MessageBox";
import PropTypes from "prop-types";
import {useWindowDimension} from "./UseWindowDimension";
import moment from 'moment';

ActionPropertiesTableTemplate.propTypes = {
    data: PropTypes.array,
    properties: PropTypes.array,
    onChange: PropTypes.func.isRequired,
    pageSize: PropTypes.number,
    parentContentRef: PropTypes.object.isRequired,
    enums: PropTypes.array.isRequired
}

export default function ActionPropertiesTableTemplate(props) {

    const theme = useTheme();
    const [height] = useWindowDimension();
    const tableRef = React.createRef();
    const [messageBox, setMessageBox] = useState({open: false, severity: 'error', title: '', message: ''});
    const data = convertPropertiesDates(props.data, props.properties);
    const properties = props.properties;
    const changeableFields = ['uuid', 'action', 'title', 'value'];
    const fieldsSpecification = {
        uuid: {title: 'Title', check: ['notEmpty']}
    };

    const setData = (newData) => {
        props.onChange(newData);
    }

    const getBodyHeight = (windowHeight) => {
        return getTableBodyHeight(props.parentContentRef, 66);
    }

    const getInitialProperty = () => {
        if (properties.length > 0) {
            return properties[0].uuid;
        } else
            return 'none';
    }

    const isEnumValue = (value, property) => {
        const enumDef = getEnumDef(property);
        const values = enumDef.items.map(item => item.value);
        return values.includes(value);
    }

    const getInitialValue = (value, property) => {
        if (['date', 'datetime', 'time'].includes(property.type) && !(value instanceof Date)) {
            return new Date();
        } else if (property.type === 'numeric' && !Number.isFinite(value)) {
            return null;
        } else if (property.type === 'boolean' && !(value === true || value === false)) {
            return false;
        } else if (property.type === 'string' && !(typeof value === 'string' || value instanceof String)) {
            return '';
        } else if (property.type === 'select' && (value == null || !isEnumValue(value, property))) {
            const enumDef = getEnumDef(property);
            return enumDef.items[0].value;
        } else
            return value;
    }

    const getPropertiesLookup = (properties) => {
        if (properties.length > 0)
            return properties.reduce((obj, item) => ({...obj, [item['uuid']]: item.title}), {});
        else
            return {none: '- none -'};
    }

    const getEnumLookup = (property) => {
        if (property.type === 'select') {
            const enumDef = getEnumDef(property);
            return enumDef.items.reduce((obj, item) => ({...obj, [item['value']]: item.text}), {});
        } else
            return null;
    }

    const getEnumDef = (property) => {
        const propDef = getPropertyByUuid(properties, property.uuid);
        return props.enums.filter(e => e.id === propDef.enumId)[0];
    }

    const renderValue = (property, value) => {
        if (property.type === 'datetime') {
            return moment(value).format('DD.MM.yyyy, HH:mm:ss');
        } else if (property.type === 'date') {
            return moment(value).format('DD.MM.yyyy');
        } else if (property.type === 'time') {
            return moment(value).format('HH:mm:ss');
        } else
            return value.toString();
    }

    const validateValue = (newData) => {
        if (newData.action === 'setValue' && newData.value == null) {
            return `Incorrect value! `;
        } else
            return null;
    }

    const validatePropertyDuplicates = (newData) => {
        if (data.map(item => item.uuid).includes(newData.uuid)) {
            return `Property can not be duplicated! `;
        } else
            return null;
    }

    const getEditable = () => {
        let editable = {};

        if (properties.length > 0)
            editable = {
                ...editable,
                onRowAdd: newData =>
                    new Promise((resolve, reject) => {
                        let message = validateItem(newData, fieldsSpecification);
                        let message2 = validateValue(newData);
                        let message3 = validatePropertyDuplicates(newData);

                        if (message != null || message2 != null || message3) {
                            setMessageBox({
                                open: true,
                                severity: 'warning',
                                title: 'Item is not valid',
                                message: message || message2 || message3
                            });
                            reject();
                            return;
                        }

                        setData([...data, newData]);
                        resolve();
                    })
            };

        editable = {
            ...editable,
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
                    let message2 = validateValue(newData);
                    let message3 = validatePropertyDuplicates(newData);
                    if (message != null || message2 != null || message3) {
                        setMessageBox({
                            open: true,
                            severity: 'warning',
                            title: 'Item is not valid',
                            message: message || message2 || message3
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
                })
        };

        editable = {
            ...editable,
            onRowDelete: oldData =>
                new Promise((resolve) => {
                    setData(data.filter(column => column.tableData.id !== oldData.tableData.id));
                    resolve();
                })
        };

        return editable;
    }

    return (
        <div>
            <MaterialTable
                title={'Modify properties:'}
                tableRef={tableRef}
                columns={[
                    {title: '#', cellStyle: {width: '1%'}, render: (rowData) => rowData ? rowData.tableData.id + 1 : ''},
                    {
                        title: 'Property',
                        field: 'uuid',
                        type: 'string',
                        initialEditValue: getInitialProperty(properties),
                        lookup: getPropertiesLookup(properties),
                        render: rowData => getPropertyTitle(properties, rowData.uuid)
                    },
                    {
                        title: 'Action',
                        field: 'action',
                        lookup: {'remove': 'Remove', 'setValue': 'Set value', 'setNull': 'Set null'},
                        initialEditValue: 'setValue'
                    },
                    {
                        title: 'Value',
                        field: 'value',
                        type: 'string',
                        render: rowData => {
                            if (rowData.action === 'setValue')
                                return renderValue(getPropertyByUuid(properties, rowData.uuid), rowData.value)
                            else
                                return '';
                        },
                        editComponent: props => {
                            if (props.rowData.action === 'setValue') {
                                const property = getPropertyByUuid(properties, props.rowData.uuid);
                                props.rowData.value = getInitialValue(props.rowData.value, property);
                                return (
                                    <MTableEditField
                                        value={props.rowData.value}
                                        columnDef={{
                                            type: property.type,
                                            lookup: getEnumLookup(property),
                                            editPlaceholder: property.type,
                                            title: property.title
                                        }}
                                        onChange={props.onChange}
                                    />
                                )
                            } else
                                return (<div/>);
                        }
                    }
                ]}
                data={data}
                options={{
                    paging: false,
                    actionsColumnIndex: -1,
                    sorting: true,
                    selection: false,
                    search: true,
                    filtering: false,
                    padding: 'dense',
                    minBodyHeight: getBodyHeight(height),
                    maxBodyHeight: getBodyHeight(height),
                    headerStyle: {position: 'sticky', top: 0, backgroundColor: getTableHeaderBackgroundColor(theme)},
                    rowStyle: rowData => ({backgroundColor: getTableRowBackgroundColor(rowData, theme)})
                }}
                components={{
                    Container: props => <div {...props} />
                }}
                editable={getEditable()}
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
            }
        );
    }
    return newProperties;
}