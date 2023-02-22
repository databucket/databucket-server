import React, {useContext, useEffect, useState} from 'react';
import Tab from "@mui/material/Tab";
import IconButton from "@mui/material/IconButton";
import CloseIcon from "@mui/icons-material/Close";
import Tabs from "@mui/material/Tabs";
import { lighten } from "@mui/material/styles";
import makeStyles from '@mui/styles/makeStyles';
import withStyles from '@mui/styles/withStyles';
import {getAppBarBackgroundColor} from "../../utils/Themes";
import AccessContext from "../../context/access/AccessContext";
import {Tooltip} from "@mui/material";
import StyledIconButtonTab from "../utils/StyledIconButtonTab";
import {getIconColor} from "../../utils/MaterialTableHelper";

const useStyles = makeStyles((theme) => ({
    tabs: {
        flex: 1,
    }
}));

const styles = theme => ({
    root: {
        "&:hover": {
            backgroundColor: lighten(getAppBarBackgroundColor(), 0.05),
            opacity: 1
        },
        textTransform: "initial"
    },
    selected: {}
});

const StyledTab = withStyles(styles)(Tab)

export default function BucketTabSelector() {

    const classes = useStyles();
    const accessContext = useContext(AccessContext);
    const {bucketsTabs, activeBucket, setActiveBucket, removeTab} = accessContext;
    let removing = false; // indicate whether changing tab is invoked by selection or by removing

    // This timeout allows to load Material Icons before first rendering
    const [ delay, setDelay ] = useState(true);
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
        <Tabs
            value={bucketsTabs.indexOf(activeBucket)}
            variant="scrollable"
            scrollButtons
            className={classes.tabs}
            allowScrollButtonsMobile>
            {bucketsTabs.map((bucket) => (
                <StyledTab key={bucket.id} component="div" onClick={() => handleChangedTab(bucket)} label={
                    <Tooltip title={getTooltipName(bucket.name, getBucketVisibleName(bucket.name))}>
                    <span>
                        <StyledIconButtonTab iconName={bucket.iconName} iconColor={getIconColor('banner', bucket.iconColor)} iconSvg={bucket.iconSvg} />
                        {getBucketVisibleName(bucket.name)}
                        <IconButton color={'inherit'} onClick={() => handleRemovedTab(bucket)} size="large">
                            <CloseIcon style={{fontSize: 18}}/>
                        </IconButton>
                    </span>
                    </Tooltip>
                }
                />
            ))}
        </Tabs>
    );

    return !delay && tabs;
}