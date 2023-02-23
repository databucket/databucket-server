import React, {useContext, useEffect, useState} from 'react';
import {lighten, styled} from '@mui/material/styles';
import Tab from "@mui/material/Tab";
import IconButton from "@mui/material/IconButton";
import CloseIcon from "@mui/icons-material/Close";
import Tabs from "@mui/material/Tabs";
import {getAppBarBackgroundColor} from "../../utils/Themes";
import AccessContext from "../../context/access/AccessContext";
import {Tooltip} from "@mui/material";
import {getIconColor} from "../../utils/MaterialTableHelper";
import StyledIcon from "../utils/StyledIcon";

const StyledTabs = styled(Tabs)(({theme}) => ({
    flex: 1,
    textTransform: "initial",
}));

const StyledTab = styled(Tab)(({theme}) => ({
    backgroundColor: lighten(getAppBarBackgroundColor(), 0.05),

    '&.Mui-selected': {
        color: '#fff',
    },
    '&.Mui-focusVisible': {
        backgroundColor: 'rgba(100, 95, 228, 0.32)',
    },
}));

export default function BucketTabSelector() {


    const accessContext = useContext(AccessContext);
    const {bucketsTabs, activeBucket, setActiveBucket, removeTab} = accessContext;
    let removing = false; // indicate whether changing tab is invoked by selection or by removing

    // This timeout allows to load Material Icons before first rendering
    const [delay, setDelay] = useState(true);
    useEffect(() => {
        setTimeout(() => setDelay(false), 700)
    }, []);

    const getBucketVisibleName = (name) => {
        return name.length > 17 ? name.substring(0, 15) + "..." : name;
    }

    const getTooltipName = (name, visibleName) => {
        if (visibleName.endsWith("..."))
            return <h2>{name}</h2>;
        else
            return "";
    }

    const handleChangedTab = (bucket) => {
        if (!removing) {
            if (bucket !== activeBucket) {
                setActiveBucket(bucket);
            }
        } else {
            removing = false;
        }
    };

    const handleRemovedTab = (bucket) => {
        removing = true;
        removeTab(bucket);
    }

    const tabs = (
        <StyledTabs
            value={bucketsTabs.indexOf(activeBucket)}
            variant="scrollable"
            scrollButtons
            allowScrollButtonsMobile>
            {bucketsTabs.map((bucket) => (
                <StyledTab key={bucket.id}
                           component="div"
                           onClick={() => handleChangedTab(bucket)}
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
                                    onClick={() => handleRemovedTab(bucket)} size="large">
                            <CloseIcon sx={{fontSize: 18}}/>
                        </IconButton>
                    </span>
                               </Tooltip>
                           }
                />
            ))}
        </StyledTabs>
    );

    return !delay && tabs;
}
