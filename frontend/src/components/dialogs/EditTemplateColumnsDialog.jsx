import React, {useContext, useEffect, useState} from 'react';
import {styled} from '@mui/material/styles';
import Dialog from '@mui/material/Dialog';
import MuiDialogTitle from '@mui/material/DialogTitle';
import MuiDialogContent from '@mui/material/DialogContent';
import IconButton from '@mui/material/IconButton';
import CloseIcon from '@mui/icons-material/Done';
import Typography from '@mui/material/Typography';
import MoreHoriz from "@mui/icons-material/MoreHoriz";
import Tooltip from "@mui/material/Tooltip";
import PropTypes from 'prop-types';
import Button from "@mui/material/Button";
import {convertNullValuesInCollection} from "../../utils/JsonHelper";
import {getColumnMapper} from "../../utils/NullValueMappers";
import {Tabs} from "@mui/material";
import Tab from "@mui/material/Tab";
import PropertiesTable, {mergeProperties} from "../utils/PropertiesTable";
import ColumnsTable from "../utils/ColumnsTable";
import MuiDialogActions from "@mui/material/DialogActions";
import TemplatesContext from "../../context/templates/TemplatesContext";
import {getTemplatesArtefacts} from "../management/templatesConfig/_TemplUtils";

const PREFIX = 'EditTemplateColumnsDialog';

const classes = {
    root: `${PREFIX}-root`,
    root2: `${PREFIX}-root2`,
    root3: `${PREFIX}-root3`,
    selected: `${PREFIX}-selected`,
    oneLine: `${PREFIX}-oneLine`,
    tabs: `${PREFIX}-tabs`,
    devGrabSpace: `${PREFIX}-devGrabSpace`,
    closeButton: `${PREFIX}-closeButton`
};

const Root = styled('div')(({theme}) => ({
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
    },

    [`& .${classes.root}`]: {
        margin: 0,
        marginLeft: 15,
        padding: theme.spacing(1),
    },
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
        <MuiDialogTitle className={classes.root} {...other}>
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
        </MuiDialogTitle>
    );
});

const DialogContent = MuiDialogContent;

const DialogActions = MuiDialogActions;


EditTemplateColumnsDialog.propTypes = {
    template: PropTypes.object.isRequired,
    configuration: PropTypes.object.isRequired,
    dataClass: PropTypes.object,
    name: PropTypes.string.isRequired,
    onChange: PropTypes.func.isRequired
}

export default function EditTemplateColumnsDialog(props) {


    const [properties, setProperties] = useState(null);
    const [columns, setColumns] = useState(convertNullValuesInCollection(props.configuration.columns, getColumnMapper()));
    const [open, setOpen] = useState(false);
    const [activeTab, setActiveTab] = useState(0);
    const dialogContentRef = React.useRef(null);
    const templatesContext = useContext(TemplatesContext);
    const {templates} = templatesContext;
    const [enums, setEnums] = useState([]);

    useEffect(() => {
        setProperties(mergeProperties(props.configuration.properties, props.dataClass));
    }, [props.configuration.properties, props.dataClass]);

    useEffect(() => {
        setEnums(getTemplatesArtefacts(props.template, templates, 'enums'));
    }, [templates]);

    useEffect(() => {
        setProperties(mergeProperties(props.configuration.properties, props.dataClass));
    }, [props.configuration.properties, props.dataClass]);


    const handleClickOpen = () => {
        setOpen(true);
    };

    const handleSave = () => {
        props.onChange({properties, columns});
        setOpen(false);
    }

    const handleChangedTab = (event, newActiveTab) => {
        setActiveTab(newActiveTab);
    }

    const getUsedUuids = () => {
        return columns.map(column => column.uuid);
    }

    return (
        <Root>
            <Tooltip title={'Define columns'}>
                <Button
                    endIcon={<MoreHoriz/>}
                    onClick={handleClickOpen}
                >
                    {`${props.configuration.columns.length}`}
                </Button>
            </Tooltip>
            <Dialog
                onClose={handleSave}
                aria-labelledby="customized-dialog-title"
                open={open}
                fullWidth={true}
                maxWidth='lg' //'xs' | 'sm' | 'md' | 'lg' | 'xl' | false
            >
                <DialogTitle id="customized-dialog-title" onClose={handleSave}>
                    <div className={classes.oneLine}>
                        <Typography variant="h6">{'Define the list of columns'}</Typography>
                        <Tabs
                            className={classes.tabs}
                            value={activeTab}
                            onChange={handleChangedTab}
                            centered
                        >

                            <StyledTab label="Columns"/>
                            <StyledTab label="Properties"/>
                        </Tabs>
                        <div className={classes.devGrabSpace}/>
                    </div>
                </DialogTitle>
                <DialogContent
                    dividers
                    style={{height: '75vh'}}
                    ref={dialogContentRef}
                    classes={{
                        root: classes.root
                    }}>
                    {activeTab === 0 &&
                        <ColumnsTable
                            columns={columns}
                            properties={properties}
                            onChange={setColumns}
                            parentContentRef={dialogContentRef}
                        />}
                    {activeTab === 1 &&
                        <PropertiesTable
                            used={getUsedUuids()}
                            data={properties}
                            enums={enums}
                            onChange={setProperties}
                            title={'Class origin and defined properties:'}
                            parentContentRef={dialogContentRef}
                        />}
                </DialogContent>
                <DialogActions
                    classes={{
                        root: classes.root2
                    }}/>
            </Dialog>
        </Root>
    );
}

const StyledTab = Tab
