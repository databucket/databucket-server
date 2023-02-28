import React, {createRef, useState} from 'react';
import {styled, useTheme} from '@mui/material/styles';
import Dialog from '@mui/material/Dialog';
import MuiDialogTitle from '@mui/material/DialogTitle';
import MuiDialogContent from '@mui/material/DialogContent';
import IconButton from '@mui/material/IconButton';
import CloseIcon from '@mui/icons-material/Done';
import MoreHoriz from "@mui/icons-material/MoreHoriz";
import Tooltip from "@mui/material/Tooltip";
import {
    getDialogTableHeight,
    getPageSizeOptionsOnDialog,
    getTableHeaderBackgroundColor,
    getTableRowBackgroundColor,
    moveDown,
    moveUp
} from "../../utils/MaterialTableHelper";
import MaterialTable from "material-table";
import PropTypes from 'prop-types';
import Button from "@mui/material/Button";
import EditIconDialog from "./SelectIconDialog";
import ArrowDropDown from "@mui/icons-material/ArrowDropDown";
import ArrowDropUp from "@mui/icons-material/ArrowDropUp";
import {MessageBox} from "../utils/MessageBox";
import {useWindowDimension} from "../utils/UseWindowDimension";
import StyledIcon from "../utils/StyledIcon";

const PREFIX = 'EditEnumDialog';

const classes = {
    closeButton: `${PREFIX}-closeButton`
};

const Root = styled('div')(({theme}) => ({
    margin: 0,
    padding: theme.spacing(2)
}));


const DialogTitle = ((props) => {
    const {children, onClose, ...other} = props;
    return (
        <MuiDialogTitle {...other}>
            {children}
            {onClose ? (
                <IconButton
                    aria-label="close"
                    sx={{
                        position: 'absolute',
                        right: 8,
                        top: 8,
                        color: (theme) => theme.palette.grey[500],
                    }}
                    onClick={onClose}
                    size="large">
                    <CloseIcon/>
                </IconButton>
            ) : null}
        </MuiDialogTitle>
    );
});

const DialogContent = MuiDialogContent;

const EditComponent = props => <EditIconDialog icon={props.value} onChange={props.onChange}/>

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
        columnsArray.push({
            title: '#',
            cellStyle: {width: '1%'},
            render: (rowData) => rowData ? rowData.tableData.id + 1 : ''
        });
        columnsArray.push({title: 'Value', field: 'value'});
        columnsArray.push({title: 'Text', field: 'text'});
        if (props.iconsEnabled)
            columnsArray.push({
                title: 'Icon',
                field: 'icon',
                initialEditValue: {name: "help", color: null, svg: null},
                render: rowData => <StyledIcon iconName={rowData.icon.name}
                                               iconColor={rowData.icon.color}
                                               iconSvg={rowData.icon.svg}/>,
                editComponent: EditComponent
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
        <Root>
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
                <DialogContent
                    dividers
                    classes={{
                        root: classes.root
                    }}>
                    <MaterialTable
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
        </Root>
    );
}
