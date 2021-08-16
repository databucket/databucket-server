import React, {useContext, useEffect, useState} from 'react';
import Tab from "@material-ui/core/Tab";
import IconButton from "@material-ui/core/IconButton";
import DynamicIcon from "../utils/DynamicIcon";
import CloseIcon from "@material-ui/icons/Close";
import Tabs from "@material-ui/core/Tabs";
import {lighten, makeStyles, withStyles} from "@material-ui/core/styles";
import {getAppBarBackgroundColor} from "../../utils/Themes";
import AccessContext from "../../context/access/AccessContext";

const useStyles = makeStyles((theme) => ({
    tabs: {
        flex: 1,
    },
    button: {
        "&:disabled": {
            color: theme.palette.primary.contrastText
        }
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
        setTimeout(() => setDelay(false), 500)
    }, []);

    const getBucketName = (name) => {
        return name.length > 17 ? name.substring(0, 15) + "..." : name;
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
            scrollButtons="on"
            className={classes.tabs}
        >
            {bucketsTabs.map((bucket) => (
                <StyledTab key={bucket.id} component="div" onClick={() => handleChangedTab(bucket)} label={
                    <span>
                        <IconButton disabled className={classes.button}>
                            <DynamicIcon iconName={bucket.iconName}/>
                        </IconButton>

                        {getBucketName(bucket.name)}

                        <IconButton color={'inherit'} onClick={() => handleRemovedTab(bucket)}>
                            <CloseIcon style={{fontSize: 18}}/>
                        </IconButton>
                    </span>
                }
                />
            ))}
        </Tabs>
    );

    return !delay && tabs;
}