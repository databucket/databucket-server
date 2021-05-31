import React, {useState} from 'react';
import Input from '@material-ui/core/Input';
import MenuItem from '@material-ui/core/MenuItem';
import ListItemText from '@material-ui/core/ListItemText';
import Select from '@material-ui/core/Select';
import Checkbox from '@material-ui/core/Checkbox';
import {features} from "../utils/ViewFeatures";

const MenuProps = {
    PaperProps: {
        style: {
            width: 240,
        },
    },
};

export default function SelectMultiViewFeaturesLookup(props) {
    const [selectedFeatures, setSelectedFeatures] = useState(props.rowData['featuresIds'] != null ? props.rowData['featuresIds'] : []);

    const handleChange = (event) => {
        setSelectedFeatures(event.target.value);
        props.onChange(event.target.value);
    };

    return (
        <div>
            <Select
                id="features-multiple-checkbox"
                multiple
                value={selectedFeatures}
                onChange={handleChange}
                input={<Input/>}
                renderValue={(selected) => `[${selected.length}]`}
                MenuProps={MenuProps}
            >
                {features.map((feature) => (
                    <MenuItem key={feature.id} value={feature.id}>
                        <Checkbox checked={selectedFeatures.indexOf(feature.id) > -1}/>
                        <ListItemText primary={feature.name}/>
                    </MenuItem>
                ))}
            </Select>
        </div>
    );
}
