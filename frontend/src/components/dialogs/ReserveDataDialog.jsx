import React, { useContext, useState } from 'react';
import {
    Button,
    Checkbox,
    FormControl,
    FormControlLabel,
    IconButton,
    InputLabel,
    Menu,
    MenuItem,
    Select,
    Slider,
    Tooltip,
    Typography
} from '@mui/material';
import { getUsername, hasAdminRole } from "../../utils/ConfigurationStorage";
import AccessContext from "../../context/access/AccessContext";

const marks = [
    { value: 1, label: '1' },
    { value: 5, label: '5' },
    { value: 10, label: '10' },
    { value: 15, label: '15' },
    { value: 20, label: '20' }
];

export default function ReserveDataDialog(props) {
    const [anchorEl, setAnchorEl] = useState(null);
    const open = Boolean(anchorEl);
    const accessContext = useContext(AccessContext);
    const { users } = accessContext;
    const [state, setState] = useState({ random: false, number: 1, username: getUsername() });

    const handleMenu = (event) => {
        setAnchorEl(event.currentTarget);
    };

    const handleClose = () => {
        setAnchorEl(null);
    };

    const handleChangeRandom = (event) => {
        setState({ ...state, random: event.target.checked });
    };

    const handleChangeNumber = (event, newValue) => {
        setState({ ...state, number: newValue });
    };

    const onChangeUser = (user) => {
        setState({ ...state, username: user.username });
    };

    const handleReserve = () => {
        props.onReserve(state);
        handleClose();
    };

    return (
        <>
            <Tooltip title={'Reserve data'}>
                <IconButton
                    onClick={handleMenu}
                    color={'inherit'}
                    style={{ marginLeft: '10px', padding: '8px' }}
                    size="large">
                    <span className="material-icons">add_task</span>
                </IconButton>
            </Tooltip>
            <Menu
                id="menu-appbar"
                anchorEl={anchorEl}
                anchorOrigin={{ vertical: 'top', horizontal: 'right' }}
                keepMounted
                transformOrigin={{ vertical: 'top', horizontal: 'right' }}
                open={open}
                onClose={handleClose}
            >
                <div style={{ display: 'flex', flexDirection: 'column', padding: '15px' }}>
                    <FormControl style={{ margin: '8px 0', minWidth: 150, display: 'flex', flexDirection: 'column' }}>
                        <Typography gutterBottom>Number of data rows</Typography>
                        <Slider
                            value={state.number}
                            aria-labelledby="discrete-slider-steps"
                            step={1}
                            marks={marks}
                            min={1}
                            max={20}
                            valueLabelDisplay="auto"
                            onChange={handleChangeNumber}
                        />
                    </FormControl>
                    {hasAdminRole() && (
                        <FormControl style={{ margin: '8px 0', minWidth: 150, display: 'flex', flexDirection: 'column' }}>
                            <InputLabel id="user-select-label">Target owner</InputLabel>
                            <Select
                                labelId="user-select-label"
                                id="user-select"
                                value={state.username}
                                variant="standard"
                            >
                                {users.map((user) => (
                                    <MenuItem key={user.id} value={user.username} onClick={() => onChangeUser(user)}>
                                        {user.username}
                                    </MenuItem>
                                ))}
                            </Select>
                        </FormControl>
                    )}
                    <FormControlLabel
                        control={<Checkbox checked={state.random} onChange={handleChangeRandom} name="checkRandom" />}
                        label="Random data"
                        style={{ margin: '8px 0' }}
                    />
                    <FormControl style={{ margin: '8px 0', minWidth: 150 }}>
                        <Button variant="contained" color="secondary" onClick={handleReserve}>
                            Reserve
                        </Button>
                    </FormControl>
                </div>
            </Menu>
        </>
    );
}
