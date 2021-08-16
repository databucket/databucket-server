import React, {useContext} from 'react';
import {ThemeProvider} from '@material-ui/core/styles';
import {getTheme} from './utils/Themes'
import AppRouter from './route/AppRouter'
import CssBaseline from "@material-ui/core/CssBaseline";
import CustomThemeContext from "./context/theme/CustomThemeContext";
import {clearAllSearchedText} from "./utils/ConfigurationStorage";
import FilterRulesEditorThemeSelector from "./components/utils/FilterRulesEditorThemeSelector";

export default function App() {

    clearAllSearchedText();
    const [themeName] = useContext(CustomThemeContext);
    return (
        <ThemeProvider theme={getTheme(themeName)}>
            <CssBaseline>
                <FilterRulesEditorThemeSelector>
                    <AppRouter/>
                </FilterRulesEditorThemeSelector>
            </CssBaseline>
        </ThemeProvider>
    );
}