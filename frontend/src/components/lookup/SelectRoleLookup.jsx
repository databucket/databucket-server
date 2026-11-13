import React, {useState} from 'react';
import {Input, MenuItem, Select} from '@mui/material';

// one role per user (since 3.12.0); a higher role includes the lower ones: SUPER > ADMIN > MEMBER > ROBOT > AGENT
const ORDER = ['SUPER', 'ADMIN', 'MEMBER', 'ROBOT', 'AGENT'];

export default function SelectRoleLookup(props) {
    const initial = props.rowData['rolesIds'] != null && props.rowData['rolesIds'].length > 0 ? props.rowData['rolesIds'][0] : '';
    const [selectedRole, setSelectedRole] = useState(initial);

    const handleChange = (event) => {
        setSelectedRole(event.target.value);
        props.onChange(event.target.value === '' ? [] : [event.target.value]);
    };

    const roles = [...props.roles].sort((a, b) => ORDER.indexOf(a.name) - ORDER.indexOf(b.name));
    return (
        <Select id="role-select" value={selectedRole} onChange={handleChange} input={<Input/>} displayEmpty>
            <MenuItem value=""><em>- none -</em></MenuItem>
            {roles.map(role => <MenuItem key={role.id} value={role.id}>{role.name}</MenuItem>)}
        </Select>
    );
}
