import React, {useState} from 'react';
import Input from '@material-ui/core/Input';
import MenuItem from '@material-ui/core/MenuItem';
import ListItemText from '@material-ui/core/ListItemText';
import Select from '@material-ui/core/Select';
import Checkbox from '@material-ui/core/Checkbox';

const MenuProps = {
    PaperProps: {
        style: {
            width: 240,
        },
    },
};

export default function SelectMultiViewFeaturesLookup(props) {
    const [selectedFeatures, setSelectedFeatures] = useState(props.rowData['featuresIds'] != null ? props.rowData['featuresIds'] : []);

    const features = [
        {id: 1, name: 'Search'},
        {id: 2, name: 'Details'},
        {id: 3, name: 'Creation'},
        {id: 4, name: 'Modifying'},
        {id: 5, name: 'Removal'},
        {id: 6, name: 'History'},
        {id: 7, name: 'Tasks'},
        {id: 8, name: 'Reservation'},
        {id: 9, name: 'Import'},
        {id: 10, name: 'Export'}
    ];

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
