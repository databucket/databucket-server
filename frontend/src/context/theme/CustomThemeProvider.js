import React, { useState} from "react";
import CustomThemeContext from "./CustomThemeContext";
import {LoadingButtonClasses} from "@mui/lab/LoadingButton";

const CustomThemeProvider = props => {
    const [customThemeName, setCustomThemeName] = useState(props.name);
    return (
        <CustomThemeContext.Provider value={[customThemeName, setCustomThemeName]}>
            {props.children}
        </CustomThemeContext.Provider>
    );
};

export default CustomThemeProvider;
