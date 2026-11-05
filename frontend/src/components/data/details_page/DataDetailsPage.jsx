import React, {createRef, useContext, useEffect, useRef, useState} from 'react';
import {createTagLookup} from "../../../utils/JsonHelper";
import {
    getBodyHeight,
    getGetOptions,
    getPutOptions,
    getTableHeaderBackgroundColor,
    getTableRowBackgroundColor
} from "../../../utils/MaterialTableHelper";
import MaterialTable from '@material-table/core';
import TagsContext from "../../../context/tags/TagsContext";
import {
    Button,
    Divider,
    Grid,
    IconButton,
    InputAdornment,
    TextField,
    Tooltip,
    useTheme
} from "@mui/material";
import {debounce} from "../../utils/Debouncer";
import {Link, useParams} from "react-router-dom";
import {getDataByIdUrl2} from "../../../utils/UrlBuilder";
import {handleErrors} from "../../../utils/FetchHelper";
import { JSONPath } from "jsonpath-plus";
import {MessageBox} from "../../utils/MessageBox";
import JsonColorsButton from "../../utils/JsonColorsButton";
import {getDirectDataPath, getProjectDataPath} from "../../../route/AppRouter";
import { JsonEditor as Editor } from 'jsoneditor-react18';
import Ajv from 'ajv';
import ace from 'brace';
import 'brace/mode/json';
import "brace/theme/monokai";
import {Close as CloseIcon} from "@mui/icons-material";
import {setPathname} from "../../../utils/ConfigurationStorage";
// import "brace/theme/eclipse";

const ajv = new Ajv({allErrors: true, verbose: true});
const jsonThemeLight = null; //"ace/theme/eclipse";
const jsonThemeDark = "ace/theme/monokai";

export default function DataDetailsPage() {

    const inputParams = useParams();
    const theme = useTheme();
    const tableRef = createRef();
    const jsonEditorRef = useRef(null);
    const jsonPathRef = useRef(null);
    const [state, setState] = useState({changed: false, changedProperties: null});
    const [messageBox, setMessageBox] = useState({open: false, severity: 'error', title: '', message: ''});
    const tagsContext = useContext(TagsContext);
    const {tags, fetchTags} = tagsContext;
    const tagsLookup = createTagLookup(tags);
    const [jsonPath, setJsonPath] = useState(null);
    const [jsonPathFilled, setJsonPathFilled] = useState(false);
    const [data, setData] = useState(null);

    const setNativeValue = (el, value) => {
        const previousValue = el.value;

        if (el.type === 'checkbox' || el.type === 'radio') {
            if ((!!value && !el.checked) || (!!!value && el.checked)) {
                el.click();
            }
        } else el.value = value;

        const tracker = el._valueTracker;
        if (tracker) {
            tracker.setValue(previousValue);
        }

        el.dispatchEvent(new Event('change', {bubbles: true}));
    }

    useEffect(() => {
        let resultOk = true;
        fetch(getDataByIdUrl2(inputParams.bucketName, inputParams.dataId), getGetOptions())
            .then(handleErrors)
            .catch(error => {
                setMessageBox({open: true, severity: 'error', title: 'Error', message: error});
                resultOk = false;
            })
            .then(dataRow => {
                if (resultOk) {
                    setData(dataRow);
                    if (jsonEditorRef.current !== null && dataRow != null && dataRow.properties != null) {
                        const jsonEditor = jsonEditorRef.current.jsonEditor;
                        jsonEditor.set(dataRow.properties);
                    }
                    if (inputParams.jsonPath != null && jsonPathRef.current != null) {
                        setJsonPath(inputParams.jsonPath);
                        setNativeValue(document.getElementById('jsonPathId'), inputParams.jsonPath);
                        setJsonPathFilled(true);
                    }
                }
            });
    }, [inputParams]);

    useEffect(() => {
        if (tags == null)
            fetchTags();
    }, [tags, fetchTags]);

    const handleChange = json => {
        const initial = JSON.stringify(data.properties);
        const current = JSON.stringify(json);
        setState({...state, changed: initial.localeCompare(current) !== 0, changedProperties: json});
    };

    const handleSave = () => {
        let resultOk = true;
        fetch(getDataByIdUrl2(inputParams.bucketName, inputParams.dataId), getPutOptions({properties: state.changedProperties}))
            .then(handleErrors)
            .catch(error => {
                setMessageBox({open: true, severity: 'error', title: 'Error', message: error});
                resultOk = false;
            })
            .then(() => {
                if (resultOk) {
                    setMessageBox({open: true, severity: 'success', title: 'Saved', message: ""});
                }
            });
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

    const debouncedChangedJsonPath = useRef(debounce(newJsonPath => setJsonPath(newJsonPath), 1000)).current;

    const handleChangedJsonPath = (event) => {
        setJsonPathFilled(event.target.value !== '');
        debouncedChangedJsonPath(event.target.value);
    };

    // the debounced change is called too, so that a change still pending does not bring the old path back
    const clearJsonPath = () => {
        const input = document.getElementById('jsonPathId');
        if (input != null)
            input.value = '';
        setJsonPathFilled(false);
        debouncedChangedJsonPath('');
        setJsonPath('');
    };

    useEffect(() => {
        if (data != null && jsonEditorRef.current !== null) {
            const url = getDirectDataPath(inputParams.bucketName, inputParams.dataId);
            const jsonEditor = jsonEditorRef.current.jsonEditor;
            if (jsonPath != null && jsonPath.length > 0) {
                const fullJson = state.changedProperties != null ? state.changedProperties : data.properties;
                let filtered = [];
                try {
                    filtered = JSONPath({path: jsonPath, json: fullJson});
                } catch (err) {
                }
                jsonEditor.aceEditor.setReadOnly(true);
                jsonEditor.set(filtered);
                window.history.replaceState(null, null, url + "/" + jsonPath);
            } else {
                jsonEditor.aceEditor.setReadOnly(false);
                jsonEditor.set(state.changed ? state.changedProperties : data.properties);
                window.history.replaceState(null, null, url);
            }
        }
    }, [jsonPath]);

    document.title = `Databucket [${inputParams.bucketName}: ${inputParams.dataId}]`;
    setPathname(null); // clear path
    return (
        <div style={{height: getBodyHeight()}}>
            <IconButton
                component={Link}
                to={getProjectDataPath()}
                aria-label="Close"
                size="large">
                <CloseIcon/>
            </IconButton>
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
                data={data != null ? [data] : []}
                options={{
                    paging: false,
                    toolbar: false,
                    actionsColumnIndex: -1,
                    maxColumnSort: 0,
                    search: false,
                    filtering: false,
                    padding: 'dense',
                    headerStyle: {position: 'sticky', top: 0, backgroundColor: getTableHeaderBackgroundColor(theme), zIndex: 2},
                    rowStyle: rowData => ({backgroundColor: getTableRowBackgroundColor(rowData, theme)})
                }}
                components={{
                    Container: props => <div {...props} />
                }}
            />
            <Editor
                ref={jsonEditorRef}
                value={{}}
                ajv={ajv}
                mode="code"
                ace={ace}
                onChange={handleChange}
                theme={theme.palette.mode === 'light' ? jsonThemeLight : jsonThemeDark}
                statusBar={false}
                htmlElementProps={{style: {height: "100%"}}}
            />

            <Divider/>
            <Grid container spacing={0} alignItems="center">
                <Grid item xs>
                    <JsonColorsButton style={{marginLeft: "30px"}}/>
                    <Tooltip id="copy-content-tooltip" title="Copy content">
                        <IconButton
                            color={"inherit"}
                            onClick={copyContent}
                            size="large">
                            <span className="material-icons">content_copy</span>
                        </IconButton>
                    </Tooltip>
                </Grid>
                <Grid item xs={10}>
                    <TextField
                        ref={jsonPathRef}
                        hiddenLabel
                        id="jsonPathId"
                        size="small"
                        fullWidth
                        placeholder={"$.store.books[*].title"}
                        InputProps={{
                            disableUnderline: true,
                            startAdornment:
                                <InputAdornment position="start">
                                    <Tooltip title="Clear JSON path">
                                        <span>
                                            <IconButton size="small" onClick={clearJsonPath} disabled={!jsonPathFilled}><CloseIcon fontSize="small"/></IconButton>
                                        </span>
                                    </Tooltip>
                                </InputAdornment>
                        }}
                        onChange={handleChangedJsonPath}
                    />
                </Grid>
                <Grid item xs>
                    <Button id="saveButton" onClick={handleSave} disabled={!state.changed} color="primary">
                        Save
                    </Button>
                </Grid>
            </Grid>
            <MessageBox
                config={messageBox}
                onClose={() => setMessageBox({...messageBox, open: false})}
            />
        </div>
    );
}
