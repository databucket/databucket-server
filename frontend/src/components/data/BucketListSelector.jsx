import React, {useContext, useEffect, useState} from 'react';
import ListItem from "@material-ui/core/ListItem";
import ListItemIcon from "@material-ui/core/ListItemIcon";
import ListItemText from "@material-ui/core/ListItemText";
import List from "@material-ui/core/List";
import DynamicIcon from "../utils/DynamicIcon";
import AccessContext from "../../context/access/AccessContext";

export default function BucketListSelector() {

    const accessContext = useContext(AccessContext);
    const {groups, buckets, activeGroup, activeBucket, addTab} = accessContext;
    const [filteredBuckets, setFilteredBuckets] = useState([]);

    useEffect(() => {
        if (buckets != null) {
            if (groups != null && groups.length > 0) {
                let fBuckets = [];
                if (activeGroup.bucketsIds != null)
                    activeGroup.bucketsIds.forEach(id => {
                        fBuckets = [...fBuckets, buckets.find(bucket => bucket.id === id)];
                    });
                setFilteredBuckets(fBuckets);
            } else
                setFilteredBuckets(buckets);
        } else
            setFilteredBuckets([]);

    }, [groups, buckets, activeGroup]);

    const onClick = (bucket) => {
        addTab(bucket);
    }

    const getBucketName = (name) => {
        return name.length > 17 ? name.substring(0, 15) + "..." : name;
    }

    return (
        <List>
            {filteredBuckets.map((bucket) => (
                <ListItem
                    button
                    selected={activeBucket != null ? bucket.id === activeBucket.id : false}
                    key={bucket.name}
                    onClick={() => onClick(bucket)}
                >
                    <ListItemIcon><DynamicIcon iconName={bucket.iconName}/></ListItemIcon>
                    <ListItemText primary={getBucketName(bucket.name)}/>
                </ListItem>
            ))}
        </List>
    );
}