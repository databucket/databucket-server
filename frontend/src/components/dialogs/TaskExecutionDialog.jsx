import React, {useCallback, useContext, useEffect, useState} from 'react';
import {styled} from '@mui/material/styles';
import Dialog from '@mui/material/Dialog';
import IconButton from '@mui/material/IconButton';
import MuiDialogTitle from '@mui/material/DialogTitle';
import MuiDialogContent from '@mui/material/DialogContent';
import MuiDialogActions from '@mui/material/DialogActions';
import CloseIcon from '@mui/icons-material/Close';
import Typography from '@mui/material/Typography';
import PropTypes from "prop-types";
import {CircularProgress, Tabs} from "@mui/material";
import {getDeleteOptions, getPostOptions, getPutOptions} from "../../utils/MaterialTableHelper";
import Tab from "@mui/material/Tab";
import {MessageBox} from "../utils/MessageBox";
import TaskActions from "../utils/TaskActions";
import PropertiesTable from "../utils/PropertiesTable";
import {getBucketTags, getBucketTasks} from "../data/BucketDataTableHelper";
import EnumsProvider from "../../context/enums/EnumsProvider";
import Button from "@mui/material/Button";
import TaskMenuSelector from "../data/TaskMenuSelector";
import {handleErrors} from "../../utils/FetchHelper";
import {getClearDataHistoryByRulesUrl, getDataUrl} from "../../utils/UrlBuilder";
import {getClassById, getPropertyByUuid} from "../../utils/JsonHelper";
import {Query, Utils as QbUtils} from "@react-awesome-query-builder/ui";
import {createConfig, getInitialTree, renderBuilder, renderResult} from "../utils/QueryBuilderHelper";
import AccessContext from "../../context/access/AccessContext";
import {getTaskExecutionDialogSize, setTaskExecutionDialogSize} from "../../utils/ConfigurationStorage";
import Grid from "@mui/material/Grid";
import {debounce2} from "../utils/UseWindowDimension";

const PREFIX = 'TaskExecutionDialog';

const classes = {
    root: `${PREFIX}-root`,
    root2: `${PREFIX}-root2`,
    root3: `${PREFIX}-root3`,
    selected: `${PREFIX}-selected`,
    dialogPaper: `${PREFIX}-dialogPaper`,
    oneLine: `${PREFIX}-oneLine`,
    tabs: `${PREFIX}-tabs`,
    devGrabSpace: `${PREFIX}-devGrabSpace`,
    divActionGrabSpace: `${PREFIX}-divActionGrabSpace`,
    container: `${PREFIX}-container`,
    closeButton: `${PREFIX}-closeButton`,
    smallerButton: `${PREFIX}-smallerButton`,
    largerButton: `${PREFIX}-largerButton`
};

const StyledDialog = styled(Dialog)(({theme}) => ({
    [`& .${classes.dialogPaper}`]: {
        minHeight: '80vh',
    },

    [`& .${classes.oneLine}`]: {
        display: 'flex',
        alignItems: 'center',
        flexWrap: 'wrap'
    },

    [`& .${classes.tabs}`]: {
        flexGrow: 1
    },

    [`& .${classes.devGrabSpace}`]: {
        width: '170px'
    },

    [`& .${classes.divActionGrabSpace}`]: {
        width: '30px'
    },

    [`& .${classes.root}`]: {
        margin: 0,
        padding: theme.spacing(2),
    },
    [`& .${classes.container}`]: {
        display: 'flex',
        flexWrap: 'wrap',
    },
    [`& .${classes.closeButton}`]: {
        position: 'absolute',
        right: theme.spacing(1),
        top: theme.spacing(1)
    },
    [`& .${classes.smallerButton}`]: {
        position: 'absolute',
        right: theme.spacing(15),
        top: theme.spacing(1)
    },
    [`& .${classes.largerButton}`]: {
        position: 'absolute',
        right: theme.spacing(10),
        top: theme.spacing(1)
    }
}));

const DialogTitle = (props => {
    const {children, onClose, onMakeDialogSmaller, onMakeDialogLarger} = props;
    return (
        <MuiDialogTitle disableTypography className={classes.root}>
            <Typography variant="h6">{children}</Typography>
            <IconButton
                aria-label="Smaller"
                className={classes.smallerButton}
                onClick={onMakeDialogSmaller}
                color={"inherit"}
                disabled={onMakeDialogSmaller == null}
                size="large">
                <span className="material-icons">fullscreen_exit</span>
            </IconButton>
            <IconButton
                aria-label="Larger"
                className={classes.largerButton}
                onClick={onMakeDialogLarger}
                color={"inherit"}
                disabled={onMakeDialogLarger == null}
                size="large">
                <span className="material-icons">fullscreen</span>
            </IconButton>
            {onClose ? (
                <IconButton
                    aria-label="Close"
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

const DialogActions = MuiDialogActions;

TaskExecutionDialog.propTypes = {
    open: PropTypes.bool.isRequired,
    bucket: PropTypes.object.isRequired,
    onClose: PropTypes.func.isRequired,
    reload: PropTypes.func.isRequired,
    activeLogic: PropTypes.object
};

const initialActions = {properties: []};

export default function TaskExecutionDialog(props) {


    const accessContext = useContext(AccessContext);
    const bucketTags = getBucketTags(props.bucket, accessContext.tags);
    const [activeTab, setActiveTab] = useState(0);
    const [messageBox, setMessageBox] = useState({open: false, severity: 'error', title: '', message: ''});
    const [appliesCount, setAppliesCount] = useState(0);
    const [state, setState] = useState({
        actions: initialActions,
        properties: [],
        logic: null,
        tree: null,
        config: null,
        processing: false
    });
    const [dialogSize, setDialogSize] = useState('md');
    const dialogContentRef = React.useRef(null);

    useEffect(() => {
        setDialogSize(getTaskExecutionDialogSize());
    }, []);

    useEffect(() => {
        if (props.open) {
            setActiveTab(0);
            const properties = getClassProperties();
            const config = createConfig(properties, bucketTags, accessContext.users, accessContext.enums);
            const tree = QbUtils.checkTree(getInitialTree(props.activeLogic, null, config), config);
            setState({
                ...state,
                properties: getClassProperties(),
                actions: initialActions,
                logic: props.activeLogic,
                tree: tree,
                config: config
            });
        }
    }, [props.open]);


    const onTaskSelected = (task) => {
        if (task.filterId != null) {
            const filter = accessContext.filters.filter(f => f.id === task.filterId)[0];
            const properties = getMergedProperties(getClassProperties(), task.configuration.properties, filter.configuration.properties);
            const config = createConfig(properties, bucketTags, accessContext.users, accessContext.enums);
            const tree = QbUtils.checkTree(getInitialTree(filter.configuration.logic, filter.configuration.tree, config), config);
            setState({
                ...state,
                actions: task.configuration.actions,
                logic: filter.configuration.logic,
                tree: tree,
                properties: properties,
                config: config
            });
        } else {
            const properties = getMergedProperties(getClassProperties(), task.configuration.properties, []);
            const config = createConfig(properties, bucketTags, accessContext.users, accessContext.enums);
            const tree = QbUtils.checkTree(getInitialTree(null, null, config), config);
            setState({
                ...state,
                actions: task.configuration.actions,
                logic: null,
                tree: tree,
                properties: properties,
                config: config
            });
        }
    }

    useEffect(() => {
        refreshAppliesCount({open: props.open, bucket: props.bucket, logic: state.logic});
    }, [props.open, state.logic]);

    const refreshAppliesCount = useCallback(
        debounce2(({open, bucket, logic}) => {
            if (open) {
                let resultOk = true;
                fetch(getDataUrl(bucket) + '/get?limit=0', getPostOptions({logic}))
                    .then(handleErrors)
                    .catch(error => {
                        setMessageBox({open: true, severity: 'error', title: 'Error', message: error});
                        resultOk = false;
                    })
                    .then(result => {
                        if (resultOk)
                            setAppliesCount(result.total);
                    });
            }
        }, 1000),
        []
    );

    const onTaskExecute = () => {
        let resultOk = true;
        if (state.actions.type === 'remove') {
            setState({...state, processing: true});
            fetch(getDataUrl(props.bucket), getDeleteOptions({logic: state.logic}))
                .then(handleErrors)
                .catch(error => {
                    setMessageBox({open: true, severity: 'error', title: 'Error', message: error});
                    resultOk = false;
                })
                .then(result => {
                    setState({...state, processing: false});
                    if (resultOk) {
                        setMessageBox({open: true, severity: 'success', title: result.message, message: null});
                        refreshAppliesCount({open: props.open, bucket: props.bucket, logic: state.logic});
                        props.reload();
                    }
                });
        } else if (state.actions.type === 'clear history') {
            setState({...state, processing: true});
            fetch(getClearDataHistoryByRulesUrl(props.bucket), getDeleteOptions({logic: state.logic}))
                .then(handleErrors)
                .catch(error => {
                    setMessageBox({open: true, severity: 'error', title: 'Error', message: error});
                    resultOk = false;
                })
                .then(result => {
                    setState({...state, processing: false});
                    if (resultOk) {
                        setMessageBox({open: true, severity: 'success', title: result.message, message: null});
                        props.reload();
                    }
                });
        } else {
            let change = false;
            let payload = {logic: state.logic};

            if (state.actions.setTag != null && state.actions.setTag === true) {
                payload.tagId = state.actions.tagId;
                change = true;
            }

            if (state.actions.setReserved != null && state.actions.setReserved === true) {
                payload.reserved = state.actions.reserved;
                change = true;
            }

            if (state.actions.properties != null && state.actions.properties.length > 0) {
                let propertiesToSet = {};
                let propertiesToRemove = [];
                state.actions.properties.forEach(property => {
                    const propertyDef = getPropertyByUuid(state.properties, property.uuid);
                    if (['setValue', 'setNull'].includes(property.action))
                        propertiesToSet[propertyDef.path] = property.value;
                    else
                        propertiesToRemove.push(propertyDef.path);
                });
                if (Object.keys(propertiesToSet).length) {
                    payload.propertiesToSet = propertiesToSet;
                    change = true;
                }

                if (propertiesToRemove.length > 0) {
                    payload.propertiesToRemove = propertiesToRemove;
                    change = true;
                }
            }

            if (change) {
                setState({...state, processing: true});
                fetch(getDataUrl(props.bucket), getPutOptions(payload))
                    .then(handleErrors)
                    .catch(error => {
                        setMessageBox({open: true, severity: 'error', title: 'Error', message: error});
                        resultOk = false;
                    })
                    .then(result => {
                        setState({...state, processing: false});
                        if (resultOk) {
                            setMessageBox({open: true, severity: 'success', title: result.message, message: null});
                            refreshAppliesCount({open: props.open, bucket: props.bucket, logic: state.logic});
                            props.reload();
                        }
                    });
            } else
                setMessageBox({
                    open: true,
                    severity: 'info',
                    title: "No modifications has been defined!",
                    message: null
                });
        }
    }

    const handleChangedTab = (event, newActiveTab) => {
        setActiveTab(newActiveTab);
    }

    const handleClose = () => {
        props.onClose();
    };

    const getClassProperties = () => {
        if (props.bucket.classId != null) {
            const dataClass = getClassById(accessContext.classes, props.bucket.classId);
            return dataClass.configuration;
        } else
            return [];
    }

    const getMergedProperties = (classProperties, taskProperties, filterProperties) => {
        let mergedProperties = [];

        classProperties.forEach(property => {
            if (!mergedProperties.find(f => f.path === property.path)) {
                mergedProperties = [...mergedProperties, property];
            }
        });

        taskProperties.forEach(property => {
            if (!mergedProperties.find(f => f.path === property.path)) {
                mergedProperties = [...mergedProperties, property];
            }
        });

        filterProperties.forEach(property => {
            if (!mergedProperties.find(f => f.path === property.path)) {
                mergedProperties = [...mergedProperties, property];
            }
        });

        return mergedProperties;
    }

    const getUsedUuids = () => {
        return [];
    }

    const setActions = (actions) => {
        setState({...state, actions: actions});
    }

    const setProperties = (properties) => {
        const config = createConfig(properties, bucketTags, accessContext.users, accessContext.enums);
        let tree = QbUtils.checkTree(getInitialTree(props.activeLogic, null, config), config);
        setState({...state, properties: properties, logic: props.activeLogic, tree: tree, config: config});
    }

    const onRulesChange = (tree, config) => {
        const logic = QbUtils.jsonLogicFormat(tree, config).logic;
        setState({...state, logic: logic, tree: tree, config: config});
    }

    const onMakeDialogSmaller = () => {
        if (dialogSize === 'lg') {
            setDialogSize('md');
            setTaskExecutionDialogSize('md');
        }
    }

    const onMakeDialogLarger = () => {
        if (dialogSize === 'md') {
            setDialogSize('lg');
            setTaskExecutionDialogSize('lg');
        }
    }

    return (
        <StyledDialog
            onClose={handleClose} // Enable this to close editor by clicking outside the dialog
            aria-labelledby="task-execution-dialog-title"
            open={props.open}
            fullWidth={true}
            maxWidth={dialogSize}  //'xs' | 'sm' | 'md' | 'lg' | 'xl' | false
        >
            <DialogTitle
                id="customized-dialog-title"
                onClose={handleClose}
                onMakeDialogSmaller={dialogSize === 'lg' ? onMakeDialogSmaller : null}
                onMakeDialogLarger={dialogSize === 'md' ? onMakeDialogLarger : null}
            >
                <div className={classes.oneLine}>
                    <Typography variant="h6">{'Task execution'}</Typography>
                    <TaskMenuSelector tasks={getBucketTasks(props.bucket, accessContext.tasks)}
                                      onTaskSelected={onTaskSelected}/>
                    <Tabs
                        className={classes.tabs}
                        value={activeTab}
                        onChange={handleChangedTab}
                        centered
                    >
                        <StyledTab label="Action"/>
                        <StyledTab label="Rules"/>
                        <StyledTab label="Properties"/>
                    </Tabs>
                    <div className={classes.devGrabSpace}/>
                </div>
            </DialogTitle>
            <EnumsProvider>
                <DialogContent
                    dividers
                    style={{height: '62vh'}}
                    ref={dialogContentRef}
                    classes={{
                        root: classes.root
                    }}>
                    {props.open && state.processing &&
                        <Grid
                            container
                            spacing={0}
                            direction="column"
                            alignItems="center"
                            justifyContent="center"
                            style={{minHeight: '50vh'}}
                        >
                            <Grid item xs={3}>
                                <CircularProgress disableShrink/>
                            </Grid>
                        </Grid>
                    }
                    {props.open && !state.processing && activeTab === 0 &&
                        <TaskActions
                            actions={state.actions}
                            properties={state.properties}
                            tags={getBucketTags(props.bucket, accessContext.tags)}
                            onChange={setActions}
                            pageSize={null}
                        />}

                    {props.open && !state.processing && activeTab === 1 &&
                        <div>
                            <Query
                                {...state.config}
                                value={state.tree}
                                onChange={onRulesChange}
                                renderBuilder={renderBuilder}
                            />
                            {renderResult({tree: state.tree, config: state.config})}
                        </div>
                    }

                    {props.open && !state.processing && activeTab === 2 &&
                        <PropertiesTable
                            used={getUsedUuids()}
                            data={state.properties}
                            enums={accessContext.enums}
                            onChange={setProperties}
                            title={'Class origin and defined properties:'}
                            pageSize={null}
                            parentContentRef={dialogContentRef}
                        />}

                </DialogContent>
            </EnumsProvider>
            <DialogActions
                classes={{
                    root: classes.root2
                }}>
                <Typography
                    color={'primary'}>{`${appliesCount} data ${appliesCount > 1 ? 'rows' : 'row'} ${appliesCount > 1 ? 'match' : 'matches'} the rules`}</Typography>
                <div className={classes.divActionGrabSpace}/>
                <Button
                    variant="contained"
                    color="primary"
                    onClick={onTaskExecute}
                    disabled={state.actions.type == null}
                >
                    Execute
                </Button>
            </DialogActions>
            <MessageBox
                config={messageBox}
                onClose={() => setMessageBox({...messageBox, open: false})}
            />
        </StyledDialog>
    );
}


const StyledTab = Tab
