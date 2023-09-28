import React, {useEffect, useState} from 'react'
import {
    Dialog,
    DialogContent as MuiDialogContent,
    DialogTitle as MuiDialogTitle,
    IconButton,
    styled,
    Tooltip,
    useTheme
} from '@mui/material';
import {
    Close as CloseIcon,
    YoutubeSearchedFor as CompareIcon
} from '@mui/icons-material';
import {getDataHistoryPropertiesUrl} from "../../utils/UrlBuilder";
import {getGetOptions} from "../../utils/MaterialTableHelper";
import {handleErrors} from "../../utils/FetchHelper";
import {MessageBox} from "../utils/MessageBox";
import ReactDiffViewer from "react-diff-viewer-continued";

const PREFIX = 'DataHistoryPropertiesDiffDialog';

const classes = {
    closeButton: `${PREFIX}-closeButton`
};

const StyledDialogTitle = styled(MuiDialogTitle)(({theme}) => ({
    margin: 0,
    padding: theme.spacing(2),

    [`& .${classes.closeButton}`]: {
        position: 'absolute',
        right: theme.spacing(1),
        top: theme.spacing(1)
    }
}));

const DialogTitle = (({children, onClose}) => {
    return (
        <StyledDialogTitle>
            {children}
            {onClose ? (
                <IconButton
                    aria-label="Close"
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

export default function DataHistoryPropertiesDiffDialog(props) {

    const theme = useTheme();
    const [messageBox, setMessageBox] = useState(
        {open: false, severity: 'error', title: '', message: ''});
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
        let oValue = '';
        let nValue = '';
        const previousId = getPreviousId(state.history, state.selectedRow);

        let resultOk = true;
        fetch(getDataHistoryPropertiesUrl(state.bucket, state.dataRowId,
            previousId, state.selectedRow.id), getGetOptions())
        .then(handleErrors)
        .catch(error => {
            setMessageBox({
                open: true,
                severity: 'error',
                title: 'Error',
                message: error
            });
            resultOk = false;
        })
        .then(result => {
            if (resultOk) {
                if (previousId > 0) {
                    oValue = result.filter(
                        d => (d.id === previousId))[0].properties;
                    oValue = JSON.stringify(oValue, null, 2);
                }

                nValue = result.filter(
                    d => (d.id === state.selectedRow.id))[0].properties;
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
        for (const element of history) {
            let obj = element;
            if (obj.hasOwnProperty('properties') && obj.properties === true
                && obj.id !== row.id) {
                result = obj.id;
            }
            if (obj.id === row.id) {
                return result;
            }
        }
    }

    const handleClose = () => {
        setState({...state, open: false});
    };

    return (
        <>
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
                    <ReactDiffViewer
                        useDarkTheme={theme.palette.mode === 'dark'}
                        oldValue={state.oldValue}
                        newValue={state.newValue}
                        splitView={true}
                        disableWordDiff={false}
                    />
                </DialogContent>
                <MessageBox
                    config={messageBox}
                    onClose={() => setMessageBox({...messageBox, open: false})}
                />
            </Dialog>
        </>
    );
}
