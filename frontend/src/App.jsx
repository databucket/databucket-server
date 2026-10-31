import React from 'react';
import AppRouter from './route/AppRouter'
import {CssBaseline, StyledEngineProvider} from "@mui/material";
import {getThemeName} from "./utils/ConfigurationStorage";
import StaticStylesSelector from "./components/StaticStylesSelector";

export default function App() {

    return (
        <StyledEngineProvider injectFirst>
            <CssBaseline>
                <StaticStylesSelector themeName={getThemeName()}>
                    <AppRouter/>
                </StaticStylesSelector>
            </CssBaseline>
        </StyledEngineProvider>
    );
}