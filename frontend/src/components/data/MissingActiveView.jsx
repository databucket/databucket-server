import React from 'react';
import {styled} from '@mui/material/styles';
import {Typography} from "@mui/material";

const StyledTypography = styled(Typography)(({theme}) => ({
    padding: theme.spacing(3),
    whiteSpace: "nowrap",
    overflow: "hidden",
    textOverflow: "ellipsis",
}));

export default function MissingActiveView() {

    return (
        <StyledTypography
            variant="h6"
            color={'error'}
        >
            {'You do not have permission to any view of this bucket.'}
        </StyledTypography>
    );
}
