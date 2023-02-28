import React, {useMemo, useState} from "react";
import {getThemeName, saveThemeName} from "../../utils/ConfigurationStorage";
import CustomThemeContext from "./CustomThemeContext";
import {createTheme, ThemeProvider} from "@mui/material/styles";
import {getTheme} from "../../utils/Themes";


const CustomThemeProvider = props => {
    const [mode, setMode] = useState(getThemeName());
    const colorMode = useMemo(() => ({
            toggleColorMode: () => {
                setMode((prevMode) => {
                    let newMode = prevMode === 'light' ? 'dark' : 'light';
                    saveThemeName(newMode);
                    return newMode;
                });
            },
        }), [],
    );
    const theme = useMemo(() => createTheme(getTheme(mode)), [mode]);
    return (
        <CustomThemeContext.Provider value={colorMode}>
            <ThemeProvider theme={theme}>
                {props.children}
            </ThemeProvider>
        </CustomThemeContext.Provider>
    );
};

export default CustomThemeProvider;
