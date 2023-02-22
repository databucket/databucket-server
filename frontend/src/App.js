import React, {useContext} from 'react';
import { ThemeProvider, StyledEngineProvider } from '@mui/material/styles';
import {getTheme} from './utils/Themes'
import AppRouter from './route/AppRouter'
import CssBaseline from "@mui/material/CssBaseline";
import CustomThemeContext from "./context/theme/CustomThemeContext";
import {clearAllSearchedText, clearAllOrders} from "./utils/ConfigurationStorage";
import StaticStylesSelector from "./components/StaticStylesSelector";

export default function App() {

    clearAllSearchedText();
    clearAllOrders();
    const [themeName] = useContext(CustomThemeContext);
    return (
        <StyledEngineProvider injectFirst>
            <ThemeProvider theme={getTheme(themeName)}>
                <CssBaseline>
                    <StaticStylesSelector themeName={themeName}>
                        <AppRouter/>
                    </StaticStylesSelector>
                </CssBaseline>
            </ThemeProvider>
        </StyledEngineProvider>
    );
}