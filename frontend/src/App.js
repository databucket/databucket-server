import React, {useContext} from 'react';
import {ThemeProvider} from '@material-ui/core/styles';
import {getTheme} from './utils/Themes'
import AppRouter from './route/AppRouter'
import CssBaseline from "@material-ui/core/CssBaseline";
import CustomThemeContext from "./context/theme/CustomThemeContext";

export default function App() {

    const [themeName] = useContext(CustomThemeContext);
    return (
        <ThemeProvider theme={getTheme(themeName)}>
            <CssBaseline>
                <AppRouter/>
            </CssBaseline>
        </ThemeProvider>
    );
}