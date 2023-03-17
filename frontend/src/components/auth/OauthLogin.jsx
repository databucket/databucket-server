import React from "react";
import {Box, Button, Typography} from "@material-ui/core";

export default function OauthLoginComponent({authOptions}) {
    return (
        <Box style={{
            marginBottom: "5vh",
            alignItems: "center",
            justifyContent: "center",
            display: "flex",
            flexDirection: "column"
        }}>
            <Typography variant="h5" style={{padding: "5vh 0"}}>
                Other Options
            </Typography>
            {authOptions && authOptions.map(option => {
                return (
                    <Button
                        key={option.name}
                        variant="contained"
                        component="button"
                        color="primary"
                        href={option.url}
                    >
                        {option.name}
                    </Button>)
            })}
        </Box>
    );
}
