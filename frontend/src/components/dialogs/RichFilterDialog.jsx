import React, {useCallback, useContext, useEffect, useState} from 'react';
import {makeStyles, withStyles} from '@material-ui/core/styles';
import Dialog from '@material-ui/core/Dialog';
import IconButton from '@material-ui/core/IconButton';
import MuiDialogTitle from '@material-ui/core/DialogTitle';
import MuiDialogContent from '@material-ui/core/DialogContent';
import MuiDialogActions from '@material-ui/core/DialogActions';
import CloseIcon from '@material-ui/icons/Close';
import Typography from '@material-ui/core/Typography';
import PropTypes from "prop-types";
import {Tabs} from "@material-ui/core";
import {getPostOptions, getSettingsTabHooverBackgroundColor, getSettingsTabSelectedColor} from "../../utils/MaterialTableHelper";
import Tab from "@material-ui/core/Tab";
import {MessageBox} from "../utils/MessageBox";
import PropertiesTable from "../utils/PropertiesTable";
import {getBucketFilters, getBucketTags} from "../data/BucketDataTableHelper";
import EnumsProvider from "../../context/enums/EnumsProvider";
import Button from "@material-ui/core/Button";
import {handleErrors} from "../../utils/FetchHelper";
import {getDataUrl} from "../../utils/UrlBuilder";
import {getClassById} from "../../utils/JsonHelper";
import {Query, Utils as QbUtils} from "react-awesome-query-builder";
import {createConfig, getInitialTree, renderBuilder, renderResult} from "../utils/QueryBuilderHelper";
import AccessContext from "../../context/access/AccessContext";
import FilterMenuSelector from "../data/FilterMenuSelector";
import {getDataFilterDialogSize, setDataFilterDialogSize} from "../../utils/ConfigurationStorage";

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

const DialogTitle = withStyles(styles)(props => {
    const {children, classes, onClose, onMakeDialogSmaller, onMakeDialogLarger} = props;
    return (
        <MuiDialogTitle disableTypography className={classes.root}>
            <Typography variant="h6">{children}</Typography>
            <IconButton aria-label="Smaller" className={classes.smallerButton} onClick={onMakeDialogSmaller} color={"inherit"} disabled={onMakeDialogSmaller == null}>
                <span className="material-icons">fullscreen_exit</span>
            </IconButton>
            <IconButton aria-label="Larger" className={classes.largerButton} onClick={onMakeDialogLarger} color={"inherit"} disabled={onMakeDialogLarger == null}>
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

const debounce = (func, wait, immediate) => {
    let timeout;

    return (...args) => {
        let context = this;
        let later = () => {
            timeout = null;
            if (!immediate) func.apply(context, args);
        };

        let callNow = immediate && !timeout;
        clearTimeout(timeout);
        timeout = setTimeout(later, wait);
        if (callNow) func.apply(context, args);
    };
}

RichFilterDialog.propTypes = {
    open: PropTypes.bool.isRequired,
    bucket: PropTypes.object.isRequired,
    onClose: PropTypes.func.isRequired,
    reload: PropTypes.func.isRequired,
    activeLogic: PropTypes.object,
    setActiveLogic: PropTypes.func.isRequired
};

export default function RichFilterDialog(props) {

    const classes = useStyles();
    const accessContext = useContext(AccessContext);
    const bucketTags = getBucketTags(props.bucket, accessContext.tags);
    const [activeTab, setActiveTab] = useState(0);
    const [messageBox, setMessageBox] = useState({open: false, severity: 'error', title: '', message: ''});
    const [appliesCount, setAppliesCount] = useState(0);
    const [state, setState] = useState({properties: [], logic: null, tree: null, config: null});
    const [dialogSize, setDialogSize] = useState('md');
    const dialogContentRef = React.useRef(null);

    useEffect(() => {
        setDialogSize(getDataFilterDialogSize());
    }, []);

    useEffect(() => {
        if (props.open) {
            setActiveTab(0);
            const properties = getClassProperties();
            const config = createConfig(properties, bucketTags, accessContext.users, accessContext.enums);
            const disabledRulesLogic = makeRulesDisabled(props.activeLogic);
            let tree = QbUtils.checkTree(getInitialTree(disabledRulesLogic, null, config), config);
            setState({...state, properties: getClassProperties(), logic: disabledRulesLogic, tree: tree, config: config});
        }
    }, [props.open]);

    const makeRulesDisabled = (inputLogic) => {
        // _meta << not implemented by component founder yet!!!
        // const inputLogicStr = JSON.stringify(inputLogic);
        // const disabledILogicStr = inputLogicStr.replace("\"var\":", "\"_meta\": {\"readonly\": true}, \"var\":");
        // return JSON.parsnpute(disabledInputLogicStr);
        return inputLogic;
    }

    const filterApply = () => {
        props.setActiveLogic(state.logic);
    }

    const getMergedProperties = (classProperties, filterProperties) => {
        let mergedProperties = [];

        classProperties.forEach(property => {
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

    const onFilterSelected = (filter) => {
        const properties = getMergedProperties(getClassProperties(), filter.configuration.properties);
        const config = createConfig(properties, bucketTags, accessContext.users, accessContext.enums);
        const tree = QbUtils.checkTree(getInitialTree(filter.configuration.logic, filter.configuration.tree, config), config);
        setState({
            ...state,
            logic: filter.configuration.logic,
            tree: tree,
            properties: properties,
            config: config
        });
    }

    useEffect(() => {
        refreshAppliesCount({open: props.open, bucket: props.bucket, logic: state.logic});
    }, [props.open, state.logic]);

    const refreshAppliesCount = useCallback(
        debounce(({open, bucket, logic}) => {
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

    const setProperties = (properties) => {
        const config = createConfig(properties, bucketTags, accessContext.users, accessContext.enums);
        const disabledRulesLogic = makeRulesDisabled(props.activeLogic);
        let tree = QbUtils.checkTree(getInitialTree(disabledRulesLogic, null, config), config);
        setState({...state, properties: properties, logic: disabledRulesLogic, tree: tree, config: config});
    }

    const onRulesChange = (tree, config) => {
        const logic = QbUtils.jsonLogicFormat(tree, config).logic;
        setState({...state, logic: logic, tree: tree, config: config});
    }

    const onMakeDialogSmaller = () => {
        if (dialogSize === 'lg') {
            setDialogSize('md');
            setDataFilterDialogSize('md');
        }
    }

    const onMakeDialogLarger = () => {
        if (dialogSize === 'md') {
            setDialogSize('lg');
            setDataFilterDialogSize('lg');
        }
    }

    return (
        <Dialog
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
                onMakeDialogLarger={dialogSize === 'md' ? onMakeDialogLarger: null}
            >
                <div className={classes.oneLine}>
                    <Typography variant="h6">{'Data filter'}</Typography>
                    <FilterMenuSelector filters={getBucketFilters(props.bucket, accessContext.filters)} onFilterSelected={onFilterSelected}/>
                    <Tabs
                        className={classes.tabs}
                        value={activeTab}
                        onChange={handleChangedTab}
                        centered
                    >
                        <StyledTab label="Rules"/>
                        <StyledTab label="Properties"/>
                    </Tabs>
                    <div className={classes.devGrabSpace}/>
                </div>
            </DialogTitle>
            <EnumsProvider>
                <DialogContent dividers style={{height: '62vh'}} ref = {dialogContentRef}>
                    {props.open && activeTab === 0 &&
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

                    {props.open && activeTab === 1 &&
                    <PropertiesTable
                        used={[]}
                        data={state.properties}
                        enums={accessContext.enums}
                        onChange={setProperties}
                        title={'Class origin and defined properties:'}
                        pageSize={null}
                        parentContentRef={dialogContentRef}
                    />}

                </DialogContent>
            </EnumsProvider>
            <DialogActions>
                <Typography color={'primary'}>{`${appliesCount} data ${appliesCount > 1 ? 'rows' : 'row'} ${appliesCount > 1 ? 'match' : 'matches'} the rules`}</Typography>
                <div className={classes.divActionGrabSpace}/>
                <Button
                    variant="contained"
                    color="primary"
                    onClick={filterApply}
                >
                    Apply
                </Button>
            </DialogActions>
            <MessageBox
                config={messageBox}
                onClose={() => setMessageBox({...messageBox, open: false})}
            />
        </Dialog>
    );
}


const useStyles = makeStyles(() => ({
    dialogPaper: {
        minHeight: '80vh',
    },
    oneLine: {
        display: 'flex',
        alignItems: 'center',
        flexWrap: 'wrap'
    },
    tabs: {
        flexGrow: 1
    },
    devGrabSpace: {
        width: '250px'
    },
    divActionGrabSpace: {
        width: '30px'
    }
}));

const tabStyles = theme => ({
    root: {
        "&:hover": {
            backgroundColor: getSettingsTabHooverBackgroundColor(theme),
            opacity: 1
        },
        "&$selected": {
            // backgroundColor: getSettingsTabSelectedBackgroundColor(theme),
            color: getSettingsTabSelectedColor(theme),
        },
        textTransform: "initial"
    },
    selected: {}
});

const StyledTab = withStyles(tabStyles)(Tab)
