import React, {useContext, useEffect, useState} from 'react';
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
import EnumsContext from "../../context/enums/EnumsContext";
import {convertNullValuesInCollection} from "../../utils/JsonHelper";
import {getColumnMapper} from "../../utils/NullValueMappers";
import PropertiesTable, {mergeProperties} from "../utils/PropertiesTable";
import ColumnsTable from "../utils/ColumnsTable";

const PREFIX = 'EditColumnsDialog';

const classes = {
    root: `${PREFIX}-root`,
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
        <MuiDialogTitle disableTypography className={classes.root} {...other}>
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


EditColumnsDialog.propTypes = {
    configuration: PropTypes.object.isRequired,
    dataClass: PropTypes.object,
    name: PropTypes.string.isRequired,
    onChange: PropTypes.func.isRequired
}

export default function EditColumnsDialog(props) {

    const [properties, setProperties] = useState(null);
    const [columns, setColumns] = useState(convertNullValuesInCollection(props.configuration.columns, getColumnMapper()));
    const [open, setOpen] = useState(false);
    const [activeTab, setActiveTab] = useState(0);
    const enumsContext = useContext(EnumsContext);
    const {enums, fetchEnums} = enumsContext;
    const dialogContentRef = React.useRef(null);

    useEffect(() => {
        setProperties(mergeProperties(props.configuration.properties, props.dataClass));
    }, [props.configuration.properties, props.dataClass]);

    useEffect(() => {
        if (enums == null)
            fetchEnums();
    }, [enums, fetchEnums]);

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
                        root: classes.root
                    }}/>
            </Dialog>
        </Root>
    );
}

const StyledTab = Tab
