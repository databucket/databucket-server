import React from 'react';
import PropTypes from 'prop-types';
import Breadcrumbs from '@material-ui/core/Breadcrumbs';
import { withStyles } from '@material-ui/core';
import NavigateNextIcon from '@material-ui/icons/NavigateNext';
import Button from '@material-ui/core/Button';
import Menu from '@material-ui/core/Menu';
import MenuItem from '@material-ui/core/MenuItem';
import ExpandMoreIcon from '@material-ui/icons/ExpandMore'
import DynamicIcon from './utils/DynamicIcon';

const styles = theme => ({
    root: {
        padding: theme.spacing(1, 2),
    },
    button: {
        margin: theme.spacing(0),
        textTransform: 'none',
        fontSize: 18
    },
    leftIcon: {
        marginRight: theme.spacing(1),
    },
    rightIcon: {
        marginLeft: theme.spacing(1),
    }
});

class SelectorView extends React.Component {

    constructor(props) {
        super(props);
        this.state = {
            selectedBucket: null,
            bucketViews: null,
            selectedView: null,
            anchorElView: null
        };
    }

    static getDerivedStateFromProps(props, state) {
        if (props.selectedBucket != null && props.selectedView != null && props.selectedView.columns != null && props.bucketViews != null) {

            let preparedBucketViews = props.bucketViews;
            if (preparedBucketViews.length > 1 && props.selectedView.view_id > 0) {
                for( var i = 0; i < preparedBucketViews.length; i++){ 
                    if ( preparedBucketViews[i].view_id === 0) {
                        preparedBucketViews.splice(i, 1); 
                    }
                 }
            } 
            
            return {
                selectedBucket: props.selectedBucket,
                bucketViews: preparedBucketViews,
                selectedView: props.selectedView
            };
        } else {
            return {
                selectedBucket: null,
                bucketViews: null,
                selectedView: null,
                anchorElView: null,
            };
        }
    }    

    handleViewClick(event) {
        this.setState({ anchorElView: event.currentTarget });
    }

    handleSelectedView(selectedView) {
        if (selectedView !== this.state.selectedView)
            this.props.onViewSelected(selectedView);

        this.handleViewsClose();
    }

    handleViewsClose() {
        this.setState({ anchorElView: null });
    }

    render() {
        const { classes } = this.props;
        if (this.state.selectedBucket != null && this.state.selectedView != null) {
            return (
                <div elevation={0} className={classes.root}>
                    <Breadcrumbs color='inherit' separator={<NavigateNextIcon fontSize="default" />} aria-label="Breadcrumb">
                        <Button size="small" color='inherit' aria-haspopup="true" className={classes.button}>
                            <DynamicIcon iconName={this.state.selectedBucket.icon_name} />
                            <div className={classes.leftIcon}></div>
                            {this.state.selectedBucket.bucket_name}
                        </Button>
                        <Button size="small" color='inherit' aria-controls="views-menu" aria-haspopup="true" onClick={(event) => this.handleViewClick(event)} className={classes.button}>
                            {this.state.selectedView !== null ? this.state.selectedView.view_name : null}
                            <ExpandMoreIcon className={classes.rightIcon} />
                        </Button>
                    </Breadcrumbs>
                    <Menu
                        id="views-menu"
                        anchorEl={this.state.anchorElView}
                        keepMounted
                        open={Boolean(this.state.anchorElView)}
                        onClose={(event) => this.handleViewsClose(event)}
                    >
                        {this.state.bucketViews.map((view) => (
                            <MenuItem
                                selected={view.view_id === this.state.selectedView.view_id}
                                onClick={() => this.handleSelectedView(view)} key={view.view_id}>{view.view_name}
                            </MenuItem>
                        ))}
                    </Menu>
                </div>
            )
        }
        else return (<div />);
    }
}

SelectorView.propTypes = {
    classes: PropTypes.object.isRequired,
};

export default withStyles(styles)(SelectorView);