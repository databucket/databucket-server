import React, {useContext} from 'react';
import {ThemeProvider} from '@material-ui/core/styles';
import {LightTheme, DarkTheme} from './utils/Themes'
import AppRouter from './route/AppRouter'
import {ThemeContext} from "./context/ThemeContext";
import CssBaseline from "@material-ui/core/CssBaseline";
// import {getProjectId, getProjectName, getRoles, getThemeName, getToken} from "./utils/ConfigurationStorage";
// import DatabucketMainDrawer from './components/DatabucketMainDrower';
// import ConditionsTable from './components/conditionsTable/ConditionsTable';

export default function App() {

    window.apiURL = 'http://localhost:8080/api';
    // window.apiURL = './api';

    // console.log("Theme: " + getThemeName());
    // console.log("Token: " + getToken());
    // console.log("ProjectId: " + getProjectId());
    // console.log("ProjectName: " + getProjectName());
    // console.log("Roles: " + getRoles());

    const [themeName] = useContext(ThemeContext);
    return (
        <ThemeProvider theme={getTheme(themeName)}>
            {/*<DatabucketMainDrawer />*/}
            {/* <ConditionsTable /> */}
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
