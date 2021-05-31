import React, {createRef, useEffect, useState} from 'react';
import {useTheme, withStyles} from '@material-ui/core/styles';
import Dialog from '@material-ui/core/Dialog';
import IconButton from '@material-ui/core/IconButton';
import MuiDialogTitle from '@material-ui/core/DialogTitle';
import MuiDialogContent from '@material-ui/core/DialogContent';
import MuiDialogActions from '@material-ui/core/DialogActions';
import CloseIcon from '@material-ui/icons/Close';
import Typography from '@material-ui/core/Typography';
import MaterialTable from 'material-table';
import LockedIcon from '@material-ui/icons/Lock';
import UnlockedIcon from '@material-ui/icons/LockOpen';
import DataHistoryPropertiesDiffDialog from './DataHistoryPropertiesDiffDialog';
import PropTypes from "prop-types";
import {createTagLookup} from "../../utils/JsonHelper";
import {getTableHeaderBackgroundColor} from "../../utils/MaterialTableHelper";

const styles = theme => ({
    root: {
        margin: 0,
        padding: theme.spacing(2),
    },
    container: {
        display: 'flex',
        flexWrap: 'wrap',
    },
    closeButton: {
        position: 'absolute',
        right: theme.spacing(1),
        top: theme.spacing(1)
    }
});

const DialogTitle = withStyles(styles)(props => {
    const {children, classes, onClose} = props;
    return (
        <MuiDialogTitle disableTypography className={classes.root}>
            <Typography variant="h6">{children}</Typography>
            {onClose ? (
                <IconButton aria-label="Close" className={classes.closeButton} onClick={onClose}>
                    <CloseIcon/>
                </IconButton>
            ) : null}
        </MuiDialogTitle>
    );
});

const DialogContent = withStyles(theme => ({
    root: {
        padding: theme.spacing(0),
    },
}))(MuiDialogContent);

const DialogActions = withStyles(theme => ({
    root: {
        margin: 0,
        padding: theme.spacing(1),
    },
}))(MuiDialogActions);

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
    const theme = useTheme();
    const [state, setState] = useState({bucket: null, dataRowId: null, history: [], open: false, columns: []});

    useEffect(() => {
        if (props.open === true) {
            const tagsLookup = createTagLookup(props.tags);
            const preparedColumns = [
                {title: 'Id', field: 'index'},
                {
                    title: 'Modified at', field: 'modified_at', type: 'datetime', editable: 'never',
                    render: rowData => <div>{rowData != null ? rowData.modified_at != null ? new Date(rowData.modified_at).toLocaleString() : null : null}</div>
                },
                {title: 'Modified by', field: 'modified_by', editable: 'never'},
                {title: 'Tag', field: 'tag_id', editable: 'never', lookup: tagsLookup},
                {
                    title: 'Reserved', field: 'reserved', editable: 'never',
                    render: rowData => <div>{rowData.reserved != null ? rowData.reserved ? <LockedIcon color="action"/> : <UnlockedIcon color="action"/> : ''}</div>
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

    return (
        <Dialog
            onClose={handleClose} // Enable this to close editor by clicking outside the dialog
            aria-labelledby="customized-dialog-title"
            open={state.open}
            fullWidth={true}
            maxWidth='lg'  //'xs' | 'sm' | 'md' | 'lg' | 'xl' | false
        >
            <DialogTitle id="customized-dialog-title" onClose={handleClose}>
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
            </DialogContent>
            <DialogActions/>
        </Dialog>
    );
}
