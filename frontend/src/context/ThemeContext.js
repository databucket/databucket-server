import React, { useState, createContext } from "react";

export const ThemeContext = createContext([null, {}]);

export const ThemeContextProvider = props => {
    const [themeName, setThemeName] = useState(props.name);
    return (
        <ThemeContext.Provider value={[themeName, setThemeName]}>
            {props.children}
        </ThemeContext.Provider>
    );
};