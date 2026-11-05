import React, {forwardRef} from 'react';
import {Icon, IconButton, useTheme} from '@mui/material';
import {parseCustomSvg} from "./SvgHelper";
import PropTypes from "prop-types";
import {getIconColor} from "../../utils/MaterialTableHelper";

// A button with an icon (a Material icon by name, or an SVG) in its color, as the icon shows in the tables
const StyledIconButton = forwardRef(
    function StyledIconButton({onClick, iconName, iconColor, iconSvg, ...props},
        ref) {
        const theme = useTheme();

        if (iconSvg) {
            return (
                <IconButton onClick={onClick}
                            size="large"
                            {...props}
                            ref={ref}>
                    {parseCustomSvg(iconSvg,
                        getIconColor(theme.palette.mode, iconColor))}
                </IconButton>
            );
        } else {
            return (
                <IconButton onClick={onClick}
                            size="large"
                            {...props}
                            ref={ref}>
                    <Icon sx={{color: iconColor}}>{iconName}</Icon>
                </IconButton>
            );
        }
    });

StyledIconButton.propTypes = {
    iconName: PropTypes.string,
    iconColor: PropTypes.string,
    iconSvg: PropTypes.string,
    onClick: PropTypes.func
};

export default StyledIconButton;
