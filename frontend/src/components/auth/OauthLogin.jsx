import React from "react";
import {Box, Button, Typography} from "@material-ui/core";

export default function OauthLoginComponent({authOptions}) {
    return (
        <Box className="Container">
            <Typography className="Title" variant="h5">
                Other Options
            </Typography>
            {authOptions && authOptions.map(option => {
                return (
                    <Button
                        key={option.name}
                        component="button"
                        color="inherit"
                        href={option.url}
                    >
                        {option.name}
                    </Button>)
            })}
        </Box>
    );
}
