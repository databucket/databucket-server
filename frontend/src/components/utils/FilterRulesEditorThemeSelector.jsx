import React from 'react';
import {useTheme} from "@material-ui/core/styles";

const FilterRulesEditorThemeSelector = ({children}) => {
    const theme = useTheme();

    if (theme.palette.type === 'dark')
        require("./awesome-query-builder-styles-dark.css");
     else
        require("./awesome-query-builder-styles-light.css");

    return (
        <div>
            {children}
        </div>
    );
}

export default FilterRulesEditorThemeSelector;