import React, {useEffect} from 'react';

const StaticStylesSelector = ({themeName, children}) => {

    useEffect(() => {
        if (themeName === 'dark') {
            import("./styles/json-editor-styles-dark.css").then(() => {
            });
        } else {
            import("./styles/json-editor-styles-light.css").then(() => {
            });
        }
    }, [themeName]);

    return (
        <div>
            {children}
        </div>
    );
}

export default StaticStylesSelector;
