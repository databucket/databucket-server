import React, {useContext} from 'react';
import {ThemeProvider} from '@material-ui/core/styles';
import {LightTheme, DarkTheme} from './utils/Themes'
import AppRouter from './route/AppRouter'
import CssBaseline from "@material-ui/core/CssBaseline";
import CustomThemeContext from "./context/theme/CustomThemeContext";

export default function App() {

    window.apiURL = 'http://localhost:8080/api';
    // window.apiURL = './api';

    const [themeName] = useContext(CustomThemeContext);
    return (
        <ThemeProvider theme={getTheme(themeName)}>
            <CssBaseline>
                <AppRouter/>
            </CssBaseline>
        </ThemeProvider>
    );
}

function getTheme(name) {
    switch (name) {
        case 'dark':
            return DarkTheme;
        default:
            return LightTheme;
    }
}
