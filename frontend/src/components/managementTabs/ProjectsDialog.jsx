import React from 'react';
import {withStyles} from '@material-ui/core/styles';
import Dialog from '@material-ui/core/Dialog';
import MuiDialogTitle from '@material-ui/core/DialogTitle';
import MuiDialogContent from '@material-ui/core/DialogContent';
import MuiDialogActions from '@material-ui/core/DialogActions';
import IconButton from '@material-ui/core/IconButton';
import CloseIcon from '@material-ui/icons/Close';
import Typography from '@material-ui/core/Typography';
import MoreHoriz from "@material-ui/icons/MoreHoriz";
import Tooltip from "@material-ui/core/Tooltip";
import {
    getPageSizeOptions,
    getTableHeaderBackgroundColor,
    getTableIcons,
    getTableRowBackgroundColor
} from "../../utils/MaterialTableHelper";
import MaterialTable from "material-table";
import {getLastPageSize} from "../../utils/ConfigurationStorage";
import { useTheme } from '@material-ui/core/styles';
import {setSelectionProjects} from "../../utils/JsonHelper";

const styles = (theme) => ({
    root: {
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

const DialogTitle = withStyles(styles)((props) => {
    const {children, classes, onClose, ...other} = props;
    return (
        <MuiDialogTitle disableTypography className={classes.root} {...other}>
            <Typography variant="h6">{children}</Typography>
            {onClose ? (
                <IconButton aria-label="close" className={classes.closeButton} onClick={onClose}>
                    <CloseIcon/>
                </IconButton>
            ) : null}
        </MuiDialogTitle>
    );
});

const DialogContent = withStyles((theme) => ({
    root: {
        padding: theme.spacing(0),
    },
}))(MuiDialogContent);

const DialogActions = withStyles((theme) => ({
    root: {
        margin: 0,
        padding: theme.spacing(1),
    },
}))(MuiDialogActions);

export default function ProjectsDialog(props) {
    const theme = useTheme();
    const [open, setOpen] = React.useState(false);
    const projects = setSelectionProjects(props.projects, props.userRowData['projectsIds']);
    const tableRef = React.createRef();
    const pageSize = getLastPageSize();
    const username = props.userRowData.username;

    const handleClickOpen = () => {
        setOpen(true);
    };

    const handleSave = () => {
        let projectsIds = projects.filter(p => p.tableData != null && p.tableData.checked === true).map(function (p) {
            return p.id
        })
        props.onChange(projectsIds);
        setOpen(false);
    }

    return (
        <div>
            <Tooltip title='Projects'>
                <IconButton
                    onClick={handleClickOpen}
                    color="default"
                >
                    <MoreHoriz/>
                </IconButton>
            </Tooltip>
            <Dialog
                onClose={handleSave}
                aria-labelledby="customized-dialog-title"
                open={open}
                fullWidth={true}
                maxWidth='xl' //'xs' | 'sm' | 'md' | 'lg' | 'xl' | false
            >
                <DialogTitle id="customized-dialog-title" onClose={handleSave}>
                    Select projects
                </DialogTitle>
                <DialogContent dividers>
                    <MaterialTable
                        icons={getTableIcons()}
                        title={`User: ${username}`}
                        tableRef={tableRef}
                        columns={[
                            {
                                title: 'Id',
                                field: 'id',
                                type: 'numeric',
                                editable: 'never',
                                filtering: true,
                                defaultSort: 'asc'
                            },
                            {title: 'Enabled', field: 'enabled', type: 'boolean'},
                            {title: 'Name', field: 'name', type: 'string', editable: 'always', filtering: true},
                            {
                                title: 'Description',
                                field: 'description',
                                type: 'string',
                                editable: 'always',
                                filtering: true
                            },
                            {
                                title: 'Expiration date',
                                field: 'expirationDate',
                                type: 'datetime',
                                editable: 'always',
                                filtering: true,
                                render: rowData =>
                                    <div>{rowData != null ? rowData['expirationDate'] != null ? new Date(rowData['expirationDate']).toLocaleString() : null : null}</div>,
                            },
                            {
                                title: 'Created date',
                                field: 'createdDate',
                                type: 'datetime',
                                editable: 'never',
                                filtering: true,
                                render: rowData =>
                                    <div>{rowData != null ? rowData['createdDate'] != null ? new Date(rowData['createdDate']).toLocaleString() : null : null}</div>,
                            },
                            {title: 'Created by', field: 'createdBy', editable: 'never'},
                            {
                                title: 'Last modified date',
                                field: 'lastModifiedDate',
                                type: 'datetime',
                                editable: 'never',
                                filtering: false,
                                render: rowData =>
                                    <div>{rowData != null ? rowData['lastModifiedDate'] != null ? new Date(rowData['lastModifiedDate']).toLocaleString() : null : null}</div>,
                            },
                            {title: 'Last modified by', field: 'lastModifiedBy', editable: 'never'},
                        ]}
                        data={projects}
                        options={{
                            paging: true,
                            pageSize: pageSize,
                            paginationType: 'stepped',
                            pageSizeOptions: getPageSizeOptions(),
                            actionsColumnIndex: -1,
                            sorting: false,
                            selection: true,
                            filtering: false,
                            padding: 'dense',
                            headerStyle: {backgroundColor: getTableHeaderBackgroundColor(theme)},
                            rowStyle: rowData => ({backgroundColor: getTableRowBackgroundColor(rowData, theme)})
                        }}
                        components={{
                            Container: props => <div {...props} />
                        }}
                    />
                </DialogContent>
                <DialogActions />
            </Dialog>
        </div>
    );
}