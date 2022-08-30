import React, {useContext} from 'react';
import {ThemeProvider} from '@material-ui/core/styles';
import {getTheme} from './utils/Themes'
import AppRouter from './route/AppRouter'
import CssBaseline from "@material-ui/core/CssBaseline";
import CustomThemeContext from "./context/theme/CustomThemeContext";
import {clearAllSearchedText, clearAllOrders} from "./utils/ConfigurationStorage";
import DynamicStylesSelector from "./components/DynamicStylesSelector";

export default function App() {

    clearAllSearchedText();
    clearAllOrders();
    const [themeName] = useContext(CustomThemeContext);
    return (
        <ThemeProvider theme={getTheme(themeName)}>
            <CssBaseline>
                <DynamicStylesSelector>
                    <AppRouter/>
                </DynamicStylesSelector>
            </CssBaseline>
        </ThemeProvider>
    );
}