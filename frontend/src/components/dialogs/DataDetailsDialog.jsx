import React, {createRef, useEffect, useState} from 'react';
import {makeStyles, useTheme, withStyles} from '@material-ui/core/styles';
import PropTypes from 'prop-types';
import MaterialTable from 'material-table';
import Button from '@material-ui/core/Button';
import Dialog from '@material-ui/core/Dialog';
import IconButton from '@material-ui/core/IconButton';
import MuiDialogTitle from '@material-ui/core/DialogTitle';
import MuiDialogContent from '@material-ui/core/DialogContent';
import MuiDialogActions from '@material-ui/core/DialogActions';
import JSONInput from 'react-json-editor-ajrm/index';
import locale from 'react-json-editor-ajrm/locale/en';
import CloseIcon from '@material-ui/icons/Close';
import Typography from '@material-ui/core/Typography';
import {Divider} from '@material-ui/core';
import {createTagLookup} from "../../utils/JsonHelper";
import {getTableHeaderBackgroundColor, getTableRowBackgroundColor} from "../../utils/MaterialTableHelper";
import {getDataDetailsDialogSize, setDataDetailsDialogSize} from "../../utils/ConfigurationStorage";

const titleStyles = theme => ({
    root: {
        margin: 0,
        padding: theme.spacing(2)
    },
    container: {
        display: 'flex',
        flexWrap: 'wrap',
    },
    closeButton: {
        position: 'absolute',
        right: theme.spacing(1),
        top: theme.spacing(1)
    },
    smallerButton: {
        position: 'absolute',
        right: theme.spacing(15),
        top: theme.spacing(1)
    },
    largerButton: {
        position: 'absolute',
        right: theme.spacing(10),
        top: theme.spacing(1)
    }
});

const DialogTitle = withStyles(titleStyles)(props => {
    const {children, classes, onClose, onMakeDialogSmaller, onMakeDialogLarger} = props;
    return (
        <MuiDialogTitle disableTypography className={classes.root}>
            <Typography variant="h6">{children}</Typography>
            <IconButton aria-label="Smaller" className={classes.smallerButton} onClick={onMakeDialogSmaller} color={"inherit"}>
                <span className="material-icons">fullscreen_exit</span>
            </IconButton>
            <IconButton aria-label="Larger" className={classes.largerButton} onClick={onMakeDialogLarger} color={"inherit"}>
                <span className="material-icons">fullscreen</span>
            </IconButton>
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

const useStyles = makeStyles(() => ({
    dialogPaper: {
        minHeight: '98vh',
    }
}));

DataDetailsDialog.propTypes = {
    open: PropTypes.bool.isRequired,
    dataRow: PropTypes.object,
    tags: PropTypes.array,
    onChange: PropTypes.func.isRequired
};

export default function DataDetailsDialog(props) {

    const theme = useTheme();
    const classes = useStyles();
    const tableRef = createRef();
    const [state, setState] = useState({dataRow: null, open: false, changed: false, valid: true, changedProperties: null});
    const tagsLookup = createTagLookup(props.tags);
    const [dialogSize, setDialogSize] = useState(false);

    useEffect(() => {
        setDialogSize(getDataDetailsDialogSize());
    }, []);

    useEffect(() => {
        setState({...state, dataRow: props.dataRow, open: props.open});
    }, [props.dataRow, props.open]);

    const handleChanged = (contentValues) => {
        setState({...state, valid: contentValues.error === false, changed: true, changedProperties: contentValues.jsObject});
    };

    const handleSave = () => {
        setState({...state, open: false, changed: false});
        props.onChange({...state.dataRow, properties: state.changedProperties}, true);
    };

    const handleClose = () => {
        props.onChange(null, false);
        setState({...state, open: false, changed: false});
    };

    const onMakeDialogSmaller = () => {
        if (dialogSize === 'lg') {
            setDialogSize('md');
            setDataDetailsDialogSize('md');
        } else if (dialogSize === 'xl') {
            setDialogSize('lg');
            setDataDetailsDialogSize('lg');
        } else if (dialogSize === true) {
            setDialogSize('xl');
            setDataDetailsDialogSize('xl');
        }
    }

    const onMakeDialogLarger = () => {
        if (dialogSize === 'md') {
            setDialogSize('lg');
            setDataDetailsDialogSize('lg');
        } else if (dialogSize === 'lg') {
            setDialogSize('xl');
            setDataDetailsDialogSize('xl');
        } else if (dialogSize === 'xl') {
            setDialogSize(true);
            setDataDetailsDialogSize(true);
        }
    }

    return (
        <Dialog
            onClose={handleClose} // Enable this to close editor by clicking outside the dialog
            aria-labelledby="customized-dialog-title"
            classes={{paper: classes.dialogPaper}}
            open={state.open}
            fullWidth={true}
            maxWidth={dialogSize}  //'xs' | 'sm' | 'md' | 'lg' | 'xl' | false
        >
            <DialogTitle
                id="customized-dialog-title"
                onClose={handleClose}
                onMakeDialogSmaller={onMakeDialogSmaller}
                onMakeDialogLarger={onMakeDialogLarger}
            >
                Data details
            </DialogTitle>
            <Divider/>
            <MaterialTable
                tableRef={tableRef}
                columns={[
                    {title: 'Id', field: 'id', type: 'numeric'},
                    {title: 'Tag', field: 'tagId', type: 'numeric', lookup: tagsLookup},
                    {title: 'Reserved', field: 'reserved', type: 'boolean'},
                    {title: 'Owner', field: 'owner', type: 'string'},
                    {title: 'Created at', field: 'createdAt', type: 'datetime'},
                    {title: 'Created by', field: 'createdBy', type: 'string'},
                    {title: 'Modified at', field: 'modifiedAt', type: 'datetime'},
                    {title: 'Modified by', field: 'modifiedBy', type: 'string'}
                ]}
                data={[state.dataRow]}
                options={{
                    paging: false,
                    toolbar: false,
                    actionsColumnIndex: -1,
                    sorting: false,
                    search: false,
                    filtering: false,
                    padding: 'dense',
                    headerStyle: {position: 'sticky', top: 0, backgroundColor: getTableHeaderBackgroundColor(theme)},
                    rowStyle: rowData => ({backgroundColor: getTableRowBackgroundColor(rowData, theme)})
                }}
                components={{
                    Container: props => <div {...props} />
                }}
            />
            <DialogContent>
                <JSONInput
                    id='json_editor'
                    placeholder={state.dataRow !== null ? state.dataRow.properties : null}
                    theme={theme.palette.type === 'light' ? "light_mitsuketa_tribute" : "dark_mitsuketa_tribute"}
                    locale={locale}
                    style={{body: {fontSize: 'large', fontWeight: 'bold'}, errorMessage: {fontSize: 'large'}}}
                    width="100%"
                    height="100%"
                    onKeyPressUpdate={true}
                    waitAfterKeyPress={1000}
                    onChange={(jsObject) => handleChanged(jsObject)}
                />
            </DialogContent>
            <Divider/>
            <DialogActions>
                <Button id="saveButton" onClick={handleSave} disabled={!state.changed || !state.valid} color="primary">
                    Save
                </Button>
            </DialogActions>
        </Dialog>
    );
}

