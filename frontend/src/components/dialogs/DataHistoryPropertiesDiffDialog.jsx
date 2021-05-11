import React, {useEffect, useState} from 'react'
import {withStyles} from '@material-ui/core/styles';
import Dialog from '@material-ui/core/Dialog';
import Tooltip from '@material-ui/core/Tooltip';
import IconButton from '@material-ui/core/IconButton';
import MuiDialogTitle from '@material-ui/core/DialogTitle';
import MuiDialogContent from '@material-ui/core/DialogContent';
import CloseIcon from '@material-ui/icons/Close';
import Typography from '@material-ui/core/Typography';
import CompareIcon from '@material-ui/icons/YoutubeSearchedFor';
import {getDataHistoryPropertiesUrl} from "../../utils/UrlBuilder";
import {getGetOptions} from "../../utils/MaterialTableHelper";
import {handleErrors} from "../../utils/FetchHelper";
import {MessageBox} from "../utils/MessageBox";

const styles = theme => ({
    root: {
        margin: 0,
        padding: theme.spacing(2),
    },
    closeButton: {
        position: 'absolute',
        right: theme.spacing(1),
        top: theme.spacing(1)
    },
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

export default function DataHistoryPropertiesDiffDialog(props) {

    const [messageBox, setMessageBox] = useState({open: false, severity: 'error', title: '', message: ''});
    const [state, setState] = useState({
        bucket: null,
        dataRowId: null,
        oldValue: '',
        newValue: '',
        history: null,
        selectedRow: null,
        open: false
    });

    useEffect(() => {
        setState({
            ...state,
            bucket: props.bucket,
            dataRowId: props.dataRowId,
            history: props.history,
            selectedRow: props.selectedRow
        });
    }, [props.bucket, props.dataRowId, props.history, props.selectedRow]);

    const handleClickOpen = () => {
        console.log("handleClickOpen");
        console.log(state);
        let oValue = '';
        let nValue = '';
        const previousId = getPreviousId(state.history, state.selectedRow);

        let resultOk = true;
        fetch(getDataHistoryPropertiesUrl(state.bucket, state.dataRowId, previousId, state.selectedRow.id), getGetOptions())
            .then(handleErrors)
            .catch(error => {
                setMessageBox({open: true, severity: 'error', title: 'Error', message: error});
                resultOk = false;
            })
            .then(result => {
                if (resultOk) {
                    if (previousId > 0) {
                        oValue = result.filter(d => (d.id === previousId))[0].properties;
                        oValue = JSON.stringify(oValue, null, 2);
                    }

                    nValue = result.filter(d => (d.id === state.selectedRow.id))[0].properties;
                    nValue = JSON.stringify(nValue, null, 2);
                    setState({
                        ...state,
                        oldValue: oValue,
                        newValue: nValue,
                        open: true,
                    });
                }
            });
    };

    const getPreviousId = (history, row) => {
        let result = -1;
        for (let i = 0; i < history.length; i++) {
            let obj = history[i];
            if (obj.hasOwnProperty('properties') && obj.properties === true && obj.id !== row.id) {
                result = obj.id;
            }
            if (obj.id === row.id)
                return result;
        }
    }

    const handleClose = () => {
        setState({...state, open: false});
    };

    return (
        <div>
            <Tooltip title='Show changes'>
                <IconButton
                    onClick={handleClickOpen}
                    color="default"
                    size="small"
                >
                    <CompareIcon/>
                </IconButton>
            </Tooltip>
            <Dialog
                onClose={handleClose} // Enable this to close editor by clicking outside the dialog
                aria-labelledby="customized-dialog-title"
                open={state.open}
                fullWidth={true}
                maxWidth='xl' //'xs' | 'sm' | 'md' | 'lg' | 'xl' | false
            >
                <DialogTitle id="customized-dialog-title" onClose={handleClose}>
                    Properties difference
                </DialogTitle>
                <DialogContent dividers>
                    {/*<ReactDiffViewer*/}
                    {/*    oldValue={state.oldValue}*/}
                    {/*    newValue={state.newValue}*/}
                    {/*    splitView={true}*/}
                    {/*    disableWordDiff={false}*/}
                    {/*/>*/}
                    {/*Nowa porównywarka:*/}
                    {/*https://www.npmjs.com/package/react-diff-view*/}
                </DialogContent>
                <MessageBox
                    config={messageBox}
                    onClose={() => setMessageBox({...messageBox, open: false})}
                />
            </Dialog>
        </div>
    );
}
