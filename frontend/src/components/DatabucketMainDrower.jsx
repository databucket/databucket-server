import React from 'react';
import clsx from 'clsx';
import PropTypes from 'prop-types';
import Drawer from '@material-ui/core/Drawer';
import AppBar from '@material-ui/core/AppBar';
import Toolbar from '@material-ui/core/Toolbar';
import CssBaseline from '@material-ui/core/CssBaseline';
import Divider from '@material-ui/core/Divider';
import IconButton from '@material-ui/core/IconButton';
import MenuIcon from '@material-ui/icons/Menu';
import ChevronLeftIcon from '@material-ui/icons/ChevronLeft';
import DatabucketTable from './DatabucketTable';
import { withStyles } from '@material-ui/core';
import SettingsDialog from './dialogs/Settings/SettingsDialog';
import SelectorGroup from './SelectorGroup';
import SelectorBucket from './SelectorBucket';
import SelectorView from './SelectorView';
import DynamicIcon from './DynamicIcon';
import LookupIconDialog from './dialogs/EditLookupIconDialog';
import Cookies from 'universal-cookie';

const drawerWidth = 250;

const cookies = new Cookies();

const styles = theme => ({
    root: {
        display: 'flex',
    },
    grow: {
        flexGrow: 1,
    },
    appBar: {
        zIndex: theme.zIndex.drawer + 1,
        transition: theme.transitions.create(['width', 'margin'], {
            easing: theme.transitions.easing.sharp,
            duration: theme.transitions.duration.leavingScreen,
        }),
    },
    appBarShift: {
        marginLeft: drawerWidth,
        width: `calc(100% - ${drawerWidth}px)`,
        transition: theme.transitions.create(['width', 'margin'], {
            easing: theme.transitions.easing.sharp,
            duration: theme.transitions.duration.enteringScreen,
        }),
    },
    menuButton: {
        marginRight: theme.spacing(2),
    },
    hide: {
        display: 'none',
    },
    drawer: {
        width: drawerWidth,
        flexShrink: 0,
        whiteSpace: 'nowrap',
    },
    drawerOpen: {
        width: drawerWidth,
        transition: theme.transitions.create('width', {
            easing: theme.transitions.easing.sharp,
            duration: theme.transitions.duration.enteringScreen,
        }),
    },
    drawerClose: {
        transition: theme.transitions.create('width', {
            easing: theme.transitions.easing.sharp,
            duration: theme.transitions.duration.leavingScreen,
        }),
        overflowX: 'hidden',
        width: theme.spacing(7) + 1,
        [theme.breakpoints.up('sm')]: {
            width: theme.spacing(9) + 1,
        },
    },
    toolbar: {
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'flex-end',
        padding: '0 8px',
        ...theme.mixins.toolbar,
    },
    content: {
        flexGrow: 1,
        padding: theme.spacing(0),
    },
    contentShift: {
        transition: theme.transitions.create('margin', {
            easing: theme.transitions.easing.easeOut,
            duration: theme.transitions.duration.enteringScreen,
        }),
        marginLeft: 0,
    },
    title: {
        display: 'none',
        [theme.breakpoints.up('sm')]: {
            display: 'block',
        },
    }
});


class DatabucketMainDrawer extends React.Component {

    constructor(props) {
        super(props);

        this.state = {
            open: false,
            allGroups: null,
            selectedGroup: null,
            allClasses: null,
            allBuckets: null,
            allViews: null,
            allTags: null,
            allTasks: null,
            allEvents: null,
            allFilters: null,
            allColumns: null,
            tableInputObject: {
                bucket: null,
                view: null,
                bucketViews: null, // used for view selector
                filters: null
            }
        };

        this.setPageTitle();
        this.reloadDictionaries();
    }

    handleDrawerOpen() {
        this.setState({ open: true });
    }

    handleDrawerClose() {
        this.setState({ open: false });
    }

    createTableInputObject(bucket, view) {
        let tagsLookup = {};
        let filters = null;
        let bucketViews = null;

        if (this.state.allTags !== null)
            for (var j = 0; j < this.state.allTags.length; j++) {
                const tag = this.state.allTags[j];
                if ((tag.bucket_id === bucket.bucket_id || tag.bucket_id === null) && (tag.class_id === bucket.class_id || tag.class_id === null))
                    tagsLookup[tag.tag_id] = tag.tag_name;
            }

        if (this.state.allViews !== null)
            bucketViews = this.state.allViews.filter(d => ((d.bucket_id === null || d.bucket_id === bucket.bucket_id)) && (d.class_id === null || d.class_id === bucket.class_id));

        if (this.state.allFilters !== null)
            filters = this.state.allFilters.filter(d => ((d.bucket_id === null || d.bucket_id === bucket.bucket_id)) && (d.class_id === null || d.class_id === bucket.class_id));

        let preparedView = JSON.parse(JSON.stringify(view));
        let columnsArray = this.state.allColumns.filter(d => (d.columns_id === preparedView.columns_id));
        let preparedColumns = JSON.parse(JSON.stringify(columnsArray[0].columns));
        preparedView.columns = this.convertColumns(preparedColumns, tagsLookup);

        return {
            bucket: bucket,
            view: preparedView,
            bucketViews: bucketViews,
            filters: filters
        };
    }

    /*
        Invoked when user changes an item in the groups list.
        @newSelectedGroup - json object
    */
    onGroupSelected(newSelectedGroup) {
        const lastSelectedBucket = this.getLastSelectedBucket(this.state.allBuckets, newSelectedGroup);
        if (lastSelectedBucket != null) {
            const lastSelectedView = this.getLastSelectedView(lastSelectedBucket);
            let newTableInputObject = this.createTableInputObject(lastSelectedBucket, lastSelectedView);
            this.setState({ selectedGroup: newSelectedGroup, tableInputObject: newTableInputObject });
        } else {
            let newTableInputObject = {
                bucket: null,
                view: null,
                bucketViews: null,
                filters: null
            };
            this.setState({ selectedGroup: newSelectedGroup, tableInputObject: newTableInputObject });
        }
        this.setLastSelectedGroup(newSelectedGroup);
    }

    /*
        Invoked when user changes an item in the buckets list.
        @newSelectedBucket - json object
    */
    onBucketSelected(newSelectedBucket) {
        let lastSelectedView = this.getLastSelectedView(newSelectedBucket);
        let newTableInputObject = this.createTableInputObject(newSelectedBucket, lastSelectedView);
        this.setState({ tableInputObject: newTableInputObject });
        this.setLastSelectedBucket(newSelectedBucket, this.state.selectedGroup);
    }

    /*
        Invoked when user changes an item in the views list.
        @newSelectedView - json object
    */
    onViewSelected(newSelectedView) {
        let newTableInputObject = this.createTableInputObject(this.state.tableInputObject.bucket, newSelectedView);
        this.setState({ tableInputObject: newTableInputObject });
        this.setLastSelectedView(this.state.tableInputObject.bucket, newSelectedView);
    }

    setLastSelectedBucket(bucket, selectedGroup) {
        let selectedGroupId = 0;
        if (selectedGroup != null)
            selectedGroupId = selectedGroup.group_id;

        const current = new Date();
        const nextYear = new Date();
        nextYear.setFullYear(current.getFullYear() + 1);
        cookies.set('last_bucket_id_g_' + selectedGroupId, bucket.bucket_id, { path: window.location.href, expires: nextYear });
    }

    getLastSelectedBucket(buckets, selectedGroup) {
        let selectedGroupId = 0;
        if (selectedGroup != null)
            selectedGroupId = selectedGroup.group_id;

        if (buckets != null && buckets.length > 0 && selectedGroup != null && selectedGroup.buckets != null && selectedGroup.buckets.length > 0) {
            const lastBucketId = cookies.get('last_bucket_id_g_' + selectedGroupId);
            let groupBuckets = buckets.filter(b => (selectedGroup.buckets.includes(b.bucket_id)));
            if (lastBucketId != null) {
                var lastSelectedBuckets = buckets.filter(d => (d.bucket_id === parseInt(lastBucketId) && selectedGroup.buckets.includes(d.bucket_id)));
                if (lastSelectedBuckets.length > 0) {
                    return lastSelectedBuckets[0];
                } else {
                    return groupBuckets[0];
                }
            } else if (groupBuckets.length > 0) {
                return groupBuckets[0];
            } else {
                return null;
            }
        } else if (buckets != null && buckets.length > 0 && selectedGroup == null) {
            return buckets[0];
        } else {
            return null;
        }
    }

    setLastSelectedView(bucket, view) {
        const current = new Date();
        const nextYear = new Date();
        nextYear.setFullYear(current.getFullYear() + 1);
        cookies.set("bucket-" + bucket.bucket_id + '-last_view', view.view_id, { path: window.location.href, expires: nextYear });
    }

    getLastSelectedView(bucket) {
        let bucketViews = null;
        if (this.state.allViews !== null)
            bucketViews = this.state.allViews.filter(d => ((d.bucket_id === null || d.bucket_id === bucket.bucket_id)) && (d.class_id === null || d.class_id === bucket.class_id));

        let result = null;

        const lastViewId = cookies.get("bucket-" + bucket.bucket_id + '-last_view');
        if (lastViewId != null) {
            const filteredViews = bucketViews.filter(d => (d.view_id === parseInt(lastViewId)));
            if (filteredViews.length > 0)
                result = filteredViews[0];
            else
                result = bucketViews[0];
        } else
            result = bucketViews[0];


        return result;
    }

    setLastSelectedGroup(group) {
        const current = new Date();
        const nextYear = new Date();
        nextYear.setFullYear(current.getFullYear() + 1);
        cookies.set("last_group_id", group.group_id, { path: window.location.href, expires: nextYear });
    }

    getLastSelectedGroup(groups) {
        if (groups != null && groups.length > 0) {
            const lastGroupId = cookies.get('last_group_id');
            if (lastGroupId != null) {
                const filteredGroups = groups.filter(g => (g.group_id === parseInt(lastGroupId)));
                return filteredGroups.length > 0 ? filteredGroups[0] : groups[0];
            } else
                return groups[0];
        } else
            return null;
    }

    onDictonariesLoaded(allInputGroups, allInputClasses, allInputBuckets, allInputViews, 
        allInputFilters, allInputTags, allInputColumns, allInputTasks, allInputEvents) {

        let changed = false;
        let nextState = this.state;

        if (allInputGroups != null) {
            if (JSON.stringify(this.state.allGroups) !== JSON.stringify(allInputGroups)) {
                nextState.allGroups = allInputGroups;
                changed = true;
            }
        }

        if (allInputClasses != null) {
            if (JSON.stringify(this.state.allClasses) !== JSON.stringify(allInputClasses)) {
                nextState.allClasses = allInputClasses;
                changed = true;
            }
        }

        if (allInputBuckets != null) {
            if (JSON.stringify(this.state.allBuckets) !== JSON.stringify(allInputBuckets)) {
                nextState.allBuckets = allInputBuckets;
                changed = true;
            }
        }

        if (allInputViews != null) {
            if (JSON.stringify(this.state.allViews) !== JSON.stringify(allInputViews)) {
                nextState.allViews = allInputViews;
                changed = true;
            }
        }

        if (allInputTags != null) {
            if (JSON.stringify(this.state.allTags) !== JSON.stringify(allInputTags)) {
                nextState.allTags = allInputTags;
                changed = true;
            }
        }

        if (allInputFilters != null) {
            if (JSON.stringify(this.state.allFilters) !== JSON.stringify(allInputFilters)) {
                nextState.allFilters = allInputFilters;
                changed = true;
            }
        }

        if (allInputColumns != null) {
            if (JSON.stringify(this.state.allColumns) !== JSON.stringify(allInputColumns)) {
                nextState.allColumns = allInputColumns;
                changed = true;
            }
        }

        if (allInputTasks != null) {
            if (JSON.stringify(this.state.allTasks) !== JSON.stringify(allInputTasks)) {
                nextState.allTasks = allInputTasks;
                changed = true;
            }
        }

        if (allInputEvents != null) {
            if (JSON.stringify(this.state.allEvents) !== JSON.stringify(allInputEvents)) {
                nextState.allEvents = allInputEvents;
                changed = true;
            }
        }

        if (changed && allInputBuckets != null) {
            nextState.selectedGroup = this.getLastSelectedGroup(nextState.allGroups);
            const lastSelectedBucket = this.getLastSelectedBucket(nextState.allBuckets, nextState.selectedGroup);

            if (lastSelectedBucket != null) {
                const lastSelectedView = this.getLastSelectedView(lastSelectedBucket);
                nextState.tableInputObject = this.createTableInputObject(lastSelectedBucket, lastSelectedView);
            } else {
                nextState.tableInputObject = {
                    bucket: null,
                    view: null,
                    bucketViews: null,
                    filters: null
                };
            }
            this.setState(nextState);
        }
    }

    reloadDictionaries() {
        this.getAllDictionaries().then(([groups, classes, buckets, views, filters, tags, columns, tasks, events]) => {
            this.onDictonariesLoaded(groups, classes, buckets, views, filters, tags, columns, tasks, events);
        });
    }

    fetchViews() {
        return fetch(window.API + '/view')
            .then(response => response.json())
            .then(responseJson => responseJson.views);
    }

    fetchGroups() {
        return fetch(window.API + '/group')
            .then(response => response.json())
            .then(responseJson => responseJson.groups);
    }

    fetchClasses() {
        return fetch(window.API + '/class')
            .then(response => response.json())
            .then(responseJson => responseJson.classes);
    }

    fetchBuckets() {
        return fetch(window.API + '/bucket?sort=index')
            .then(response => response.json())
            .then(responseJson => responseJson.buckets);
    }

    fetchFilters() {
        return fetch(window.API + '/filter')
            .then(response => response.json())
            .then(responseJson => responseJson.filters);
    }

    fetchColumns() {
        return fetch(window.API + '/columns')
            .then(response => response.json())
            .then(responseJson => responseJson.columns);
    }

    fetchTags() {
        return fetch(window.API + '/tag')
            .then(response => response.json())
            .then(responseJson => responseJson.tags);
    }

    fetchTasks() {
        return fetch(window.API + '/task')
            .then(response => response.json())
            .then(responseJson => responseJson.tasks);
    }

    fetchEvents() {
        return fetch(window.API + '/event')
            .then(response => response.json())
            .then(responseJson => responseJson.events);
    }

    setPageTitle() {
        fetch(window.API + '/title')
            .then(response => response.json())
            .then(responseJson => responseJson.title)
            .then(title => {
                document.title = title;
            })
    }

    getAllDictionaries() {
        return Promise.all([
            this.fetchGroups(), 
            this.fetchClasses(), 
            this.fetchBuckets(), 
            this.fetchViews(), 
            this.fetchFilters(), 
            this.fetchTags(), 
            this.fetchColumns(), 
            this.fetchTasks(),
            this.fetchEvents()
        ]);
    }

    onSettingsClose() {
        this.reloadDictionaries();
    }

    getFilteredBuckets(allBuckets, selectedGroup) {
        if (selectedGroup != null) {
            if (selectedGroup.buckets != null && selectedGroup.buckets.length > 0) {
                let result = allBuckets.filter(b => (selectedGroup.buckets.includes(b.bucket_id)));
                return result;
            } else
                return null;
        } else
            return allBuckets;
    }


    strToDateTime(dateTimeString) {
        if (dateTimeString != null)
            return new Date(dateTimeString).toLocaleString();
        else
            return null;

    }

    strToDate(dateTimeString) {
        if (dateTimeString != null)
            return new Date(dateTimeString).toLocaleDateString();
        else
            return null;
    }

    strToTime(dateTimeString) {
        if (dateTimeString != null)
            return new Date(dateTimeString).toLocaleTimeString();
        else
            return null;
    }

    createColumnLookup(items) {
        let lookup = {};
        if (items != null) {
            for (let i = 0; i < items.length; i++) {
                lookup[items[i].key] = items[i].text_value;
            }
        }
        return lookup;
    }

    //columns for table
    convertColumns(columns, tagsLookup) {
        if (columns !== null) {
            for (var i = 0; i < columns.length; i++) {
                let column = columns[i];
                
                column.emptyValue = '';

                if (column.type === 'datetime') {
                    column.render = rowData => <div>{rowData != null ? this.strToDateTime(rowData[column.title]) : null}</div>;
                } else if (column.type === 'date') {
                    column.render = rowData => <div>{rowData != null ? this.strToDate(rowData[column.title]) : null}</div>;
                } else if (column.type === 'time') {
                    column.render = rowData => <div>{rowData != null ? this.strToTime(rowData[column.title]) : null}</div>;
                }

                if (column.field === 'tag_id') {
                    column.lookup = tagsLookup;
                } else if (column.def_values != null && column.def_values.items != null && column.def_values.items.length > 0) {
                    if (column.def_values.text_values) {
                        column.lookup = this.createColumnLookup(column.def_values.items);
                    } else {
                        column.render = rowData => <DynamicIcon iconName={this.getIconName(column.def_values.items, rowData[column.field])} color='action' />;
                        column.editComponent = props => <LookupIconDialog
                            selectedIconName={this.getIconName(column.def_values.items, props.rowData[column.field])}
                            items={column.def_values.items}
                            onChange={props.onChange} />
                    }
                }

                column.source = column.field;
                column.field = column.title;
            }

            // console.log(columns);
            return columns;
        } else return [];
    }

    getIconName(items, value) {
        var filteredItems = items.filter(item => (item.key === value));
        if (filteredItems.length > 0)
            return filteredItems[0].icon_name;
        else
            return null;
    }

    render() {
        const { classes } = this.props;
        return (
            <div className={classes.root}>
                <CssBaseline />
                <div>
                    <div className={classes.grow}>
                        <AppBar
                            position="fixed"
                            className={clsx(classes.appBar, { [classes.appBarShift]: this.state.open })}
                        >
                            <Toolbar>
                                <IconButton
                                    edge="start"
                                    color="inherit"
                                    aria-label="Open drawer"
                                    onClick={() => this.handleDrawerOpen()}
                                    className={clsx(classes.menuButton, this.state.open && classes.hide)}
                                >
                                    <MenuIcon />
                                </IconButton>
                                <SelectorGroup
                                    selectedGroup={this.state.selectedGroup}
                                    allGroups={this.state.allGroups}
                                    onGroupSelected={(newGroupSelected) => this.onGroupSelected(newGroupSelected)}
                                />
                                <SelectorView
                                    selectedBucket={this.state.tableInputObject.bucket}
                                    selectedView={this.state.tableInputObject.view}
                                    bucketViews={this.state.tableInputObject.bucketViews}
                                    onViewSelected={(newSelectedView) => this.onViewSelected(newSelectedView)}
                                />

                                <div className={classes.grow} />
                                <SettingsDialog
                                    onClose={(reload) => this.onSettingsClose(reload)}
                                    groups={this.state.allGroups}
                                    classes={this.state.allClasses}
                                    buckets={this.state.allBuckets}
                                    columns={this.state.allColumns}
                                    filters={this.state.allFilters}
                                    tags={this.state.allTags}
                                    tasks={this.state.allTasks}
                                    events={this.state.allEvents}
                                />
                            </Toolbar>
                        </AppBar>
                    </div>
                </div>
                <Drawer
                    variant="permanent"
                    className={clsx(classes.drawer, {
                        [classes.drawerOpen]: this.state.open,
                        [classes.drawerClose]: !this.state.open,
                    })}
                    classes={{
                        paper: clsx({
                            [classes.drawerOpen]: this.state.open,
                            [classes.drawerClose]: !this.state.open,
                        }),
                    }}
                    open={this.state.open}
                >
                    <div className={classes.toolbar}>
                        <IconButton onClick={() => this.handleDrawerClose()} >
                            <ChevronLeftIcon />
                        </IconButton>
                    </div>
                    <Divider />
                    <SelectorBucket
                        buckets={this.getFilteredBuckets(this.state.allBuckets, this.state.selectedGroup)}
                        selectedBucket={this.state.tableInputObject.bucket}
                        onBucketSelected={(selectedBucket) => this.onBucketSelected(selectedBucket)}
                    />
                </Drawer>
                <main className={classes.content}>
                    <div className={classes.toolbar} />
                    <DatabucketTable
                        selected={this.state.tableInputObject}
                        allTags={this.state.allTags}
                        allTasks={this.state.allTasks}
                    />
                </main>
            </div>
        );
    }

}

DatabucketMainDrawer.propTypes = {
    classes: PropTypes.object.isRequired,
};

export default withStyles(styles)(DatabucketMainDrawer);

