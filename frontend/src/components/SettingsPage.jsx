import React, {useContext} from 'react';
import {MenuItem, Paper, Select} from "@material-ui/core";
import Typography from "@material-ui/core/Typography";
import {ThemeContext, ThemeContextProvider} from "../context/ThemeContext";
import {saveThemeName} from "../utils/ConfigurationStorage";

export default function SettingPage() {

    const [themeName, setThemeName] = useContext(ThemeContext);

    const handleChange = (event) => {
        let name = event.target.value;
        setThemeName(name);
        saveThemeName(name)
    };

    return (
        <div className="login-page-paper">
            <Paper className="Login" elevation={3}>
                <Typography variant="h4">
                    SettingsPage
                </Typography>
                <ThemeContextProvider>
                    <Select
                        value={themeName}
                        onChange={handleChange}
                        displayEmpty
                        inputProps={{'aria-label': 'Theme name'}}
                    >
                        <MenuItem value='light'>Light</MenuItem>
                        <MenuItem value='dark'>Dark</MenuItem>
                    </Select>
                </ThemeContextProvider>
            </Paper>
        </div>
    );
}
