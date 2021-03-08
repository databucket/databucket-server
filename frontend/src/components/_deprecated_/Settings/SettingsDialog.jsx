import React from 'react';
import { makeStyles } from '@material-ui/core/styles';
import { Tooltip } from '@material-ui/core';
import Dialog from '@material-ui/core/Dialog';
import AppBar from '@material-ui/core/AppBar';
import Toolbar from '@material-ui/core/Toolbar';
import IconButton from '@material-ui/core/IconButton';
import CloseIcon from '@material-ui/icons/Close';
import Slide from '@material-ui/core/Slide';
import SettingsIcon from '@material-ui/icons/Settings';
import Tabs from '@material-ui/core/Tabs';
import Tab from '@material-ui/core/Tab';
import GroupsTab from './GroupsTab';
import ClassesTab from './ClassesTab';
import BucketsTab from './BucketsTab';
import FiltersTab from './FiltersTab';
import TagsTab from './TagsTab';
import ViewsTab from './ViewsTab';
import ColumnsTab from './ColumnsTab';
import TasksTab from './TasksTab';
import InfoTab from './InfoTab';
import EventsTab from './EventsTab';
import EventsLogTab from './EventsLogTab';

const BUCKET_DEFAULT = 'every';
const CLASS_DEFAULT = 'none';

const useStyles = makeStyles(theme => ({
    appBar: {
        position: 'relative',
    },
    title: {
        marginLeft: theme.spacing(2),
    },
    tabs: {
        flex: 1,
    },
}));

const Transition = React.forwardRef(function Transition(props, ref) {
    return <Slide direction="left" ref={ref} {...props} />;
});

function SettingsDialog(props) {
    const classes = useStyles();
    const [open, setOpen] = React.useState(false);
    const [value, setValue] = React.useState(0);
    const [bucketsLookup, setBucketsLookup] = React.useState(null);
    const [bucketsLookup2, setBucketsLookup2] = React.useState(null);
    const [classesLookup, setClassesLookup] = React.useState(null);
    const [tasksLookup, setTasksLookup] = React.useState(null); // for events log page
    const [eventsLookup, setEventsLookup] = React.useState(null); // for events log page
    const [filters, setFilters] = React.useState(null);
    const [columns, setColumns] = React.useState(null);
    const [tags, setTags] = React.useState(null);
    const [tasks, setTasks] = React.useState(null);
    // const [events, setEvents] = React.useState(null);

    function onBucketsLoaded(buckets) {
        setBucketsLookup(createBucketsLookup(buckets));
        setBucketsLookup2(createBucketsLookup2(buckets));
    }

    function createBucketsLookup(buckets) {
        let lookup = {};
        lookup[BUCKET_DEFAULT] = '- every -';
        if (buckets != null)
            for (var i = 0; i < buckets.length; i++)
                lookup[(buckets[i].bucket_id).toString()] = buckets[i].bucket_name;
        return lookup;
    }

    function createBucketsLookup2(buckets) {
        let lookup = {};
        if (buckets != null)
            for (var i = 0; i < buckets.length; i++)
                lookup[(buckets[i].bucket_id).toString()] = buckets[i].bucket_name;
        return lookup;
    }

    function createTasksLookup(tasks) {
        let lookup = {};
        if (tasks != null)
            for (var i = 0; i < tasks.length; i++)
                lookup[(tasks[i].task_id).toString()] = tasks[i].task_name;
        return lookup;
    }

    function createEventsLookup(events) {
        let lookup = {};
        if (events != null)
            for (var i = 0; i < events.length; i++)
                lookup[(events[i].event_id).toString()] = events[i].event_name;
        return lookup;
    }

    function onClassesLoaded(classes) {
        setClassesLookup(createClassesLookup(classes));
    }

    function createClassesLookup(classes) {
        let lookup = {};
        lookup[CLASS_DEFAULT] = '- none -';
        if (classes != null)
            for (var i = 0; i < classes.length; i++)
                lookup[(classes[i].class_id).toString()] = classes[i].class_name;

        return lookup;
    }

    function onFiltersLoaded(filters) {
        setFilters(filters);
    }

    function onColumnsLoaded(columns) {
        setColumns(columns);
    }

    function onTagsLoaded(tags) {
        setTags(tags);
    }

    function onTasksLoaded(tasks) {
        setTasks(tasks);
        setTasksLookup(createTasksLookup(tasks));
    }

    function onEventsLoaded(events) {
        // setEvents(events);
        setEventsLookup(createEventsLookup(events));
    }

    function handleChange(event, newValue) {
        setValue(newValue);
    }

    function handleClickOpen() {
        setBucketsLookup(createBucketsLookup(props.buckets));
        setBucketsLookup2(createBucketsLookup2(props.buckets));
        setClassesLookup(createClassesLookup(props.classes));
        setTasksLookup(createTasksLookup(props.tasks));
        setEventsLookup(createEventsLookup(props.events));
        setColumns(props.columns);
        setFilters(props.filters);
        setTags(props.tags);
        setTasks(props.tasks);
        setOpen(true);
    }

    function handleClose() {
        props.onClose();
        setOpen(false);
    }

    return (
        <div>
            <Tooltip title="Settings">
                <IconButton edge="end" onClick={handleClickOpen} color="inherit" ><SettingsIcon /></IconButton>
            </Tooltip>
            <Dialog fullScreen open={open} onClose={handleClose} TransitionComponent={Transition}>
                <AppBar className={classes.appBar}>
                    <Toolbar variant="dense">
                        <IconButton color="inherit" edge="start" onClick={handleClose} aria-label="Close">
                            <CloseIcon />
                        </IconButton>
                        <Tabs
                            value={value}
                            onChange={handleChange}
                            variant="scrollable"
                            scrollButtons="on"
                            className={classes.tabs}
                        >
                            <Tab label="Groups" />
                            <Tab label="Classes" />
                            <Tab label="Buckets" />
                            <Tab label="Tags" />
                            <Tab label="Columns" />
                            <Tab label="Filters" />
                            <Tab label="Views" />
                            <Tab label="Tasks" />
                            <Tab label="Events" />
                            <Tab label="Logs" />
                            <Tab label="Info" />
                        </Tabs>
                    </Toolbar>
                </AppBar>
                {value === 0 && <GroupsTab bucketsLookup={bucketsLookup} />}
                {value === 1 && <ClassesTab onClassesLoaded={(classes) => onClassesLoaded(classes)} />}
                {value === 2 && <BucketsTab onBucketsLoaded={(buckets) => onBucketsLoaded(buckets)} classesLookup={classesLookup} />}
                {value === 3 && <TagsTab bucketsLookup={bucketsLookup} classesLookup={classesLookup} onTagsLoaded={(tags) => onTagsLoaded(tags)} />}
                {value === 4 && <ColumnsTab bucketsLookup={bucketsLookup} classesLookup={classesLookup} onColumnsLoaded={(columns) => onColumnsLoaded(columns)} />}
                {value === 5 && <FiltersTab bucketsLookup={bucketsLookup} classesLookup={classesLookup} onFiltersLoaded={(filters) => onFiltersLoaded(filters)} />}
                {value === 6 && <ViewsTab bucketsLookup={bucketsLookup} classesLookup={classesLookup} columns={columns} filters={filters} />}
                {value === 7 && <TasksTab bucketsLookup={bucketsLookup} classesLookup={classesLookup} tags={tags} onTasksLoaded={(tasks) => onTasksLoaded(tasks)} />}
                {value === 8 && <EventsTab bucketsLookup={bucketsLookup} classesLookup={classesLookup} tags={tags} tasks={tasks} onEventsLoaded={(events) => onEventsLoaded(events)} />}
                {value === 9 && <EventsLogTab bucketsLookup={bucketsLookup2} tasksLookup={tasksLookup} eventsLookup={eventsLookup}/>}
                {value === 10 && <InfoTab />}
            </Dialog>
        </div>
    );
}

export default SettingsDialog;