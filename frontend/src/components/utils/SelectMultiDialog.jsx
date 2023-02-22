import React, {createRef, useState} from 'react';
import { styled } from '@mui/material/styles';
import Dialog from '@mui/material/Dialog';
import MuiDialogTitle from '@mui/material/DialogTitle';
import MuiDialogContent from '@mui/material/DialogContent';
import IconButton from '@mui/material/IconButton';
import CloseIcon from '@mui/icons-material/Done';
import Typography from '@mui/material/Typography';
import MoreHoriz from "@mui/icons-material/MoreHoriz";
import Tooltip from "@mui/material/Tooltip";
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
import {useTheme} from '@mui/material/styles';
import {setSelectionItemsByIds} from "../../utils/JsonHelper";
import PropTypes from 'prop-types';
import Button from "@mui/material/Button";
import {useWindowDimension} from "./UseWindowDimension";
const PREFIX = 'SelectMultiDialog';

const classes = {
    root: `${PREFIX}-root`,
    root2: `${PREFIX}-root2`,
    closeButton: `${PREFIX}-closeButton`
};

const Root = styled('div')((
    {
        theme
    }
) => ({
    [`& .${classes.root2}`]: {
        margin: 0,
        padding: theme.spacing(2),
    },

    [`& .${classes.closeButton}`]: {
        position: 'absolute',
        right: theme.spacing(1),
        top: theme.spacing(1),
        color: theme.palette.grey[500],
    }
}));

const DialogTitle = ((props) => {
    const {children,  onClose, ...other} = props;
    return (
        <MuiDialogTitle disableTypography className={classes.root} {...other}>
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
        </MuiDialogTitle>
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
    const [height] = useWindowDimension();
    const [open, setOpen] = useState(false);
    const [data] = useState(setSelectionItemsByIds(props.data, props.ids));
    const tableRef = createRef();
    const [pageSize, setPageSize] = useState(getLastPageSizeOnDialog);
    const [selection, setSelection] = useState(data.filter(p => p.tableData != null && p.tableData.checked === true));

    const handleClickOpen = () => {
        setOpen(true);
    };

    const handleSave = () => {
        props.onChange(selection.map((item) => {return item.id}));
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
                <DialogContent
                    dividers
                    classes={{
                        root: classes.root
                    }}>
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
                            maxBodyHeight: getDialogTableHeight(height, 30),
                            minBodyHeight: getDialogTableHeight(height, 30),
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