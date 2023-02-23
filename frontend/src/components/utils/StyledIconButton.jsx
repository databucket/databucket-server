import React, {forwardRef} from 'react';
import {styled, useTheme} from '@mui/material/styles';
import IconButton from "@mui/material/IconButton";
import {parseCustomSvg} from "./SvgHelper";
import PropTypes from "prop-types";
import {getIconColor} from "../../utils/MaterialTableHelper";
import {Icon} from "@mui/material";

const PREFIX = 'StyledIconButton';

const classes = {
    customStyles: `${PREFIX}-customStyles`
};

const TheStyledIconButton = styled(IconButton)(({theme}) => ({
    color: (props) => getIconColor(theme.palette.mode, props.iconColor)
}));

TheStyledIconButton.propTypes = {
    iconName: PropTypes.string.isRequired,
    iconColor: PropTypes.string.isRequired,
    iconSvg: PropTypes.string.isRequired,
    onClick: PropTypes.func
};

export default forwardRef(function StyledIconButton({onClick, iconName, iconColor, iconSvg, ...props}, ref) {
    const theme = useTheme();

    if (iconSvg !== null) {
        return (
            <TheStyledIconButton onClick={onClick}
                                 size="large"
                                 {...props}
                                 ref={ref}>
                {parseCustomSvg(iconSvg, getIconColor(theme.palette.mode, iconColor))}
            </TheStyledIconButton>
        );
    } else {
        return (
            <IconButton onClick={onClick}
                        className={classes.customStyles}
                        size="large"
                        {...props}
                        ref={ref}>
                <Icon>{iconName}</Icon>
            </IconButton>
        );
    }
});
