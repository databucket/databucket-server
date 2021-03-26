import React, {createRef, useState} from 'react';
import {withStyles} from '@material-ui/core/styles';
import Dialog from '@material-ui/core/Dialog';
import MuiDialogTitle from '@material-ui/core/DialogTitle';
import MuiDialogContent from '@material-ui/core/DialogContent';
import IconButton from '@material-ui/core/IconButton';
import CloseIcon from '@material-ui/icons/Done';
import Typography from '@material-ui/core/Typography';
import MoreHoriz from "@material-ui/icons/MoreHoriz";
import Tooltip from "@material-ui/core/Tooltip";
import {
    getDialogTableHeight,
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
import {useWindowDimension} from "../../utils/UseWindowDimension";

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


EditEnumDialog.propTypes = {
    name: PropTypes.string.isRequired,
    iconsEnabled: PropTypes.bool.isRequired,
    items: PropTypes.array.isRequired,
    onChange: PropTypes.func.isRequired
}

export default function EditEnumDialog(props) {

    const theme = useTheme();
    const [height] = useWindowDimension();
    const tableRef = createRef();
    const [messageBox, setMessageBox] = useState({open: false, severity: 'error', title: '', message: ''});
    const [open, setOpen] = useState(false);
    const [data, setData] = useState(props.items != null ? props.items : []);

    const getColumns = () => {
        let columnsArray = [];
        columnsArray.push({title: '#', cellStyle: {width: '1%'}, render: (rowData) => rowData ? rowData.tableData.id + 1 : ''});
        columnsArray.push({title: 'Value', field: 'value'});
        columnsArray.push({title: 'Text', field: 'text'});
        if (props.iconsEnabled)
            columnsArray.push({
                title: 'Icon', field: 'icon',
                render: rowData => <DynamicIcon iconName={rowData.icon}/>,
                editComponent: props => <EditIconDialog value={props.value} onChange={props.onChange}/>
            });
        return columnsArray;
    }

    const handleClickOpen = () => {
        setOpen(true);
    };

    const handleSave = () => {
        props.onChange(data.map(({value, text, icon}) => ({value, text, icon})));
        setOpen(false);
    }

    const isValid = (dataItem) => {
        let message = '';
        if (dataItem.value == null || dataItem.value.length === 0)
            message = "The value must not be empty"

        else if (dataItem.text == null || dataItem.text.length === 0)
            message = "The text must not be empty"

        if (message.length > 0)
            setMessageBox({open: true, severity: 'error', title: 'Error', message: message});

        return message.length === 0;
    }

    return (
        <div>
            <Tooltip title='Define items'>
                <Button
                    endIcon={<MoreHoriz/>}
                    onClick={handleClickOpen}
                >
                    {`${data.length}`}
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
                    {`Enum: ${props.name}`}
                </DialogTitle>
                <DialogContent dividers>
                    <MaterialTable
                        icons={getTableIcons()}
                        title={`Value list`}
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
                            maxBodyHeight: getDialogTableHeight(height, 30),
                            minBodyHeight: getDialogTableHeight(height, 30),
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
            </Dialog>
            <MessageBox
                config={messageBox}
                onClose={() => setMessageBox({...messageBox, open: false})}
            />
        </div>
    );
}