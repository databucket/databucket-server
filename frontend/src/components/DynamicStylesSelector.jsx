import React from 'react';
import {useTheme} from "@material-ui/core/styles";

const DynamicStylesSelector = ({children}) => {
    const theme = useTheme();

    if (theme.palette.type === 'dark') {
        import("./styles/awesome-query-builder-styles-dark.css").then(()=>{});
        import("./styles/json-editor-styles-dark.css").then(()=>{});
    } else {
        import("./styles/awesome-query-builder-styles-light.css").then(()=>{});
        import("./styles/json-editor-styles-light.css").then(()=>{});
    }

    return (
        <div>
            {children}
        </div>
    );
}

export default DynamicStylesSelector;