import React from 'react';
import { createRoot } from 'react-dom/client';
import App from './App';
import * as serviceWorker from './serviceWorker';
import {getThemeName} from "./utils/ConfigurationStorage";
import CustomThemeProvider from "./context/theme/CustomThemeProvider";
import '@fontsource/roboto/300.css';
import '@fontsource/roboto/400.css';
import '@fontsource/roboto/500.css';
import '@fontsource/roboto/700.css';
import './index.css';

const container = document.getElementById('root');
const root = createRoot(container);

root.render(
    <CustomThemeProvider name={getThemeName()}>
        <App/>
    </CustomThemeProvider>
);

// If you want your app to work offline and load faster, you can change
// unregister() to register() below. Note this comes with some pitfalls.
// Learn more about service workers: https://bit.ly/CRA-PWA
serviceWorker.unregister();
