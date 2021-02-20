import React, {createRef, useState} from 'react';
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
import {
    getPageSizeOptionsOnDialog,
    getTableHeaderBackgroundColor,
    getTableIcons,
    getTableRowBackgroundColor, moveDown, moveUp
} from "../../../utils/MaterialTableHelper";
import MaterialTable from "material-table";
import {useTheme} from '@material-ui/core/styles';
import PropTypes from 'prop-types';
import Button from "@material-ui/core/Button";
import DynamicIcon from "../../utils/DynamicIcon";
import EditIconDialog from "./EditIconDialog";
import ArrowDropDown from "@material-ui/icons/ArrowDropDown";
import ArrowDropUp from "@material-ui/icons/ArrowDropUp";
import {MessageBox} from "../../utils/MessageBox";

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


EditEnumDialog.propTypes = {
    name: PropTypes.string.isRequired,
    textValues: PropTypes.bool.isRequired,
    items: PropTypes.array.isRequired,
    onChange: PropTypes.func.isRequired
}

export default function EditEnumDialog(props) {

    const theme = useTheme();
    const tableRef = createRef();
    const [messageBox, setMessageBox] = useState({open: false, severity: 'error', title: '', message: ''});
    const [open, setOpen] = useState(false);
    const [data, setData] = useState(props.items != null ? props.items : []);

    const getColumns = () => {
        let columnsArray = [];
        columnsArray.push({title: '#', cellStyle: {width: '1%'}, render: (rowData) => rowData ? rowData.tableData.id + 1 : ''});
        columnsArray.push({title: 'Key', field: 'key'});
        if (props.textValues)
            columnsArray.push({title: 'Text', field: 'textValue'});
        else
            columnsArray.push({
                title: 'Icon', field: 'iconName',
                render: rowData => <DynamicIcon iconName={rowData.iconName}/>,
                editComponent: props => <EditIconDialog value={props.value} onChange={props.onChange}/>
            });
        return columnsArray;
    }

    const handleClickOpen = () => {
        setOpen(true);
    };

    const handleSave = () => {
        let dataClone = JSON.parse(JSON.stringify(data));
        for (let i = 0; i < dataClone.length; i++) {
            let col = dataClone[i];
            delete col['tableData'];
            delete col['sorting'];
            delete col['filtering'];
        }
        props.onChange(dataClone);
        setOpen(false);
    }

    const isValid = (dataItem) => {
        let message = '';
        if (dataItem.key == null || dataItem.key.length === 0)
            message = "The key must not be empty"

        else if (data.filter(item => item.key === dataItem.key).length > 0)
            message = "The key must be unique"

        if (message.length > 0)
            setMessageBox({open: true, severity: 'error', title: 'Error', message: message});

        return message.length === 0;
    }

    return (
        <div>
            <Tooltip title='Define items'>
                <Button
                    startIcon={<MoreHoriz/>}
                    onClick={handleClickOpen}
                >
                    {`[${data.length}]`}
                </Button>
            </Tooltip>
            <Dialog
                onClose={handleSave}
                aria-labelledby="customized-dialog-title"
                open={open}
                fullWidth={true}
                maxWidth='md' //'xs' | 'sm' | 'md' | 'lg' | 'xl' | false
            >
                <DialogTitle id="customized-dialog-title" onClose={handleSave}>
                    {`Values configuration`}
                </DialogTitle>
                <DialogContent dividers>
                    <MaterialTable
                        icons={getTableIcons()}
                        title={`Column: ${props.name}`}
                        tableRef={tableRef}
                        columns={getColumns()}
                        data={data}
                        options={{
                            paging: true,
                            pageSize: 10,
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
                                    if (isValid(newData)) {
                                        setData([...data, newData]);
                                        resolve();
                                    } else
                                        reject();
                                }),
                            onRowUpdate: (newData, oldData) =>
                                new Promise((resolve, reject) => {
                                    if (isValid(newData)) {
                                        const updated = data.map(item => {
                                            if (item.tableData.id === oldData.tableData.id)
                                                return newData;
                                            return item;
                                        });
                                        setData(updated);
                                        resolve();
                                    } else
                                        reject();
                                }),
                            onRowDelete: oldData =>
                                new Promise((resolve) => {
                                    setData(data.filter(item => item.tableData.id !== oldData.tableData.id));
                                    resolve();
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