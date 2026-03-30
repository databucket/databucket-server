import React, { useContext, useState } from 'react';
import {
    Button,
    Dialog,
    DialogActions as MuiDialogActions,
    DialogContent as MuiDialogContent,
    DialogTitle as MuiDialogTitle,
    IconButton,
    styled,
    Tooltip,
    Typography,
    Box
} from '@mui/material';
import { Close as CloseIcon, MoreHoriz } from '@mui/icons-material';
import PropTypes from 'prop-types';
import EnumsContext from "../../context/enums/EnumsContext";
import PropertiesTable from "../utils/PropertiesTable";

const PREFIX = 'EditClassFieldsDialog';

const classes = {
    root: `${PREFIX}-root`,
    root2: `${PREFIX}-root2`,
    root3: `${PREFIX}-root3`,
    closeButton: `${PREFIX}-closeButton`
};

const Root = styled('div')(({ theme }) => ({
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

const StyledDialogContent = styled(MuiDialogContent)(({ theme }) => ({
    padding: 0,
    '&:first-of-type': {
        paddingTop: 0,
    },
}));

const DialogTitle = ((props) => {
    const { children, onClose, ...other } = props;
    return (
        <MuiDialogTitle disableTypography className={classes.root} {...other}>
            <Box display="flex" justifyContent="space-between" alignItems="center">
                <Typography variant="h6">{children}</Typography>
                {onClose && (
                    <IconButton
                        aria-label="close"
                        className={classes.closeButton}
                        onClick={onClose}
                        size="large">
                        <CloseIcon />
                    </IconButton>
                )}
            </Box>
        </MuiDialogTitle>
    );
});

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
        props.onChange(data.map(({ title, path, type, enumId, uuid }) => ({ title, path, type, enumId, uuid })));
        setOpen(false);
    }

    return (
        <Root>
            <Tooltip title={'Configure properties'}>
                <Button
                    endIcon={<MoreHoriz />}
                    onClick={handleClickOpen}
                    color={'inherit'}
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
                <StyledDialogContent
                    dividers
                    style={{ height: '75vh' }}
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
                </StyledDialogContent>
                <DialogActions
                    classes={{
                        root: classes.root2
                    }} />
            </Dialog>
        </Root>
    );
}
