import React, {createRef, useContext, useEffect, useState} from 'react';
import {withStyles} from '@material-ui/core/styles';
import Dialog from '@material-ui/core/Dialog';
import MuiDialogTitle from '@material-ui/core/DialogTitle';
import MuiDialogContent from '@material-ui/core/DialogContent';
import MuiDialogActions from '@material-ui/core/DialogActions';
import IconButton from '@material-ui/core/IconButton';
import CloseIcon from '@material-ui/icons/Done';
import Typography from '@material-ui/core/Typography';
import MoreHoriz from "@material-ui/icons/MoreHoriz";
import Tooltip from "@material-ui/core/Tooltip";
import MaterialTable from "material-table";
import {useTheme} from '@material-ui/core/styles';
import PropTypes from 'prop-types';
import Button from "@material-ui/core/Button";
import {getLastPageSizeOnDialog, setLastPageSizeOnDialog} from "../../../utils/ConfigurationStorage";
import {
    getPageSizeOptionsOnDialog,
    getTableHeaderBackgroundColor,
    getTableIcons, getTableRowBackgroundColor, moveDown, moveUp
} from "../../../utils/MaterialTableHelper";
import ExamplesIcon from "@material-ui/icons/Grain";
import ArrowDropUp from "@material-ui/icons/ArrowDropUp";
import ArrowDropDown from "@material-ui/icons/ArrowDropDown";
import EnumsContext from "../../../context/enums/EnumsContext";
import SelectEnumDialog from "./SelectEnumDialog";
import {MessageBox} from "../../utils/MessageBox";
import {convertNullValuesInCollection, isItemChanged, validateItem} from "../../../utils/JsonHelper";
import {getColumnMapper} from "../../../utils/NullValueMappers";

const styles = (theme) => ({
    root: {
        margin: 0,
        padding: theme.spacing(2),
    },
    closeButton: {
        position: 'absolute',
        right: theme.spacing(1),
        top: theme.spacing(1),
        color: theme.palette.grey[500],
    },
});

const DialogTitle = withStyles(styles)((props) => {
    const {children, classes, onClose, ...other} = props;
    return (
        <MuiDialogTitle disableTypography className={classes.root} {...other}>
            <Typography variant="h6">{children}</Typography>
            {onClose ? (
                <IconButton aria-label="close" className={classes.closeButton} onClick={onClose}>
                    <CloseIcon/>
                </IconButton>
            ) : null}
        </MuiDialogTitle>
    );
});

const DialogContent = withStyles((theme) => ({
    root: {
        padding: theme.spacing(0),
    },
}))(MuiDialogContent);

const DialogActions = withStyles((theme) => ({
    root: {
        margin: 0,
        padding: theme.spacing(1),
    },
}))(MuiDialogActions);


EditColumnsDialog.propTypes = {
    configuration: PropTypes.array.isRequired,
    name: PropTypes.string.isRequired,
    description: PropTypes.string.isRequired,
    onChange: PropTypes.func.isRequired
}

export default function EditColumnsDialog(props) {

    const theme = useTheme();
    const [messageBox, setMessageBox] = useState({open: false, severity: 'error', title: '', message: ''});
    const [data, setData] = useState(convertNullValuesInCollection(props.configuration, getColumnMapper()));
    const [open, setOpen] = useState(false);
    const tableRef = createRef();
    const [pageSize, setPageSize] = useState(getLastPageSizeOnDialog);
    const enumsContext = useContext(EnumsContext);
    const {enums, fetchEnums} = enumsContext;
    const changeableFields = ['title', 'field', 'type', 'enabled', 'align', 'hidden', 'format', 'width', 'enumId', 'editable', 'sorting', 'filtering'];
    const fieldsSpecification = {
        title: {title: 'Title', check: ['notEmpty', 'min1', 'max50']},
        field: {title: 'Source', check: ['notEmpty']}
    };

    useEffect(() => {
        if (enums == null)
            fetchEnums();
    }, [enums, fetchEnums]);

    const handleClickOpen = () => {
        setOpen(true);
    };

    const handleSave = () => {
        props.onChange(data);
        setOpen(false);
    }

    const onChangeRowsPerPage = (pageSize) => {
        setPageSize(pageSize);
        setLastPageSizeOnDialog(pageSize);
    }

    const setDefaultColumns = () => {
        setData([
            {enabled: true, title: 'Id', field: 'dataId', type: 'numeric', editable: 'never', sorting: true, filtering: true, align: 'center', hidden: false},
            {enabled: true, title: 'Tag', field: 'tagId', type: 'numeric', editable: 'always', sorting: true, filtering: true, align: 'center', hidden: false},
            {enabled: true, title: 'Locked', field: 'locked', type: 'boolean', editable: 'always', sorting: true, filtering: true, align: 'center', hidden: false},
            {enabled: true, title: 'Locked by', field: 'lockedBy', type: 'string', editable: 'never', sorting: true, filtering: true, align: 'center', hidden: false},
            {enabled: true, title: 'Created by', field: 'createdBy', type: 'string', editable: 'never', sorting: true, filtering: true, align: 'center', hidden: false},
            {enabled: true, title: 'Created at', field: 'createdDate', type: 'datetime', editable: 'never', sorting: true, filtering: true, align: 'center', hidden: false},
            {enabled: true, title: 'Updated by', field: 'updatedBy', type: 'string', editable: 'never', sorting: true, filtering: true, align: 'center', hidden: false},
            {enabled: true, title: 'Updated at', field: 'updatedDate', type: 'datetime', editable: 'never', sorting: true, filtering: true, align: 'center', hidden: false},
            {enabled: true, title: 'Text', field: '$.text', type: 'string', editable: 'always', sorting: true, filtering: true, align: 'center', hidden: false},
            {enabled: true, title: 'Number', field: '$.number', type: 'numeric', editable: 'always', sorting: true, filtering: true, align: 'center', hidden: false}
        ]);
    }

    const allowEnum = (rowData) => {
        return rowData.field != null && rowData.field.startsWith('$') && rowData.type === 'string';
    }

    const getEnumName = (rowData) => {
        if (allowEnum(rowData)) {
            const found = enums.find(en => en.id === rowData['enumId']);
            if (found)
                return found.name;
        }
        return '';
    }

    return (
        <div>
            <Tooltip title={'Configure columns'}>
                <Button
                    startIcon={<MoreHoriz/>}
                    onClick={handleClickOpen}
                >
                    {`[${props.configuration.length}]`}
                </Button>
            </Tooltip>
            <Dialog
                onClose={handleSave}
                aria-labelledby="customized-dialog-title"
                open={open}
                fullWidth={true}
                maxWidth='xl' //'xs' | 'sm' | 'md' | 'lg' | 'xl' | false
            >
                <DialogTitle id="customized-dialog-title" onClose={handleSave}>
                    {'Columns configuration'}
                </DialogTitle>
                <DialogContent dividers>
                    <MaterialTable
                        icons={getTableIcons()}
                        title={`Columns: ${props.name} ${props.description}`}
                        tableRef={tableRef}
                        columns={[
                            {title: '#', cellStyle: {width: '1%'}, render: (rowData) => rowData ? rowData.tableData.id + 1 : ''},
                            {title: 'Enabled', field: 'enabled', type: 'boolean', initialEditValue: true, cellStyle: {width: '1%'}},
                            {title: 'Hidden', field: 'hidden', type: 'boolean', initialEditValue: false, cellStyle: {width: '1%'}},
                            {title: 'Title', field: 'title', type: 'string', emptyValue: 'Name'},
                            {title: 'Source', field: 'field', type: 'string', emptyValue: '$.name'},
                            {
                                title: 'Type',
                                field: 'type',
                                lookup: {
                                    'string': 'String',
                                    'numeric': 'Numeric',
                                    'datetime': 'Datetime',
                                    'date': 'Date',
                                    'time': 'Time',
                                    'boolean': 'Boolean'
                                },
                                initialEditValue: 'string'
                            },
                            {
                                title: 'Align', field: 'align', type: 'string', editable: 'always',
                                lookup: {
                                    'center': 'Center',
                                    'inherit': 'Inherit',
                                    'justify': 'Justify',
                                    'left': 'Left',
                                    'right': 'Right'
                                }, initialEditValue: 'center'
                            },
                            {title: 'Format', field: 'format', type: 'string', editable: 'always', emptyValue: ''},
                            {title: 'Width', field: 'width', type: 'string', editable: 'always', emptyValue: ''},
                            {
                                title: 'Enum',
                                field: 'enumId',
                                render: rowData => getEnumName(rowData),
                                editComponent: props => allowEnum(props.rowData) ? (
                                    <SelectEnumDialog
                                        enums={enums}
                                        rowData={props.rowData}
                                        onChange={props.onChange}
                                    />
                                    ) : <div/>
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
                            {title: 'Sorting', field: 'sorting', type: 'boolean', initialEditValue: true, cellStyle: {width: '1%'}},
                            {title: 'Filtering', field: 'filtering', type: 'boolean', initialEditValue: true, cellStyle: {width: '1%'}}
                        ]}
                        data={data}
                        onChangeRowsPerPage={onChangeRowsPerPage}
                        options={{
                            paging: true,
                            pageSize: pageSize,
                            paginationType: 'stepped',
                            pageSizeOptions: getPageSizeOptionsOnDialog(),
                            actionsColumnIndex: -1,
                            sorting: true,
                            selection: false,
                            filtering: false,
                            padding: 'dense',
                            headerStyle: {backgroundColor: getTableHeaderBackgroundColor(theme)},
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
                                            severity: 'Item is not valid',
                                            title: '',
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
                                new Promise((resolve) => {
                                    setData(data.filter(column => column.tableData.id !== oldData.tableData.id))
                                    resolve();
                                }),
                        }}
                        actions={[
                            {
                                icon: () => <ExamplesIcon/>,
                                tooltip: 'Set example columns',
                                isFreeAction: true,
                                onClick: () => setDefaultColumns(),
                            },
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
                </DialogContent>
                <DialogActions/>
            </Dialog>
            <MessageBox
                config={messageBox}
                onClose={() => setMessageBox({...messageBox, open: false})}
            />
        </div>
    );
}