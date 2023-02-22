import React, {createRef, useState} from 'react';
import withStyles from '@mui/styles/withStyles';
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
    getTableHeaderBackgroundColor, getTableRowBackgroundColor
} from "../../utils/MaterialTableHelper";
import MaterialTable from "material-table";
import {
    getLastPageSizeOnDialog,
    setLastPageSizeOnDialog
} from "../../utils/ConfigurationStorage";
import {useTheme} from '@mui/material/styles';
import PropTypes from 'prop-types';
import Button from "@mui/material/Button";
import RadioChecked from "@mui/icons-material/RadioButtonChecked";
import RadioUnchecked from "@mui/icons-material/RadioButtonUnchecked";
import {useWindowDimension} from "./UseWindowDimension";
// import TableIcons from "./TableIcons";

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

const DialogContent = withStyles((theme) => ({
    root: {
        padding: theme.spacing(0),
    },
}))(MuiDialogContent);

SelectSingleDialog.propTypes = {
    columns: PropTypes.array.isRequired,
    data: PropTypes.array.isRequired,
    id: PropTypes.number.isRequired,
    tooltipTitle: PropTypes.string.isRequired,
    dialogTitle: PropTypes.string.isRequired,
    tableTitle: PropTypes.string.isRequired,
    maxWidth: PropTypes.string.isRequired,
    onChange: PropTypes.func.isRequired
}

export default function SelectSingleDialog(props) {

    const theme = useTheme();
    const [height] = useWindowDimension();
    const [open, setOpen] = useState(false);
    const [data] = useState(props.data);
    const tableRef = createRef();
    const [pageSize, setPageSize] = useState(getLastPageSizeOnDialog);

    const handleClickOpen = () => {
        setOpen(true);
    };

    const handleSave = () => {
        setOpen(false);
    }

    const onChangeRowsPerPage = (pageSize) => {
        setPageSize(pageSize);
        setLastPageSizeOnDialog(pageSize);
    }

    const getSelectionName = () => {
        if (props.id > 0)
            return data.find(item => item.id === props.id).name;
        else
            return '';
    }

    const getColumnSelection = () => {
        return {
            title: '',
            field: '',
            type: 'numeric',
            editable: 'never',
            filtering: false,
            cellStyle: {width: '1%'},
            render: rowData => (rowData['id'] === props.id ? <RadioChecked color={'secondary'} fontSize={'small'}/> : <RadioUnchecked fontSize={'small'}/>)
        };
    };

    return (
        <div>
            <Tooltip title={props.tooltipTitle}>
                <Button
                    endIcon={<MoreHoriz/>}
                    onClick={handleClickOpen}
                    style={{textTransform: 'none'}}
                >
                    {getSelectionName()}
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
                        columns={[getColumnSelection(), ...props.columns]}
                        data={data}
                        onChangeRowsPerPage={onChangeRowsPerPage}
                        onRowClick={(event, rowData) => props.onChange(rowData.id !== props.id ? rowData.id : -1)}
                        options={{
                            paging: true,
                            pageSize: pageSize,
                            paginationType: 'stepped',
                            pageSizeOptions: getPageSizeOptionsOnDialog(),
                            actionsColumnIndex: -1,
                            sorting: false,
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
                    />
                </DialogContent>
            </Dialog>
        </div>
    );
}