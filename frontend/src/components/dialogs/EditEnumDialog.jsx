import React, {createRef, useState} from 'react';
import {
    Button,
    Dialog,
    DialogActions,
    DialogContent as MuiDialogContent,
    DialogTitle as MuiDialogTitle,
    IconButton,
    Menu,
    MenuItem,
    styled,
    Tooltip,
    Typography,
    useTheme
} from '@mui/material';
import {
    Close as CloseIcon,
    MoreHoriz,
    UnfoldMore
} from '@mui/icons-material';
import {
    getDialogTableHeight,
    getTableHeaderBackgroundColor,
    getTableRowBackgroundColor,
    movedRow,
    removeRow,
    replaceRow,
    sortedRows,
    withoutHeaderSorting
} from "../../utils/MaterialTableHelper";
import MaterialTable from '@material-table/core';
import PropTypes from 'prop-types';
import EditIconDialog from "./SelectIconDialog";
import {MessageBox} from "../utils/MessageBox";
import StyledIcon from "../utils/StyledIcon";
import DraggableBodyRow from "../utils/DraggableBodyRow";
import TableContainer from "../utils/TableContainer";

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

const DialogContent = styled(MuiDialogContent)({
    padding: 0,
});


const EditComponent = props => <EditIconDialog icon={props.value}
                                               onChange={props.onChange}/>

EditEnumDialog.propTypes = {
    name: PropTypes.string.isRequired,
    iconsEnabled: PropTypes.bool.isRequired,
    items: PropTypes.array.isRequired,
    onChange: PropTypes.func.isRequired
}

export default function EditEnumDialog(props) {

    const theme = useTheme();
    const tableRef = createRef();
    const [messageBox, setMessageBox] = useState(
        {open: false, severity: 'error', title: '', message: ''});
    const [open, setOpen] = useState(false);
    const [data, setData] = useState(props.items != null ? props.items : []);
    // the Sort menu: sorts the items for good (the order of the list in the data); opened at the position of the
    // button, because material-table renders its actions again and the button element itself doesn't stay
    const [sortMenuPosition, setSortMenuPosition] = useState(null);

    const openSortMenu = event => {
        const rect = event.currentTarget.getBoundingClientRect();
        setSortMenuPosition({top: rect.bottom, left: rect.left});
    }

    const sortItems = (field, descending) => {
        setData(sortedRows(data, field, descending));
        setSortMenuPosition(null);
    }

    const getColumns = () => {
        let columnsArray = [];
        columnsArray.push({
            title: '#',
            cellStyle: {width: '1%'},
            render: (rowData) => rowData ? rowData.tableData.id + 1 : ''
        });
        columnsArray.push({title: 'Value', field: 'value'});
        columnsArray.push({title: 'Text', field: 'text'});
        if (props.iconsEnabled) {
            columnsArray.push({
                title: 'Icon',
                field: 'icon',
                initialEditValue: {name: "help", color: null, svg: null},
                render: rowData => <StyledIcon iconName={rowData.icon.name}
                                               iconColor={rowData.icon.color}
                                               iconSvg={rowData.icon.svg}/>,
                editComponent: EditComponent
            });
        }
        return withoutHeaderSorting(columnsArray);
    }

    const handleClickOpen = () => {
        setOpen(true);
    };

    const handleSave = () => {
        props.onChange(
            data.map(({value, text, icon}) => ({value, text, icon})));
        setOpen(false);
    }

    const isValid = (dataItem) => {
        let message = '';
        if (dataItem.value == null || dataItem.value.length === 0) {
            message = "The value must not be empty"
        } else if (dataItem.text == null || dataItem.text.length === 0) {
            message = "The text must not be empty"
        }

        if (message.length > 0) {
            setMessageBox({
                open: true,
                severity: 'error',
                title: 'Error',
                message: message
            });
        }

        return message.length === 0;
    }

    return (
        <Root>
            <Tooltip title='Define items'>
                <Button
                    endIcon={<MoreHoriz/>}
                    onClick={handleClickOpen}
                    color={'inherit'}
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
                            // one list without pages, so that any item can be dragged anywhere
                            paging: false,
                            actionsColumnIndex: -1,
                            selection: false,
                            filtering: false,
                            padding: 'dense',
                            headerStyle: {
                                backgroundColor: getTableHeaderBackgroundColor(
                                    theme)
                            },
                            maxBodyHeight: getDialogTableHeight(30),
                            minBodyHeight: getDialogTableHeight(30),
                            rowStyle: rowData => ({
                                backgroundColor: getTableRowBackgroundColor(
                                    rowData, theme)
                            })
                        }}
                        components={{
                            Container: TableContainer,
                            Row: props => <DraggableBodyRow {...props}
                                                            onMoveRow={(from, to) => setData(movedRow(data, from, to))}/>
                        }}
                        editable={{
                            onRowAdd: newData =>
                                new Promise((resolve, reject) => {
                                    if (isValid(newData)) {
                                        setData([...data, newData]);
                                        resolve();
                                    } else {
                                        reject();
                                    }
                                }),
                            onRowUpdate: (newData, oldData) =>
                                new Promise((resolve, reject) => {
                                    if (isValid(newData)) {
                                        setData(replaceRow(data, oldData, newData));
                                        resolve();
                                    } else {
                                        reject();
                                    }
                                }),
                            onRowDelete: oldData =>
                                new Promise((resolve) => {
                                    setData(removeRow(data, oldData));
                                    resolve();
                                }),
                        }}
                        actions={[
                            {
                                icon: () => <UnfoldMore/>,
                                tooltip: 'Sort items',
                                isFreeAction: true,
                                disabled: data.length < 2,
                                onClick: openSortMenu
                            }
                        ]}
                    />
                    <Menu
                        anchorReference='anchorPosition'
                        anchorPosition={sortMenuPosition ?? undefined}
                        open={sortMenuPosition != null}
                        onClose={() => setSortMenuPosition(null)}
                    >
                        <MenuItem onClick={() => sortItems('value', false)}>Value: A → Z</MenuItem>
                        <MenuItem onClick={() => sortItems('value', true)}>Value: Z → A</MenuItem>
                        <MenuItem onClick={() => sortItems('text', false)}>Text: A → Z</MenuItem>
                        <MenuItem onClick={() => sortItems('text', true)}>Text: Z → A</MenuItem>
                    </Menu>
                </DialogContent>
                {/* the footer closes the list (no pagination any more); the height of the former pagination, which
                    getDialogTableHeight leaves for it */}
                <DialogActions sx={{minHeight: 52, justifyContent: 'flex-start', px: 2}}>
                    <Typography variant='body2' color='text.secondary'>
                        {data.length === 1 ? '1 item' : `${data.length} items`}
                    </Typography>
                </DialogActions>
            </Dialog>
            <MessageBox
                config={messageBox}
                onClose={() => setMessageBox({...messageBox, open: false})}
            />
        </Root>
    );
}
