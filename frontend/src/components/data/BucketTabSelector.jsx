import React, {useContext} from 'react';
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
    const {bucketsTabs, activeBucket, setActiveBucket, removeTab} = accessContext;
    let removing = false; // indicate whether changing tab is invoked by selection or by removing

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

    return (
        <StyledTabs
            value={bucketsTabs.indexOf(activeBucket)}
            textColor="inherit"
            indicatorColor="secondary"
            variant="scrollable"
            scrollButtons
            allowScrollButtonsMobile>
            {bucketsTabs.map((bucket) => (
                <CustomTab key={bucket.id}
                           component="div"
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
