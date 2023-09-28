import React from 'react';
import {Alert, AlertTitle, Snackbar, styled} from '@mui/material';

const PREFIX = 'MessageBox';

const classes = {
    root: `${PREFIX}-root`
};

const Root = styled('div')((
    {
        theme
    }
) => ({
    [`&.${classes.root}`]: {
        width: '100%',
        '& > * + *': {
            marginTop: theme.spacing(2),
        },
    }
}));

export const MessageBox = (props) => {

    return (
        <Root className={classes.root}>
            <Snackbar
                anchorOrigin={{
                    vertical: 'top',
                    horizontal: 'center',
                }}
                open={props.config.open}
                onClose={props.onClose}
                autoHideDuration={7000}
            >
                <Alert severity={props.config.severity}>
                    <AlertTitle>{props.config.title}</AlertTitle>
                    {props.config.message}
                </Alert>
            </Snackbar>
        </Root>
    );
}