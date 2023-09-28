import React, {useState} from 'react';
import {Checkbox, Input, ListItemText, MenuItem, Select} from '@mui/material';
import {getRolesNames} from "../../utils/JsonHelper";

const MenuProps = {
    PaperProps: {
        style: {
            width: 240,
        },
    },
};

export default function SelectMultiRolesLookup(props) {
    const [selectedRoles, setSelectedRoles] = useState(
        props.rowData['rolesIds'] != null ? props.rowData['rolesIds'] : []);

    const handleChange = (event) => {
        setSelectedRoles(event.target.value);
        props.onChange(event.target.value);
    };

    return (
        <div>
            <Select
                id="roles-multiple-checkbox"
                multiple
                value={selectedRoles}
                onChange={handleChange}
                input={<Input/>}
                renderValue={(selected) => getRolesNames(props.roles, selected)}
                MenuProps={MenuProps}
            >
                {props.roles.map((role) => (
                    <MenuItem key={role.id} value={role.id}>
                        <Checkbox
                            checked={selectedRoles.indexOf(role.id) > -1}/>
                        <ListItemText primary={role.name}/>
                    </MenuItem>
                ))}
            </Select>
        </div>
    );
}
