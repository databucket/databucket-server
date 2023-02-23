import React, {useState} from 'react';
import {styled} from '@mui/material/styles';
import PropTypes from "prop-types";
import MenuItem from "@mui/material/MenuItem";
import {IconButton, Menu, Tooltip} from "@mui/material";

const PREFIX = 'FilterMenuSelector';

const classes = {
    view: `${PREFIX}-view`,
    select: `${PREFIX}-select`,
    description: `${PREFIX}-description`
};

const Root = styled('div/')(({theme}) => ({
    flexGrow: 1,

    [`& .${classes.view}`]: {
        paddingLeft: theme.spacing(2),
        padding: theme.spacing(1)
    },

    [`& .${classes.select}`]: {
        marginLeft: '10px',
        padding: theme.spacing(1)
    },

    [`& .${classes.description}`]: {
        padding: theme.spacing(2),
        whiteSpace: "nowrap",
        overflow: "hidden",
        textOverflow: "ellipsis"
    }
}));

FilterMenuSelector.propTypes = {
    filters: PropTypes.array.isRequired,
    onFilterSelected: PropTypes.func.isRequired
}

export default function FilterMenuSelector(props) {


    const [anchorEl, setAnchorEl] = useState(null);
    const open = Boolean(anchorEl);

    const handleClick = (event) => {
        setAnchorEl(event.currentTarget);
    };

    const handleClose = () => {
        setAnchorEl(null);
    };

    const handleSelected = (filterItem) => {
        props.onFilterSelected(filterItem);
        handleClose();
    }

    if (props.filters != null && props.filters.length > 0)
        return (
            <Root>
                <Tooltip title={'Select filter'}>
                    <IconButton
                        className={classes.select}
                        aria-controls="long-menu"
                        onClick={handleClick}
                        color={'inherit'}
                        size="large">
                        <span className="material-icons">arrow_circle_down</span>
                    </IconButton>
                </Tooltip>
                <Menu
                    id="long-menu"
                    anchorEl={anchorEl}
                    keepMounted
                    open={open}
                    onClose={handleClose}
                    PaperProps={{
                        style: {
                            // maxHeight: 48 * 4.5,
                            minWidth: '15ch'
                        },
                    }}
                >
                    {props.filters.map((filterItem) => (
                        <MenuItem
                            key={filterItem.id}
                            selected={false}
                            onClick={() => handleSelected(filterItem)}
                        >
                            {filterItem.name}
                        </MenuItem>
                    ))}
                </Menu>
            </Root>
        );
    else
        return (<div/>);
}
