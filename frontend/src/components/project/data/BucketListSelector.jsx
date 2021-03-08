import React, {useContext, useEffect, useState} from 'react';
import ListItem from "@material-ui/core/ListItem";
import ListItemIcon from "@material-ui/core/ListItemIcon";
import ListItemText from "@material-ui/core/ListItemText";
import List from "@material-ui/core/List";
import DynamicIcon from "../../utils/DynamicIcon";
import AccessTreeContext from "../../../context/accessTree/AccessTreeContext";

export default function BucketListSelector() {

    const accessTreeContext = useContext(AccessTreeContext);
    const {accessTree, activeGroup, activeBucket, addTab} = accessTreeContext;
    const [buckets, setBuckets] = useState([]);

    useEffect(() => {
        if (accessTree != null) {
            if (accessTree.groups != null && accessTree.groups.length > 0) {
                let buckets = [];
                if (activeGroup.bucketsIds != null)
                    activeGroup.bucketsIds.forEach(id => {
                        buckets = [...buckets, accessTree.buckets.find(bucket => bucket.id === id)];
                    });
                setBuckets(buckets);
            } else
                setBuckets(accessTree.buckets);
        } else
            setBuckets([]);

    }, [accessTree, activeGroup]);

    const onClick = (bucket) => {
        addTab(bucket);
    }

    const getBucketName = (name) => {
        return name.length > 17 ? name.substring(0, 15) + "..." : name;
    }

    return (
        <List>
            {buckets.map((bucket) => (
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