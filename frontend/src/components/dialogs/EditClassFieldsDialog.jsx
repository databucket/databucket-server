import React, {useContext, useState} from 'react';
import { styled } from '@mui/material/styles';
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
import EnumsContext from "../../context/enums/EnumsContext";
import PropertiesTable from "../utils/PropertiesTable";
import MuiDialogActions from "@mui/material/DialogActions";

const PREFIX = 'EditClassFieldsDialog';

const classes = {
    root: `${PREFIX}-root`,
    root2: `${PREFIX}-root2`,
    root3: `${PREFIX}-root3`,
    closeButton: `${PREFIX}-closeButton`
};

const Root = styled('div')((
    {
        theme
    }
) => ({
    [`& .${classes.root3}`]: {
        margin: 0,
        padding: theme.spacing(2),
    },

    [`& .${classes.closeButton}`]: {
        position: 'absolute',
        right: theme.spacing(1),
        top: theme.spacing(1),
        color: theme.palette.grey[500],
    }
}));

const DialogTitle = ((props) => {
    const {children,  onClose, ...other} = props;
    return (
        <MuiDialogTitle disableTypography className={classes.root} {...other}>
            <Typography variant="h6">{children}</Typography>
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

EditClassFieldsDialog.propTypes = {
    configuration: PropTypes.array.isRequired,
    name: PropTypes.string.isRequired,
    description: PropTypes.string.isRequired,
    onChange: PropTypes.func.isRequired
}

export default function EditClassFieldsDialog(props) {

    const [data, setData] = useState(props.configuration);
    const [open, setOpen] = useState(false);
    const enumsContext = useContext(EnumsContext);
    const dialogContentRef = React.useRef(null);

    const handleClickOpen = () => {
        setOpen(true);
    };

    const handleSave = () => {
        props.onChange(data.map(({title, path, type, enumId, uuid}) => ({title, path, type, enumId, uuid})));
        setOpen(false);
    }

    return (
        <Root>
            <Tooltip title={'Configure properties'}>
                <Button
                    endIcon={<MoreHoriz/>}
                    onClick={handleClickOpen}
                >
                    {`${props.configuration.length}`}
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
                    {'Define class properties'}
                </DialogTitle>
                <DialogContent
                    dividers
                    style={{height: '75vh'}}
                    ref={dialogContentRef}
                    classes={{
                        root: classes.root
                    }}>
                    {open &&
                    <PropertiesTable
                        data={data}
                        enums={enumsContext.enums}
                        onChange={setData}
                        title={''}
                        parentContentRef={dialogContentRef}
                    />
                    }
                </DialogContent>
                <DialogActions
                    classes={{
                        root: classes.root2
                    }} />
            </Dialog>
        </Root>
    );
}