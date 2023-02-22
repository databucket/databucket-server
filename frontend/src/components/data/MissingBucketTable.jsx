import React from 'react';
import { styled } from '@mui/material/styles';
import {Typography} from "@mui/material";
const PREFIX = 'MissingBucketTable';

const classes = {
    message: `${PREFIX}-message`
};

const StyledTypography = styled(Typography)((
    {
        theme
    }
) => ({
    [`&.${classes.message}`]: {
        padding: theme.spacing(3),
        whiteSpace: "nowrap",
        overflow: "hidden",
        textOverflow: "ellipsis",
    }
}));

export default function MissingBucketTable() {

    return (
        <StyledTypography
            variant="h6"
            color={'error'}
            className={classes.message}
        >
            {'You do not have permission to any bucket.'}
        </StyledTypography>
    );
}