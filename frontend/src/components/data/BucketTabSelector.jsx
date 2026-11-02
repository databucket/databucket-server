import React, {useContext, useState} from 'react';
import {IconButton, styled, Tabs, Tooltip} from "@mui/material";
import CloseIcon from "@mui/icons-material/Close";
import AccessContext from "../../context/access/AccessContext";
import {getIconColor} from "../../utils/MaterialTableHelper";
import StyledIcon from "../utils/StyledIcon";
import {CustomTab} from "../common/CustomAppBar";

const StyledTabs = styled(Tabs)(() => ({
    flex: 1,
    textTransform: "initial",
}));

export default function BucketTabSelector() {


    const accessContext = useContext(AccessContext);
    const {bucketsTabs, activeBucket, setActiveBucket, removeTab, moveTab} = accessContext;
    let removing = false; // indicate whether changing tab is invoked by selection or by removing
    // a tab dragged to another place: its index and the place it would go to (0..length, before that tab)
    const [drag, setDrag] = useState(null);

    const getBucketVisibleName = (name) => {
        return name.length > 17 ? name.substring(0, 15) + "..." : name;
    }

    const getTooltipName = (name, visibleName) => {
        if (visibleName.endsWith("..."))
            return <h2>{name}</h2>;
        else
            return "";
    }

    const handleChangedTab = (event, bucket) => {
        event.stopPropagation();
        if (!removing) {
            if (bucket !== activeBucket) {
                setActiveBucket(bucket);
            }
        } else {
            removing = false;
        }
    };

    const handleRemovedTab = (event, bucket) => {
        event.stopPropagation();
        removing = true;
        removeTab(bucket);
    }

    const handleDragStart = (event, index) => {
        event.dataTransfer.effectAllowed = 'move';
        event.dataTransfer.setData('text/plain', bucketsTabs[index].name); // Firefox drags only with some data
        setDrag({from: index, to: null});
    };

    const handleDragOver = (event, index) => {
        if (drag == null)
            return;
        event.preventDefault();
        event.dataTransfer.dropEffect = 'move';
        const rect = event.currentTarget.getBoundingClientRect();
        const to = event.clientX < rect.left + rect.width / 2 ? index : index + 1;
        if (to !== drag.to)
            setDrag({...drag, to});
    };

    const handleDrop = (event) => {
        event.preventDefault();
        if (drag != null && drag.to != null)
            moveTab(drag.from, drag.to);
        setDrag(null);
    };

    // a line on the side of the tab where the dragged tab would go (not where it already is)
    const dropMarker = (index) => {
        if (drag == null || drag.to == null || drag.to === drag.from || drag.to === drag.from + 1)
            return {};
        if (drag.to === index)
            return {boxShadow: 'inset 3px 0 0 currentColor'};
        if (drag.to === index + 1 && index === bucketsTabs.length - 1)
            return {boxShadow: 'inset -3px 0 0 currentColor'};
        return {};
    };

    return (
        <StyledTabs
            value={bucketsTabs.indexOf(activeBucket)}
            textColor="inherit"
            indicatorColor="secondary"
            variant="scrollable"
            scrollButtons
            allowScrollButtonsMobile>
            {bucketsTabs.map((bucket, index) => (
                <CustomTab key={bucket.id}
                           component="div"
                           draggable={bucketsTabs.length > 1}
                           onDragStart={(event) => handleDragStart(event, index)}
                           onDragOver={(event) => handleDragOver(event, index)}
                           onDrop={handleDrop}
                           onDragEnd={() => setDrag(null)}
                           sx={{opacity: drag?.from === index ? 0.5 : undefined, ...dropMarker(index)}}
                           onClick={(event) => handleChangedTab(event, bucket)}
                           iconPosition="start"
                           icon={<StyledIcon iconName={bucket.iconName}
                                             iconColor={getIconColor('banner', bucket.iconColor)}
                                             iconSvg={bucket.iconSvg}
                                             sx={{marginRight: 1}}
                           />}
                           label={
                               <Tooltip title={getTooltipName(bucket.name, getBucketVisibleName(bucket.name))}>
                                   <span>
                                       {getBucketVisibleName(bucket.name)}
                                       <IconButton color="inherit"
                                                   onClick={(event) => handleRemovedTab(event, bucket)} size="large">
                                           <CloseIcon sx={{fontSize: 18}}/>
                                       </IconButton>
                                   </span>
                               </Tooltip>
                           }
                />
            ))}
        </StyledTabs>
    );
}
