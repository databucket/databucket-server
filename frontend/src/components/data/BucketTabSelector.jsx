import React, {useContext} from 'react';
import {styled} from '@mui/material/styles';
import IconButton from "@mui/material/IconButton";
import CloseIcon from "@mui/icons-material/Close";
import Tabs from "@mui/material/Tabs";
import AccessContext from "../../context/access/AccessContext";
import {Tooltip} from "@mui/material";
import {getIconColor} from "../../utils/MaterialTableHelper";
import StyledIcon from "../utils/StyledIcon";
import {CustomTab} from "../common/CustomAppBar";

const StyledTabs = styled(Tabs)(({theme}) => ({
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

    // const tabs = (
    return (
        <StyledTabs
            value={bucketsTabs.indexOf(activeBucket)}
            textColor="inherit"
            variant="scrollable"
            scrollButtons
            allowScrollButtonsMobile>
            {bucketsTabs.map((bucket) => (
                <CustomTab key={bucket.id}
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
}
