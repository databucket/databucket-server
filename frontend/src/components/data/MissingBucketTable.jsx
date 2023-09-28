import React from 'react';
import {styled, Typography} from "@mui/material";

const StyledTypography = styled(Typography)(({theme}) => ({
    padding: theme.spacing(3),
    whiteSpace: "nowrap",
    overflow: "hidden",
    textOverflow: "ellipsis",
}));

export default function MissingBucketTable() {
    return (
        <StyledTypography
            variant="h6"
            color={'error'}
        >
            {'You do not have permission to any bucket.'}
        </StyledTypography>
    );
}
