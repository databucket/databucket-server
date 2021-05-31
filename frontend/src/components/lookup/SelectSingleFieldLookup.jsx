import React from 'react';
import MenuItem from '@material-ui/core/MenuItem';
import ListItemText from '@material-ui/core/ListItemText';
import Select from '@material-ui/core/Select';
import PropTypes from "prop-types";
import {ListSubheader} from "@material-ui/core";

SelectSingleFieldLookup.propTypes = {
    selected: PropTypes.string.isRequired,
    properties: PropTypes.array.isRequired,
    onChange: PropTypes.func.isRequired
}

export default function SelectSingleFieldLookup(props) {

    const handleChange = (event) => {
        if (event.target.value != null)
            props.onChange(event.target.value);
    };

    return (
        <div>
            <Select
                id="property-single-select"
                value={props.selected}
                onChange={handleChange}
            >
                {props.properties.length > 0 &&
                <ListSubheader color={'primary'}>Properties</ListSubheader>}
                {props.properties.length > 0 &&
                    props.properties.map((field) => (
                        <MenuItem key={field.uuid} value={field.uuid}>
                            <ListItemText primary={field.title}/>
                        </MenuItem>
                ))}
                <ListSubheader color={'primary'}>Common</ListSubheader>}
                {commonFields.map((field) => (
                    <MenuItem key={field.uuid} value={field.uuid}>
                        <ListItemText primary={field.title}/>
                    </MenuItem>
                ))}
            </Select>
        </div>
    );
}

export const commonFields = [
    {title: 'Id', path: 'data_id', type: 'numeric', uuid: 'uuid_data_id'},
    {title: 'Tag', path: 'tag_id', type: 'numeric', uuid: 'uuid_tag_id'},
    {title: 'Reserved', path: 'reserved', type: 'boolean', uuid: 'uuid_reserved'},
    {title: 'Owner', path: 'owner', type: 'string', uuid: 'uuid_owner'},
    {title: 'Created at', path: 'createdAt', type: 'datetime', uuid: 'uuid_created_at'},
    {title: 'Created by', path: 'createdBy', type: 'string', uuid: 'uuid_created_by'},
    {title: 'Modified at', path: 'modifiedAt', type: 'datetime', uuid: 'uuid_modified_at'},
    {title: 'Modified by', path: 'modifiedBy', type: 'string', uuid: 'uuid_modified_by'}
];
