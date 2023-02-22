import React, {createRef, useEffect, useRef, useState} from 'react';
import { styled } from '@mui/material/styles';
import { useTheme } from '@mui/material/styles';
import PropTypes from 'prop-types';
import MaterialTable from 'material-table';
import Button from '@mui/material/Button';
import Dialog from '@mui/material/Dialog';
import IconButton from '@mui/material/IconButton';
import MuiDialogTitle from '@mui/material/DialogTitle';
import MuiDialogContent from '@mui/material/DialogContent';
import MuiDialogActions from '@mui/material/DialogActions';
import CloseIcon from '@mui/icons-material/Close';
import Typography from '@mui/material/Typography';
import {Divider, TextField, Tooltip} from '@mui/material';
import {createTagLookup} from "../../utils/JsonHelper";
import {getTableHeaderBackgroundColor, getTableRowBackgroundColor} from "../../utils/MaterialTableHelper";
import {getDataDetailsDialogSize, setDataDetailsDialogSize} from "../../utils/ConfigurationStorage";
import {MessageBox} from "../utils/MessageBox";
import jp from "jsonpath";
import {getDirectDataPath} from "../../route/AppRouter";
import {debounce2} from "../utils/UseWindowDimension";
import {JsonEditor as Editor} from 'jsoneditor-react';
import Ajv from 'ajv';
import ace from 'brace';
import 'brace/mode/json';
import "brace/theme/monokai";
const PREFIX = 'DataDetailsDialog';

const classes = {
    root: `${PREFIX}-root`,
    root2: `${PREFIX}-root2`,
    root3: `${PREFIX}-root3`,
    container: `${PREFIX}-container`,
    closeButton: `${PREFIX}-closeButton`,
    linkButton: `${PREFIX}-linkButton`,
    openButton: `${PREFIX}-openButton`,
    smallerButton: `${PREFIX}-smallerButton`,
    largerButton: `${PREFIX}-largerButton`,
    dialogPaper: `${PREFIX}-dialogPaper`
};

const StyledDialog = styled(Dialog)(() => ({
    [`& .${classes.dialogPaper}`]: {
        minHeight: '98vh',
    }
}));

// import "brace/theme/eclipse";

const ajv = new Ajv({allErrors: true, verbose: true});
const jsonThemeLight = null; //"ace/theme/eclipse";
const jsonThemeDark = "ace/theme/monokai";

const DialogTitle = (props => {
    const {children,  onClose, onMakeDialogSmaller, onMakeDialogLarger, onCopyDataLink, onOpenDataLink} = props;
    return (
        <MuiDialogTitle disableTypography className={classes.root}>
            <Typography variant="h6">{children}</Typography>
            <Tooltip id="link-tooltip" title="Copy direct link to data">
                <IconButton
                    className={classes.linkButton}
                    onClick={onCopyDataLink}
                    color={"inherit"}
                    size="large">
                    <span className="material-icons">link</span>
                </IconButton>
            </Tooltip>
            <Tooltip id="open-in-new-tooltip" title="Open details in new tab">
                <IconButton
                    className={classes.openButton}
                    onClick={onOpenDataLink}
                    color={"inherit"}
                    size="large">
                    <span className="material-icons">open_in_new</span>
                </IconButton>
            </Tooltip>
            <Tooltip id="smaller-window-tooltip" title="Smaller">
                <IconButton
                    className={classes.smallerButton}
                    onClick={onMakeDialogSmaller}
                    color={"inherit"}
                    disabled={onMakeDialogSmaller == null}
                    size="large">
                    <span className="material-icons">fullscreen_exit</span>
                </IconButton>
            </Tooltip>
            <Tooltip id="larger-window-tooltip" title="Larger">
                <IconButton
                    aria-label="Larger"
                    className={classes.largerButton}
                    onClick={onMakeDialogLarger}
                    color={"inherit"}
                    disabled={onMakeDialogLarger == null}
                    size="large">
                    <span className="material-icons">fullscreen</span>
                </IconButton>
            </Tooltip>
            {onClose ? (
                <Tooltip id="close-window-tooltip" title="Close">
                    <IconButton
                        aria-label="Close"
                        className={classes.closeButton}
                        onClick={onClose}
                        size="large">
                        <CloseIcon/>
                    </IconButton>
                </Tooltip>
            ) : null}
        </MuiDialogTitle>
    );
});

const DialogContent = MuiDialogContent;

const DialogActions = MuiDialogActions;

DataDetailsDialog.propTypes = {
    open: PropTypes.bool.isRequired,
    bucket: PropTypes.object.isRequired,
    dataRow: PropTypes.object,
    tags: PropTypes.array,
    onChange: PropTypes.func.isRequired
};

export default function DataDetailsDialog(props) {

    const theme = useTheme();

    const tableRef = createRef();
    const jsonEditorRef = useRef(null);
    const [messageBox, setMessageBox] = useState({open: false, severity: 'error', title: '', message: ''});
    const [state, setState] = useState({open: false, changed: false, changedProperties: null});
    const tagsLookup = createTagLookup(props.tags);
    const [dialogSize, setDialogSize] = useState('lg');
    const [jsonPath, setJsonPath] = useState(null);

    useEffect(() => {
        setDialogSize(getDataDetailsDialogSize());
    }, []);

    useEffect(() => {
        setState({...state, open: props.open});
    }, [props.dataRow, props.open]);

    const handleChange = json => {
        const initial = JSON.stringify(props.dataRow.properties);
        const current = JSON.stringify(json);
        setState({...state, changed: initial.localeCompare(current) !== 0, changedProperties: json});
    };

    const handleSave = () => {
        setState({...state, open: false, changed: false});
        props.onChange({...props.dataRow, properties: state.changedProperties}, true);
    };

    const handleClose = () => {
        props.onChange(null, false);
        setState({...state, open: false, changed: false});
    };

    const copyContent = async () => {
        try {
            if (jsonEditorRef.current !== null) {
                const jsonEditor = jsonEditorRef.current.jsonEditor;
                await navigator.clipboard.writeText(jsonEditor.getText());
            }
        } catch (err) {
            setMessageBox({open: true, severity: 'error', title: 'Error', message: 'Copying has failed!'});
        }
    }

    const copyDataLink = async () => {
        try {
            let dataLink = getDirectDataPath(props.bucket.name, props.dataRow.id);
            if (jsonPath != null)
                dataLink += "/" + jsonPath;
            await navigator.clipboard.writeText(dataLink);
        } catch (err) {
            setMessageBox({open: true, severity: 'error', title: 'Error', message: 'Copying has failed!'});
        }
    }

    const openDataLink = async () => {
        let dataLink = getDirectDataPath(props.bucket.name, props.dataRow.id);
        if (jsonPath != null)
            dataLink += "/" + jsonPath;
        window.open(dataLink, "_blank");
    }

    const onMakeDialogSmaller = () => {
        if (dialogSize === 'lg') {
            setDialogSize('md');
            setDataDetailsDialogSize('md');
        } else if (dialogSize === 'xl') {
            setDialogSize('lg');
            setDataDetailsDialogSize('lg');
        } else if (dialogSize === 'true') {
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
            setDialogSize('true');
            setDataDetailsDialogSize('true');
        }
    }

    const debouncedSave = useRef(debounce2(newJsonPath => setJsonPath(newJsonPath), 1000)).current;

    const handleChangedJsonPath = (event) => {
        debouncedSave(event.target.value);
    };

    useEffect(() => {
        if (props.dataRow != null && jsonEditorRef.current !== null) {
            const jsonEditor = jsonEditorRef.current.jsonEditor;
            if (jsonPath != null && jsonPath.length > 0) {
                const fullJson = state.changedProperties != null ? state.changedProperties : props.dataRow.properties;
                let filtered = [];
                try {
                    filtered = jp.query(fullJson, jsonPath);
                } catch (err) {}
                jsonEditor.aceEditor.setReadOnly(true);
                jsonEditor.set(filtered);
            } else {
                jsonEditor.aceEditor.setReadOnly(false);
                jsonEditor.set(state.changed ? state.changedProperties : props.dataRow.properties);
            }
        }
    }, [jsonPath]);

    return (
        <StyledDialog
            onClose={handleClose} // Enable this to close editor by clicking outside the dialog
            aria-labelledby="customized-dialog-title"
            classes={{paper: classes.dialogPaper}}
            open={state.open}
            fullWidth={true}
            maxWidth={dialogSize === 'true' ? true : dialogSize}  //'xs' | 'sm' | 'md' | 'lg' | 'xl' | false
        >
            <DialogTitle
                id="customized-dialog-title"
                onClose={handleClose}
                onMakeDialogSmaller={dialogSize !== 'md' ? onMakeDialogSmaller : null}
                onMakeDialogLarger={dialogSize !== 'true' ? onMakeDialogLarger : null}
                onCopyDataLink={copyDataLink}
                onOpenDataLink={openDataLink}
            >
                Data details
            </DialogTitle>
            <Divider/>
            <MaterialTable
                tableRef={tableRef}
                columns={[
                    {title: 'Id', field: 'id', type: 'numeric'},
                    {title: 'Tag name', field: 'tagId', type: 'numeric', lookup: tagsLookup},
                    {title: 'Tag id', field: 'tagId', type: 'numeric'},
                    {title: 'Reserved', field: 'reserved', type: 'boolean'},
                    {title: 'Owner', field: 'owner', type: 'string'},
                    {title: 'Created at', field: 'createdAt', type: 'datetime'},
                    {title: 'Created by', field: 'createdBy', type: 'string'},
                    {title: 'Modified at', field: 'modifiedAt', type: 'datetime'},
                    {title: 'Modified by', field: 'modifiedBy', type: 'string'}
                ]}
                data={[props.dataRow]}
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
            <DialogContent
                style={{height: '75vh'}}
                classes={{
                    root: classes.root
                }}>
                <Editor
                    ref={jsonEditorRef}
                    value={props.dataRow != null ? props.dataRow.properties : {}}
                    ajv={ajv}
                    mode="code"
                    ace={ace}
                    onChange={handleChange}
                    theme={theme.palette.mode === 'light' ? jsonThemeLight : jsonThemeDark}
                    statusBar={false}
                    htmlElementProps={{style: {height: "100%"}}}
                />
            </DialogContent>
            <Divider/>
            <DialogActions
                classes={{
                    root: classes.root2
                }}>
                <Tooltip id="copy-content-tooltip" title="Copy content">
                    <IconButton color={"inherit"} onClick={copyContent} size="large">
                        <span className="material-icons">content_copy</span>
                    </IconButton>
                </Tooltip>
                <TextField
                    hiddenLabel
                    id="jsonPathId"
                    size="small"
                    fullWidth
                    placeholder={"$.store.books[*].title"}
                    InputProps={{disableUnderline: true}}
                    onChange={handleChangedJsonPath}
                />
                <div style={{width: '100px'}}/>
                <Button id="saveButton" onClick={handleSave} disabled={!state.changed} color="primary">
                    Save
                </Button>
            </DialogActions>
            <MessageBox
                config={messageBox}
                onClose={() => setMessageBox({...messageBox, open: false})}
            />
        </StyledDialog>
    );
}

