import React from "react";
import {useTheme} from "@mui/material/styles";
import LogoBlue from "../../images/databucket-logo.svg";
import LogoWhite from "../../images/databucket-logo-white.svg";

// the width and height of the logo's viewBox
const RATIO = 263.2 / 40;

/**
 * The Databucket logo: blue, white in the dark theme. Its margins keep the place the old logo image took
 * (65 px high with empty space around the frame).
 */
export default function DatabucketLogo({height = 40, style}) {
    const theme = useTheme();
    return <img src={theme.palette.mode === 'dark' ? LogoWhite : LogoBlue} alt='Databucket'
                height={height} width={Math.round(height * RATIO)} style={{margin: '12px 0 13px', ...style}}/>;
}
