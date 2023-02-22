import React, {useEffect, useState} from 'react';
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
import TemplateTabs from "../management/templatesConfig/_TemplConfigTabs";

const PREFIX = 'EditTemplateConfigurationDialog';

const classes = {
    root: `${PREFIX}-root`,
    root2: `${PREFIX}-root2`,
    closeButton: `${PREFIX}-closeButton`
};

const Root = styled('div')((
    {
        theme
    }
) => ({
    [`& .${classes.root2}`]: {
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


EditTemplateConfigurationDialog.propTypes = {
    name: PropTypes.string.isRequired,
    rowData: PropTypes.object.isRequired,
    onChange: PropTypes.func.isRequired
}

export default function EditTemplateConfigurationDialog(props) {

    const [open, setOpen] = useState(false);
    const [template, setTemplate] = useState(null);

    // must be initiated by hook
    useEffect(() => {
        setTemplate(props.rowData);
    }, [props.rowData]);

    const handleClickOpen = () => {
        setOpen(true);
    };

    const handleSave = () => {
        props.onChange(template['configuration']);
        setOpen(false);
    }

    return (
        <Root>
            <Tooltip title='Configuration'>
                <Button
                    endIcon={<MoreHoriz/>}
                    onClick={handleClickOpen}
                >
                    {``}
                </Button>
            </Tooltip>
            <Dialog
                onClose={handleSave}
                aria-labelledby="customized-dialog-title"
                open={open}
                fullWidth={true}
                maxWidth='xl' //'xs' | 'sm' | 'md' | 'lg' | 'xl' | false
            >
                <DialogTitle id="customized-dialog-title" onClose={handleSave}>
                    {`Template: ${props.name}`}
                </DialogTitle>
                <DialogContent
                    dividers
                    classes={{
                        root: classes.root
                    }}>
                    <TemplateTabs
                        template={template}
                        setTemplate={setTemplate}
                    />
                </DialogContent>
            </Dialog>
        </Root>
    );
}