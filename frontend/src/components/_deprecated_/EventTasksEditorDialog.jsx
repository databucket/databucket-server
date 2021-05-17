import React, {forwardRef} from 'react';
import {withStyles} from '@material-ui/core/styles';
import Dialog from '@material-ui/core/Dialog';
import Tooltip from '@material-ui/core/Tooltip';
import IconButton from '@material-ui/core/IconButton';
import MuiDialogTitle from '@material-ui/core/DialogTitle';
import MuiDialogContent from '@material-ui/core/DialogContent';
import CloseIcon from '@material-ui/icons/Close';
import Typography from '@material-ui/core/Typography';
import MaterialTable from 'material-table';
import AddBox from '@material-ui/icons/AddBox';
import ArrowUpward from '@material-ui/icons/ArrowUpward';
import Check from '@material-ui/icons/Check';
import ChevronLeft from '@material-ui/icons/ChevronLeft';
import ChevronRight from '@material-ui/icons/ChevronRight';
import Clear from '@material-ui/icons/Clear';
import DeleteOutline from '@material-ui/icons/DeleteOutline';
import Edit from '@material-ui/icons/Edit';
import FilterList from '@material-ui/icons/FilterList';
import FirstPage from '@material-ui/icons/FirstPage';
import LastPage from '@material-ui/icons/LastPage';
import Remove from '@material-ui/icons/Remove';
import SaveAlt from '@material-ui/icons/SaveAlt';
import Search from '@material-ui/icons/Search';
import ViewColumn from '@material-ui/icons/ViewColumn';
import MoreHoriz from '@material-ui/icons/MoreHoriz';
import ArrowDropUp from '@material-ui/icons/ArrowDropUp';
import ArrowDropDown from '@material-ui/icons/ArrowDropDown';

const styles = theme => ({
    root: {
        backgroundColor: '#EEE', //theme.palette.primary.main,
        margin: 0,
        padding: theme.spacing(2),
    },
    closeButton: {
        position: 'absolute',
        right: theme.spacing(1),
        top: theme.spacing(1),
        color: theme.palette.grey[500],
    },
});

const tableIcons = {
    Add: forwardRef((props, ref) => <AddBox {...props} ref={ref}/>),
    Check: forwardRef((props, ref) => <Check {...props} ref={ref}/>),
    Clear: forwardRef((props, ref) => <Clear {...props} ref={ref}/>),
    Delete: forwardRef((props, ref) => <DeleteOutline {...props} ref={ref}/>),
    DetailPanel: forwardRef((props, ref) => <ChevronRight {...props} ref={ref}/>),
    Edit: forwardRef((props, ref) => <Edit {...props} ref={ref}/>),
    Export: forwardRef((props, ref) => <SaveAlt {...props} ref={ref}/>),
    Filter: forwardRef((props, ref) => <FilterList {...props} ref={ref}/>),
    FirstPage: forwardRef((props, ref) => <FirstPage {...props} ref={ref}/>),
    LastPage: forwardRef((props, ref) => <LastPage {...props} ref={ref}/>),
    NextPage: forwardRef((props, ref) => <ChevronRight {...props} ref={ref}/>),
    PreviousPage: forwardRef((props, ref) => <ChevronLeft {...props} ref={ref}/>),
    ResetSearch: forwardRef((props, ref) => <Clear {...props} ref={ref}/>),
    Search: forwardRef((props, ref) => <Search {...props} ref={ref}/>),
    SortArrow: forwardRef((props, ref) => <ArrowUpward {...props} ref={ref}/>),
    ThirdStateCheck: forwardRef((props, ref) => <Remove {...props} ref={ref}/>),
    ViewColumn: forwardRef((props, ref) => <ViewColumn {...props} ref={ref}/>),
};

const DialogTitle = withStyles(styles)(props => {
    const {children, classes, onClose} = props;
    return (
        <MuiDialogTitle disableTypography className={classes.root}>
            <Typography variant="h6">{children}</Typography>
            {onClose ? (
                <IconButton aria-label="Close" className={classes.closeButton} onClick={onClose}>
                    <CloseIcon/>
                </IconButton>
            ) : null}
        </MuiDialogTitle>
    );
});

const DialogContent = withStyles(theme => ({
    root: {
        padding: theme.spacing(0),
    },
}))(MuiDialogContent);

export default class EventTasksEditorDialog extends React.Component {

    constructor(props) {
        super(props);
        this.tableRef = React.createRef();
        this.state = {
            data: null,
            open: false,
            initiated: false,
            columns: null
        };
    }

    static getDerivedStateFromProps(props, state) {
        let newState = state;
        if (state.open === true && !state.initiated) {
            let bucket_id = props.rowData.bucket_id;
            let class_id = props.rowData.class_id;

            if (bucket_id === 'every')
                bucket_id = null;
            else
                bucket_id = parseInt(bucket_id);

            if (class_id === 'none')
                class_id = null;
            else
                class_id = parseInt(class_id);

            let filteredTasks = props.tasks.filter(task => (task.bucket_id === bucket_id || task.bucket_id == null) && (task.class_id === class_id || task.class_id == null));
            let tasksLookup = {};

            for (const task of filteredTasks)
                tasksLookup[task.task_id] = task.task_name;

            newState.columns = [
                {title: '#', render: (rowData) => rowData ? rowData.tableData.id + 1 : ''},
                {title: 'Name', field: 'task_id', lookup: tasksLookup}
            ]

            newState.initiated = true;
            newState.data = props.rowData.tasks != null ? props.rowData.tasks : [];
            return newState;
        } else {
            return newState;
        }
    }

    isMoveDownDisabled(rowData) {
        const disabled = rowData.tableData.id === this.state.data.length - 1;
        // console.log("MoveDown: " + rowData.tableData.id + " -> " + disabled);
        return disabled;
    }

    isMoveUpDisabled(rowData) {
        const disabled = rowData.tableData.id === 0;
        // console.log("MoveUp: " + rowData.tableData.id + " -> " + disabled);
        return disabled;
    }

    moveUp(rowData) {
        const data = this.state.data;
        const dataCopy = JSON.parse(JSON.stringify(this.state.data));
        const index = data.findIndex(item => (item.tableData.id === rowData.tableData.id))
        data[index].task_id = dataCopy[index - 1].task_id;
        data[index - 1].task_id = dataCopy[index].task_id;
        this.setState({data: data});
    }

    moveDown(rowData) {
        const data = this.state.data;
        const dataCopy = JSON.parse(JSON.stringify(this.state.data));
        const index = data.findIndex(item => (item.tableData.id === rowData.tableData.id))
        data[index].task_id = dataCopy[index + 1].task_id;
        data[index + 1].task_id = dataCopy[index].task_id;
        this.setState({data: data});
    }

    handleClickOpen = () => {
        this.setState({
            open: true,
        });
    };

    handleClose = () => {
        this.props.onChange(this.state.data);
        this.setState({open: false, initiated: false});
    };

    render() {
        return (
            <div>
                <Tooltip title='Tasks configuration'>
                    <IconButton
                        onClick={this.handleClickOpen}
                        color="default"
                    >
                        <MoreHoriz/>
                    </IconButton>
                </Tooltip>
                <Dialog
                    onClose={this.handleClose} // Enable this to close editor by clicking outside the dialog
                    aria-labelledby="customized-dialog-title"
                    open={this.state.open}
                    fullWidth={true}
                    maxWidth='md' //'xs' | 'sm' | 'md' | 'lg' | 'xl' | false
                >
                    <DialogTitle id="customized-dialog-title" onClose={this.handleClose}>
                        Event task configuration
                    </DialogTitle>
                    <DialogContent dividers>
                        <MaterialTable
                            title="Task list"
                            tableRef={this.tableRef}
                            columns={this.state.columns}
                            data={this.state.data}
                            options={{
                                paging: true,
                                pageSize: 15,
                                paginationType: 'stepped',
                                pageSizeOptions: [15, 20, 25],
                                actionsColumnIndex: -1,
                                sorting: false,
                                search: false,
                                filtering: false,
                                padding: 'dense',
                                headerStyle: {backgroundColor: '#eeeeee'},
                                rowStyle: rowData => ({backgroundColor: rowData.tableData.id % 2 === 1 ? '#fafafa' : '#FFF'})
                            }}
                            components={{
                                Container: props => <div {...props} />
                            }}
                            editable={{
                                onRowAdd: newData =>
                                    new Promise((resolve, reject) => {
                                        if (newData.task_id == null) {
                                            window.alert("Select task for new item.");
                                            reject();
                                            return;
                                        }

                                        let data = this.state.data;
                                        data.push(newData);
                                        this.setState({data: data}, () => resolve());
                                    }),
                                onRowUpdate: (newData, oldData) =>
                                    new Promise((resolve, reject) => {
                                        const data = this.state.data;
                                        const index = data.findIndex(item => (item.tableData.id === oldData.tableData.id))
                                        data[index] = newData;
                                        this.setState({data: data}, () => resolve());
                                    }),
                                onRowDelete: oldData =>
                                    new Promise((resolve, reject) => {
                                        const data = this.state.data;
                                        const index = data.findIndex(item => (item.tableData.id === oldData.tableData.id))
                                        data.splice(index, 1);
                                        this.setState({data: data}, () => resolve());
                                    }),
                            }}
                            actions={[
                                rowData => ({
                                    icon: () => <ArrowDropUp/>,
                                    tooltip: 'Move up',
                                    onClick: (event, rowData) => this.moveUp(rowData),
                                    disabled: this.isMoveUpDisabled(rowData)
                                }),
                                rowData => ({
                                    icon: () => <ArrowDropDown/>,
                                    tooltip: 'Move down',
                                    onClick: (event, rowData) => this.moveDown(rowData),
                                    disabled: this.isMoveDownDisabled(rowData)
                                })
                            ]}
                        />
                    </DialogContent>
                </Dialog>
            </div>
        );
    }
}
