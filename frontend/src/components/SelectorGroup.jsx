import React from 'react';
import PropTypes from 'prop-types';
import { withStyles } from '@material-ui/core';
import Button from '@material-ui/core/Button';
import Menu from '@material-ui/core/Menu';
import MenuItem from '@material-ui/core/MenuItem';
import ExpandMoreIcon from '@material-ui/icons/ExpandMore'

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

class SelectorGroup extends React.Component {

    constructor(props) {
        super(props);
        this.state = {
            selectedGroup: null,
            allGroups: null,
            anchorElGroup: null
        };
    }

    static getDerivedStateFromProps(props, state) {
        return {
            selectedGroup: props.selectedGroup !== null ? props.selectedGroup : -1,
            allGroups: props.allGroups
        };
    }

    handleGroupClick(event) {
        this.setState({ anchorElGroup: event.currentTarget });
    }

    handleSelectedGroup(selectedGroup) {
        if (selectedGroup !== this.state.selectedGroup)
            this.props.onGroupSelected(selectedGroup);

        this.handleGroupsClose();
    }

    handleGroupsClose() {
        this.setState({ anchorElGroup: null });
    }

    render() {
        const { classes } = this.props;
        if (this.state.allGroups != null && this.state.allGroups.length > 0) {
            return (
                <div elevation={0} className={classes.root}>
                    <Button size="small" color='inherit' aria-controls="groups-menu" aria-haspopup="true" onClick={(event) => this.handleGroupClick(event)} className={classes.button}>
                        {this.state.selectedGroup.group_name}
                        <ExpandMoreIcon className={classes.rightIcon} />
                    </Button>
                    <Menu
                        id="groups-menu"
                        anchorEl={this.state.anchorElGroup}
                        keepMounted
                        open={Boolean(this.state.anchorElGroup)}
                        onClose={(event) => this.handleGroupsClose(event)}
                    >
                        {this.state.allGroups.map((group) => (
                            <MenuItem
                                selected={group.group_id === this.state.selectedGroup.group_id}
                                onClick={() => this.handleSelectedGroup(group)} key={group.group_id}>{group.group_name}
                            </MenuItem>
                        ))}
                    </Menu>
                </div>
            )
        } else return (<div />);
    }
}

SelectorGroup.propTypes = {
    classes: PropTypes.object.isRequired,
};

export default withStyles(styles)(SelectorGroup);