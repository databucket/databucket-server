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
import {
    Close as CloseIcon,
    MoreHoriz,
    RadioButtonChecked as RadioChecked,
    RadioButtonUnchecked as RadioUnchecked
} from '@mui/icons-material';
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
import PropTypes from 'prop-types';

const PREFIX = 'SelectSingleDialog';

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
    const {children, onClose, ...other} = props;
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
        if (props.id > 0) {
            return data.find(item => item.id === props.id).name;
        } else {
            return '';
        }
    }

    const getColumnSelection = () => {
        return {
            title: '',
            field: '',
            type: 'numeric',
            editable: 'never',
            filtering: false,
            cellStyle: {width: '1%'},
            render: rowData => (rowData['id'] === props.id ? <RadioChecked
                    color={'secondary'} fontSize={'small'}/> :
                <RadioUnchecked fontSize={'small'}/>)
        };
    };

    return (
        <Root>
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
                <DialogContent
                    dividers
                    classes={{
                        root: classes.root
                    }}>
                    <MaterialTable
                        title={props.tableTitle != null ? props.tableTitle
                            : '-'}
                        tableRef={tableRef}
                        columns={[getColumnSelection(), ...props.columns]}
                        data={data}
                        onChangeRowsPerPage={onChangeRowsPerPage}
                        onRowClick={(event, rowData) => props.onChange(
                            rowData.id !== props.id ? rowData.id : -1)}
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
                            Container: props => <div {...props} />
                        }}
                    />
                </DialogContent>
            </Dialog>
        </Root>
    );
}
