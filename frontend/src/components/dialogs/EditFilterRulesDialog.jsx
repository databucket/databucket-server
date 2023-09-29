import React, {useState} from 'react';
import {
    Button,
    Dialog,
    DialogActions as MuiDialogActions,
    DialogContent as MuiDialogContent,
    DialogTitle as MuiDialogTitle,
    IconButton,
    styled,
    Tab,
    Tabs,
    Tooltip,
    Typography
} from '@mui/material';
import {Close as CloseIcon, MoreHoriz} from '@mui/icons-material';
import PropTypes from 'prop-types';
import {MessageBox} from "../utils/MessageBox";
import FilterRulesEditor from "../utils/FilterRulesEditor";
import {Utils as QbUtils} from "@react-awesome-query-builder/mui";
import {getObjectLengthStr} from "../../utils/JsonHelper";

const PREFIX = 'EditFilterRulesDialog';

const classes = {
    selected: `${PREFIX}-selected`,
    dialogPaper: `${PREFIX}-dialogPaper`,
    oneLine: `${PREFIX}-oneLine`,
    tabs: `${PREFIX}-tabs`,
    devGrabSpace: `${PREFIX}-devGrabSpace`,
    closeButton: `${PREFIX}-closeButton`,
};

const Root = styled('div')(({theme}) => ({
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
        width: '200px'
    }
}));

const StyledDialogTitle = styled(MuiDialogTitle)(({theme}) => ({
    margin: 0,
    padding: theme.spacing(1),

    [`& .${classes.closeButton}`]: {
        position: 'absolute',
        right: theme.spacing(1),
        top: theme.spacing(1),
        color: theme.palette.grey[500],
    }
}));


const DialogTitle = ((props) => {
    const {children, onClose, ...other} = props;
    return (
        <StyledDialogTitle {...other}>
            {children}
            {onClose ? (
                <IconButton
                    aria-label="close"
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

const DialogActions = MuiDialogActions;

EditFilterRulesDialog.propTypes = {
    configuration: PropTypes.object.isRequired,
    name: PropTypes.string.isRequired,
    dataClass: PropTypes.object,
    tags: PropTypes.array.isRequired,
    users: PropTypes.array.isRequired,
    onChange: PropTypes.func.isRequired
}

export default function EditFilterRulesDialog(props) {


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
        <Root>
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
                <DialogContent
                    dividers
                    style={{height: '75vh'}}
                    ref={dialogContentRef}>
                    {open &&
                        <FilterRulesEditor
                            activeTab={activeTab}
                            configuration={props.configuration}
                            dataClass={props.dataClass}
                            tags={props.tags}
                            users={props.users}
                            onChange={onFilterChanged}
                            parentContentRef={dialogContentRef}
                        />}
                </DialogContent>
                <DialogActions/>
            </Dialog>
            <MessageBox
                config={messageBox}
                onClose={() => setMessageBox({...messageBox, open: false})}
            />
        </Root>
    );
}

const StyledTab = Tab
