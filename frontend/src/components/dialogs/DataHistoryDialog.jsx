import React, {createRef, useEffect, useState} from 'react';
import {
    Dialog,
    DialogActions as MuiDialogActions,
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
    Lock as LockedIcon,
    LockOpen as UnlockedIcon
} from '@mui/icons-material';
import MaterialTable from 'material-table';
import DataHistoryPropertiesDiffDialog from './DataHistoryPropertiesDiffDialog';
import PropTypes from "prop-types";
import {createTagLookup} from "../../utils/JsonHelper";
import {
    getDeleteOptions,
    getTableHeaderBackgroundColor
} from "../../utils/MaterialTableHelper";
import {getClearDataHistoryByIdUrl} from "../../utils/UrlBuilder";
import {handleErrors} from "../../utils/FetchHelper";
import {MessageBox} from "../utils/MessageBox";

const PREFIX = 'DataHistoryDialog';

const classes = {
    container: `${PREFIX}-container`,
    clearHistoryButton: `${PREFIX}-clearHistoryButton`,
    closeButton: `${PREFIX}-closeButton`
};

const StyledDialog = styled(Dialog)(({theme}) => ({
    margin: 0,
    padding: theme.spacing(2),

    [`& .${classes.container}`]: {
        display: 'flex',
        flexWrap: 'wrap',
    },

    [`& .${classes.clearHistoryButton}`]: {
        position: 'absolute',
        right: theme.spacing(6),
        top: theme.spacing(1)
    },

    [`& .${classes.closeButton}`]: {
        position: 'absolute',
        right: theme.spacing(1),
        top: theme.spacing(1)
    }
}));

const DialogTitle = (({children, onClose, onClearDataHistory}) => {
    return (
        <MuiDialogTitle>
            <Typography variant="h6">{children}</Typography>
            <Tooltip id="clear-history" title="Clear data history">
                <IconButton
                    className={classes.clearHistoryButton}
                    onClick={onClearDataHistory}
                    color={"inherit"}
                    size="large">
                    <span className="material-icons">delete</span>
                </IconButton>
            </Tooltip>
            {onClose ? (
                <Tooltip id="close" title="Close">
                    <IconButton className={classes.closeButton} onClick={onClose} size="large">
                        <CloseIcon/>
                    </IconButton>
                </Tooltip>
            ) : null}
        </MuiDialogTitle>
    );
});

const DialogContent = MuiDialogContent;

const DialogActions = MuiDialogActions;

DataHistoryDialog.propTypes = {
    bucket: PropTypes.object,
    dataRowId: PropTypes.number,
    open: PropTypes.bool.isRequired,
    history: PropTypes.array,
    tags: PropTypes.array,
    onClose: PropTypes.func.isRequired
};

export default function DataHistoryDialog(props) {

    const tableRef = createRef();
    const [messageBox, setMessageBox] = useState({open: false, severity: 'error', title: '', message: ''});
    const theme = useTheme();
    const [state, setState] = useState({bucket: null, dataRowId: null, history: [], open: false, columns: []});

    useEffect(() => {
        if (props.open === true) {
            const tagsLookup = createTagLookup(props.tags);
            const preparedColumns = [
                {title: 'Id', field: 'index'},
                {
                    title: 'Modified at', field: 'modified_at', type: 'datetime', editable: 'never',
                    render: rowData =>
                        <div>{rowData != null ? rowData.modified_at != null ? new Date(rowData.modified_at).toLocaleString() : null : null}</div>
                },
                {title: 'Modified by', field: 'modified_by', editable: 'never'},
                {title: 'Tag', field: 'tag_id', editable: 'never', lookup: tagsLookup},
                {
                    title: 'Reserved', field: 'reserved', editable: 'never',
                    render: rowData => <div>{rowData.reserved != null ? rowData.reserved ?
                        <LockedIcon color="action"/> : <UnlockedIcon color="action"/> : ''}</div>
                },
                {
                    title: 'Properties', field: 'properties', editable: 'never',
                    render: rowData => <div>{rowData.properties != null ?
                        <DataHistoryPropertiesDiffDialog
                            bucket={props.bucket}
                            dataRowId={props.dataRowId}
                            history={props.history}
                            selectedRow={rowData}
                        /> : ''}</div>
                }
            ]

            if (props.history != null)
                for (let i = 0; i < props.history.length; i++)
                    props.history[i]['index'] = i + 1;

            setState({
                bucket: props.bucket,
                dataRowId: props.dataRowId,
                history: props.history,
                columns: preparedColumns,
                open: props.open
            });
        }
    }, [props.bucket, props.dataRowId, props.history, props.open]);

    const handleClose = () => {
        props.onClose();
        setState({...state, open: false});
    };

    const handleClearDataHistory = () => {
        fetch(getClearDataHistoryByIdUrl(props.bucket, props.dataRowId), getDeleteOptions())
            .then(handleErrors)
            .then(result => {
                setState({...state, history: []});
            })
            .catch(error => {
                setMessageBox({open: true, severity: 'error', title: 'Error', message: error});
            });
    }


    return (
        <StyledDialog
            onClose={handleClose} // Enable this to close editor by clicking outside the dialog
            aria-labelledby="customized-dialog-title"
            open={state.open}
            fullWidth={true}
            maxWidth='lg'  //'xs' | 'sm' | 'md' | 'lg' | 'xl' | false
        >
            <DialogTitle id="customized-dialog-title" onClose={handleClose} onClearDataHistory={handleClearDataHistory}>
                Data history [Id: {state.dataRowId}]
            </DialogTitle>
            <DialogContent dividers>
                <MaterialTable
                    tableRef={tableRef}
                    columns={state.columns}
                    data={state.history}
                    options={{
                        toolbar: false,
                        paging: false,
                        sorting: false,
                        search: false,
                        filtering: false,
                        padding: 'dense',
                        headerStyle: {position: 'sticky', top: 0, backgroundColor: getTableHeaderBackgroundColor(theme)}
                    }}
                    components={{
                        Container: props => <div {...props} />
                    }}
                />
                <MessageBox
                    config={messageBox}
                    onClose={() => setMessageBox({...messageBox, open: false})}
                />
            </DialogContent>
            <DialogActions/>
        </StyledDialog>
    );
}
