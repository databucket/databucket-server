import React, {createRef, useState} from 'react';
import {
    Button,
    Dialog,
    DialogContent as MuiDialogContent,
    DialogTitle as MuiDialogTitle,
    IconButton,
    styled,
    Tooltip,
    Typography,
    useTheme
} from '@mui/material';
import {Close as CloseIcon, MoreHoriz} from '@mui/icons-material';
import {
    getDialogTableHeight,
    getPageSizeOptionsOnDialog,
    getTableHeaderBackgroundColor,
    getTableRowBackgroundColor
} from "../../utils/MaterialTableHelper";
import MaterialTable from "material-table";
import {
    getLastPageSizeOnDialog,
    setLastPageSizeOnDialog
} from "../../utils/ConfigurationStorage";
import {setSelectionItemsByIds} from "../../utils/JsonHelper";
import PropTypes from 'prop-types';

const PREFIX = 'SelectMultiDialog';

const classes = {
    closeButton: `${PREFIX}-closeButton`
};

const Root = styled('div')(({theme}) => ({
    padding: theme.spacing(0),
}));
const StyledDialogTitle = styled(MuiDialogTitle)(({theme}) => ({
    margin: 0,
    padding: theme.spacing(2),

    [`& .${classes.closeButton}`]: {
        position: 'absolute',
        right: theme.spacing(1),
        top: theme.spacing(1),
        color: theme.palette.grey[500],
    }
}));

const DialogTitle = ((props) => {
    const {children, onClose, ...other} = props;
    return (
        <StyledDialogTitle disableTypography {...other}>
            <Typography variant="h6">{children}</Typography>
            {onClose ? (
                <IconButton
                    aria-label="close"
                    className={classes.closeButton}
                    onClick={onClose}
                    size="large">
                    <CloseIcon/>
                </IconButton>
            ) : null}
        </StyledDialogTitle>
    );
});

const DialogContent = MuiDialogContent;

SelectMultiDialog.propTypes = {
    columns: PropTypes.array.isRequired,
    data: PropTypes.array.isRequired,
    ids: PropTypes.arrayOf(PropTypes.number).isRequired,
    tooltipTitle: PropTypes.string.isRequired,
    dialogTitle: PropTypes.string.isRequired,
    tableTitle: PropTypes.string.isRequired,
    maxWidth: PropTypes.string.isRequired,
    onChange: PropTypes.func.isRequired
}

export default function SelectMultiDialog(props) {

    const theme = useTheme();
    const [open, setOpen] = useState(false);
    const [data] = useState(setSelectionItemsByIds(props.data, props.ids));
    const tableRef = createRef();
    const [pageSize, setPageSize] = useState(getLastPageSizeOnDialog);
    const [selection, setSelection] = useState(data.filter(p => p.tableData != null && p.tableData.checked === true));

    const handleClickOpen = () => {
        setOpen(true);
    };

    const handleSave = () => {
        props.onChange(selection.map((item) => {
            return item.id
        }));
        setOpen(false);
    }

    const onChangeRowsPerPage = (pageSize) => {
        setPageSize(pageSize);
        setLastPageSizeOnDialog(pageSize);
    }

    return (
        <Root>
            <Tooltip title={props.tooltipTitle}>
                <Button
                    endIcon={<MoreHoriz/>}
                    onClick={handleClickOpen}
                    style={{textTransform: 'none'}}
                >
                    {`${selection.length}`}
                </Button>
            </Tooltip>
            <Dialog
                onClose={handleSave}
                aria-labelledby="customized-dialog-title"
                open={open}
                fullWidth={true}
                maxWidth={props.maxWidth}
            >
                <DialogTitle id="customized-dialog-title" onClose={handleSave}>
                    {props.dialogTitle}
                </DialogTitle>
                <DialogContent dividers>
                    <MaterialTable
                        title={props.tableTitle != null ? props.tableTitle : '-'}
                        tableRef={tableRef}
                        columns={props.columns}
                        data={data}
                        onChangeRowsPerPage={onChangeRowsPerPage}
                        onSelectionChange={rows => setSelection(rows)}
                        options={{
                            paging: true,
                            pageSize: pageSize,
                            paginationType: 'stepped',
                            pageSizeOptions: getPageSizeOptionsOnDialog(),
                            actionsColumnIndex: -1,
                            sorting: false,
                            selection: true,
                            filtering: false,
                            padding: 'dense',
                            headerStyle: {backgroundColor: getTableHeaderBackgroundColor(theme)},
                            maxBodyHeight: getDialogTableHeight(30),
                            minBodyHeight: getDialogTableHeight(30),
                            rowStyle: rowData => ({backgroundColor: getTableRowBackgroundColor(rowData, theme)})
                        }}
                        components={{
                            Container: props => <div {...props} />
                        }}
                    />
                </DialogContent>
            </Dialog>
        </Root>
    );
}
