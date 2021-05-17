import React, { forwardRef } from 'react';
import { withStyles } from '@material-ui/core/styles';
import Dialog from '@material-ui/core/Dialog';
import IconButton from '@material-ui/core/IconButton';
import MuiDialogTitle from '@material-ui/core/DialogTitle';
import MuiDialogContent from '@material-ui/core/DialogContent';
import MuiDialogActions from '@material-ui/core/DialogActions';
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
import LockedIcon from '@material-ui/icons/Lock';
import UnlockedIcon from '@material-ui/icons/LockOpen';
import DataHistoryPropertiesDiffDialog from './DataHistoryPropertiesDiffDialog';

const styles = theme => ({
  root: {
    backgroundColor: '#EEE', //theme.palette.primary.main,
    margin: 0,
    padding: theme.spacing(2),
  },
  container: {
    display: 'flex',
    flexWrap: 'wrap',
  },
  closeButton: {
    position: 'absolute',
    right: theme.spacing(1),
    top: theme.spacing(1),
    color: theme.palette.grey[500],
  }
});

const tableIcons = {
  Add: forwardRef((props, ref) => <AddBox {...props} ref={ref} />),
  Check: forwardRef((props, ref) => <Check {...props} ref={ref} />),
  Clear: forwardRef((props, ref) => <Clear {...props} ref={ref} />),
  Delete: forwardRef((props, ref) => <DeleteOutline {...props} ref={ref} />),
  DetailPanel: forwardRef((props, ref) => <ChevronRight {...props} ref={ref} />),
  Edit: forwardRef((props, ref) => <Edit {...props} ref={ref} />),
  Export: forwardRef((props, ref) => <SaveAlt {...props} ref={ref} />),
  Filter: forwardRef((props, ref) => <FilterList {...props} ref={ref} />),
  FirstPage: forwardRef((props, ref) => <FirstPage {...props} ref={ref} />),
  LastPage: forwardRef((props, ref) => <LastPage {...props} ref={ref} />),
  NextPage: forwardRef((props, ref) => <ChevronRight {...props} ref={ref} />),
  PreviousPage: forwardRef((props, ref) => <ChevronLeft {...props} ref={ref} />),
  ResetSearch: forwardRef((props, ref) => <Clear {...props} ref={ref} />),
  Search: forwardRef((props, ref) => <Search {...props} ref={ref} />),
  SortArrow: forwardRef((props, ref) => <ArrowUpward {...props} ref={ref} />),
  ThirdStateCheck: forwardRef((props, ref) => <Remove {...props} ref={ref} />),
  ViewColumn: forwardRef((props, ref) => <ViewColumn {...props} ref={ref} />),
};

const DialogTitle = withStyles(styles)(props => {
  const { children, classes, onClose } = props;
  return (
    <MuiDialogTitle disableTypography className={classes.root}>
      <Typography variant="h6">{children}</Typography>
      {onClose ? (
        <IconButton aria-label="Close" className={classes.closeButton} onClick={onClose}>
          <CloseIcon />
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

const DialogActions = withStyles(theme => ({
  root: {
    margin: 0,
    padding: theme.spacing(1),
  },
}))(MuiDialogActions);

export default class DataHistoryDialog extends React.Component {

  constructor(props) {
    super(props);
    this.tableRef = React.createRef();
    this.state = {
      bucket: null,
      dataRowId: null,
      history: [],
      open: false,
      columns: []
    };
  }

  static getDerivedStateFromProps(props, state) {
    if (props.open === true) {
      let tagsLookup = {};
      for (let j = 0; j < props.tags.length; j++) {
        const tag = props.tags[j];
        tagsLookup[tag.tag_id] = tag.tag_name;
      }

      let preparedColumns = [
        { title: 'Id', field: 'index' },
        {
          title: 'Updated at', field: 'updated_at', type: 'datetime', editable: 'never',
          render: rowData => <div>{rowData != null ? rowData.updated_at != null ? new Date(rowData.updated_at).toLocaleString() : null : null}</div>
        },
        { title: 'Updated by', field: 'updated_by', editable: 'never' },
        { title: 'Tag', field: 'tag_id', editable: 'never', lookup: tagsLookup },
        {
          title: 'Locked', field: 'locked', editable: 'never',
          render: rowData => <div>{rowData.locked != null ? rowData.locked ? <LockedIcon color="action" /> : <UnlockedIcon color="action" /> : ''}</div>
        },
        {
          title: 'Properties', field: 'properties', editable: 'never',
          render: rowData => <div>{rowData.properties != null ?
            <DataHistoryPropertiesDiffDialog
              bucket={props.bucket}
              dataRowId={props.dataRowId}
              history={props.history}
              selectedRow={rowData}
            /> : ''}</div>
        }
      ]

      if (props.history != null)
        for (var i = 0; i < props.history.length; i++)
          props.history[i]['index'] = i + 1;

      return {
        bucket: props.bucket,
        dataRowId: props.dataRowId,
        history: props.history,
        columns: preparedColumns,
        open: props.open
      };
    } else
      return null;
  }

  addRowId(history) {
    if (history != null)
      for (var i = 0; i < history.length; i++)
        history[i]['index'] = i + 1;

    return history;
  }

  handleClose = () => {
    this.props.onClose();
    this.setState({ open: false });
  };

  render() {
    return (
      <Dialog
        onClose={this.handleClose} // Enable this to close editor by clicking outside the dialog
        aria-labelledby="customized-dialog-title"
        open={this.state.open}
        fullWidth={true}
        maxWidth='lg'  //'xs' | 'sm' | 'md' | 'lg' | 'xl' | false
      >
        <DialogTitle id="customized-dialog-title" onClose={this.handleClose}>
          Data history [data id: {this.state.dataRowId}]
          </DialogTitle>
        <DialogContent dividers>
          <MaterialTable

            // title={this.state.title}
            tableRef={this.tableRef}
            columns={this.state.columns}
            data={this.state.history}
            // onRowClick={((evt, selectedRow) => this.setState({ selectedRow }))}
            options={{
              toolbar: false,
              // showTitle: false,
              paging: false,
              // pageSize: this.pageSize,
              // pageSize: 15,
              // paginationType: 'stepped',
              // pageSizeOptions: [15, 20, 25, 30],
              actionsColumnIndex: -1,
              sorting: false,
              search: false,
              filtering: false,
              padding: 'dense',
              // headerStyle:{backgroundColor:'#eeeeee'},
              // rowStyle: rowData => ({ backgroundColor: rowData.tableData.id % 2 === 1 ? '#fafafa' : '#FFF' })
            }}
            components={{
              Container: props => <div {...props} />
            }}
          />
        </DialogContent>
        <DialogActions>
          {/* <Button id="saveButton" onClick={this.handleSave} disabled={!this.state.changed || !this.state.valid} color="primary">
            Save
            </Button> */}
        </DialogActions>
      </Dialog>
    );
  }
}
