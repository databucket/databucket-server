import React, {useEffect} from 'react';

const DynamicStylesSelector = ({themeName, children}) => {

    useEffect(() => {
        if (themeName === 'dark') {
            import("./styles/awesome-query-builder-styles-dark.css").then(()=>{});
            import("./styles/json-editor-styles-dark.css").then(()=>{});
        } else {
            import("./styles/awesome-query-builder-styles-light.css").then(()=>{});
            import("./styles/json-editor-styles-light.css").then(()=>{});
        }
    }, [themeName]);

    return (
        <div>
            {children}
        </div>
    );
}

export default DynamicStylesSelector;