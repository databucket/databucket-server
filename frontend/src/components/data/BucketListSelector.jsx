import React, {useContext, useEffect, useState} from 'react';
import {
    List,
    ListItemButton,
    ListItemIcon,
    ListItemText,
    Tooltip
} from "@mui/material";
import AccessContext from "../../context/access/AccessContext";
import StyledIcon from "../utils/StyledIcon";

export default function BucketListSelector(props) {

    const accessContext = useContext(AccessContext);
    const {groups, buckets, activeGroup, activeBucket, addTab} = accessContext;
    const [filteredBuckets, setFilteredBuckets] = useState([]);

    useEffect(() => {
        if (buckets != null) {
            if (groups != null && groups.length > 0) {
                let fBuckets = [];
                if (activeGroup.bucketsIds != null) {
                    activeGroup.bucketsIds.forEach(id => {
                        fBuckets = [...fBuckets,
                            buckets.find(bucket => bucket.id === id)];
                    });
                }
                setFilteredBuckets(fBuckets);
            } else {
                setFilteredBuckets(buckets);
            }
        } else {
            setFilteredBuckets([]);
        }

    }, [groups, buckets, activeGroup]);

    const onClick = (bucket) => {
        addTab(bucket);
    }

    const getBucketVisibleName = (name) => {
        return name.length > 23 ? name.substring(0, 22) + "..." : name;
    }

    const getTooltipName = (name, visibleName) => {
        if (visibleName.endsWith("...")  || !props.open) {
            return <h2>{name}</h2>;
        } else {
            return "";
        }
    }
    return (
        <List>
            {filteredBuckets
            .sort((a, b) => {
                return a.name > b.name ? 1 : -1
            })
            .map((bucket) => (
                <div key={bucket.name}>
                    <Tooltip placement="right"
                             title={getTooltipName(bucket.name,
                                 getBucketVisibleName(bucket.name))}>
                        <ListItemButton
                            selected={activeBucket != null ? bucket.id
                                === activeBucket.id : false}
                            key={bucket.name}
                            onClick={() => onClick(bucket)}
                        >
                            <ListItemIcon>
                                <StyledIcon
                                    iconName={bucket.iconName}
                                    iconColor={bucket.iconColor}
                                    iconSvg={bucket.iconSvg}
                                />
                            </ListItemIcon>
                            <ListItemText
                                primary={getBucketVisibleName(bucket.name)}/>
                        </ListItemButton>
                    </Tooltip>
                </div>
            ))}
        </List>
    );
}
