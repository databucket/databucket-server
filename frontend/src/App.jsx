import React from 'react';
import AppRouter from './route/AppRouter'
import {CssBaseline, StyledEngineProvider} from "@mui/material";
import "./components/styles/json-editor-styles.css";

export default function App() {

    return (
        <StyledEngineProvider injectFirst>
            <CssBaseline>
                <div>
                    <AppRouter/>
                </div>
            </CssBaseline>
        </StyledEngineProvider>
    );
}
