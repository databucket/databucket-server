import React, {useState} from 'react';
import {makeStyles, withStyles} from '@material-ui/core/styles';
import Dialog from '@material-ui/core/Dialog';
import MuiDialogTitle from '@material-ui/core/DialogTitle';
import MuiDialogContent from '@material-ui/core/DialogContent';
import IconButton from '@material-ui/core/IconButton';
import CloseIcon from '@material-ui/icons/Done';
import Typography from '@material-ui/core/Typography';
import MoreHoriz from "@material-ui/icons/MoreHoriz";
import Tooltip from "@material-ui/core/Tooltip";
import PropTypes from 'prop-types';
import Button from "@material-ui/core/Button";
import {MessageBox} from "../utils/MessageBox";
import {Utils as QbUtils} from "react-awesome-query-builder";
import {getObjectLengthStr} from "../../utils/JsonHelper";
import {Tabs} from "@material-ui/core";
import {getSettingsTabHooverBackgroundColor, getSettingsTabSelectedColor} from "../../utils/MaterialTableHelper";
import Tab from "@material-ui/core/Tab";
import MuiDialogActions from "@material-ui/core/DialogActions";
import FilterRulesEditorTemplate from "../utils/FilterRulesEditorTemplate";


const styles = (theme) => ({
    root: {
        margin: 0,
        padding: theme.spacing(1),
    },
    closeButton: {
        position: 'absolute',
        right: theme.spacing(1),
        top: theme.spacing(1),
        color: theme.palette.grey[500],
    }
});

const DialogTitle = withStyles(styles)((props) => {
    const {children, classes, onClose, ...other} = props;
    return (
        <MuiDialogTitle disableTypography className={classes.root} {...other}>
            {children}
            {onClose ? (
                <IconButton aria-label="close" className={classes.closeButton} onClick={onClose}>
                    <CloseIcon/>
                </IconButton>
            ) : null}
        </MuiDialogTitle>
    );
});

const DialogContent = withStyles((theme) => ({
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

EditTemplateFilterRulesDialog.propTypes = {
    configuration: PropTypes.object.isRequired,
    name: PropTypes.string.isRequired,
    dataClass: PropTypes.object,
    tags: PropTypes.array.isRequired,
    users: PropTypes.array.isRequired,
    onChange: PropTypes.func.isRequired,
    enums: PropTypes.array.isRequired
}

export default function EditTemplateFilterRulesDialog(props) {

    const classes = useStyles();
    const [activeTab, setActiveTab] = useState(0);
    const [messageBox, setMessageBox] = useState({open: false, severity: 'error', title: '', message: ''})
    const [open, setOpen] = useState(false);
    const [configuration, setConfiguration] = useState(null);
    const dialogContentRef = React.useRef(null);

    const handleClickOpen = () => {
        setOpen(true);
    };

    const handleSave = () => {
        if (configuration != null) {
            const logic = QbUtils.jsonLogicFormat(configuration.tree, configuration.config).logic;
            const properties = configuration.properties;
            const tree = configuration.tree;
            props.onChange({properties, logic, tree});
        }
        setOpen(false);
    }

    const onFilterChanged = ({properties, config, tree}) => {
        setConfiguration({properties, config, tree});
    }

    const handleChangedTab = (event, newActiveTab) => {
        setActiveTab(newActiveTab);
    }

    return (
        <div>
            <Tooltip title={'Define rules'}>
                <Button
                    endIcon={<MoreHoriz/>}
                    onClick={handleClickOpen}
                    style={{textTransform: 'none'}}
                >
                    {getObjectLengthStr(props.configuration)}
                </Button>
            </Tooltip>
            <Dialog
                onClose={handleSave}
                aria-labelledby="customized-dialog-title"
                classes={{paper: classes.dialogPaper}}
                open={open}
                fullWidth={true}
                maxWidth='lg' //'xs' | 'sm' | 'md' | 'lg' | 'xl' | false
            >
                <DialogTitle id="customized-dialog-title" onClose={handleSave}>
                    <div className={classes.oneLine}>
                        <Typography variant="h6">{'Filter configuration'}</Typography>
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
                <DialogContent dividers style={{height: '75vh'}} ref = {dialogContentRef}>
                    {open &&
                    <FilterRulesEditorTemplate
                        activeTab={activeTab}
                        configuration={props.configuration}
                        dataClass={props.dataClass}
                        tags={props.tags}
                        users={props.users}
                        onChange={onFilterChanged}
                        parentContentRef={dialogContentRef}
                        enums={props.enums}
                    />}
                </DialogContent>
                <DialogActions />
            </Dialog>
            <MessageBox
                config={messageBox}
                onClose={() => setMessageBox({...messageBox, open: false})}
            />
        </div>
    );
};

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
        width: '200px'
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