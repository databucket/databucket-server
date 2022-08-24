import React, {useContext, useEffect, useState} from 'react';
import ListItem from "@material-ui/core/ListItem";
import ListItemIcon from "@material-ui/core/ListItemIcon";
import ListItemText from "@material-ui/core/ListItemText";
import List from "@material-ui/core/List";
import DynamicIcon from "../utils/DynamicIcon";
import AccessContext from "../../context/access/AccessContext";
import {Tooltip} from "@material-ui/core";
import PropTypes from "prop-types";

BucketListSelector.propTypes = {
    leftPanelWidth: PropTypes.number.isRequired
}

export default function BucketListSelector(props) {

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

    const getBucketVisibleName = (name) => {
        return name.length > 23 ? name.substring(0, 22) + "..." : name;
    }

    const getTooltipName = (name, visibleName) => {
        if (visibleName.endsWith("...") || props.leftPanelWidth < 100)
            return <h2>{name}</h2>;
        else
            return "";
    }

    return (
        <List>
            {filteredBuckets
                .sort((a, b) => {
                    return a.name > b.name ? 1 : -1
                })
                .map((bucket) => (
                    <Tooltip placement="right" title={getTooltipName(bucket.name, getBucketVisibleName(bucket.name))}>
                        <ListItem
                            button
                            selected={activeBucket != null ? bucket.id === activeBucket.id : false}
                            key={bucket.name}
                            onClick={() => onClick(bucket)}
                        >
                            <ListItemIcon><DynamicIcon iconName={bucket.iconName}/></ListItemIcon>
                            <ListItemText primary={getBucketVisibleName(bucket.name)}/>
                        </ListItem>
                    </Tooltip>
                ))}
        </List>
    );
}