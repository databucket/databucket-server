import React, {useLayoutEffect, useMemo, useState} from "react";
import {getThemeName, saveThemeName} from "../../utils/ConfigurationStorage";
import CustomThemeContext from "./CustomThemeContext";
import {createTheme, ThemeProvider} from "@mui/material";
import {getTheme} from "../../utils/Themes";
import DraggablePaper from "../../components/utils/DraggablePaper";

const draggableDialogs = {
    components: {
        MuiDialog: {
            defaultProps: {
                PaperComponent: DraggablePaper
            }
        },
        MuiDialogTitle: {
            styleOverrides: {
                root: {
                    cursor: 'move'
                }
            }
        }
    }
};

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
    const theme = useMemo(() => createTheme(getTheme(mode), draggableDialogs), [mode]);
    // global styles that depend on the theme (components/styles/json-editor-styles.css) select by this attribute
    useLayoutEffect(() => {
        document.documentElement.dataset.theme = mode;
    }, [mode]);
    return (
        <CustomThemeContext.Provider value={colorMode}>
            <ThemeProvider theme={theme}>
                {props.children}
            </ThemeProvider>
        </CustomThemeContext.Provider>
    );
};

export default CustomThemeProvider;
