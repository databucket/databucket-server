import React, { forwardRef } from 'react';
import { withStyles } from '@material-ui/core/styles';
import Dialog from '@material-ui/core/Dialog';
import Tooltip from '@material-ui/core/Tooltip';
import IconButton from '@material-ui/core/IconButton';
import MuiDialogTitle from '@material-ui/core/DialogTitle';
import MuiDialogContent from '@material-ui/core/DialogContent';
import RepeatIcon from '@material-ui/icons/Repeat';
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
import ColumnValuesEditorDialog from './ColumnValuesEditorDialog';

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

export default class ColumnsEditorDialog extends React.Component {

  constructor(props) {
    super(props);
    // this.pageSize = this.getLastPageSize();
    this.tableRef = React.createRef();
    this.state = {
      data: props.json,
      title: props.title,
      open: false,
      columns: [
        { title: '#', render: (rowData) => rowData ? rowData.tableData.id + 1 : ''},
        { title: 'Title', field: 'title' },
        { title: 'Source', field: 'field' },
        { title: 'Type', field: 'type', lookup: { 'string': 'String', 'numeric': 'Numeric', 'datetime': 'Datetime', 'date': 'Date', 'time': 'Time', 'boolean': 'Boolean' } },
        { title: 'Width', field: 'width', type: 'string', editable: 'always'},
        {
          title: 'Values', field: 'def_values',
          render: rowData => rowData.field != null ? (rowData.field.startsWith('$') && rowData.type === 'string') ? <RepeatIcon color='action' /> : <div /> : <div />,
          editComponent: props => props.rowData.field != null ? (props.rowData.field.startsWith('$') && props.rowData.type === 'string') ? <ColumnValuesEditorDialog rowData={props.rowData} onChange={props.onChange} /> : <div /> : <div />
        },
        { title: 'Editable', field: 'editable', lookup: { 'always': 'Always', 'never': 'Never', 'onAdd': 'On add', 'onUpdate': 'On update' } },
        { title: 'Sorting', field: 'sorting', type: 'boolean' },
        { title: 'Filtering', field: 'filtering', type: 'boolean' },
      ]
    };
  }

  // static getDerivedStateFromProps(props, state) {
  //   console.log('1. getDerivedStateFromProps');
  //   console.log(props);
  //   console.log(state);
  //   return state;
  // }  

  // shouldComponentUpdate(nextProps, nextState) {
  //   console.log('ColumnsEditorDialog.shouldComponentUpdate()');
  //   // console.log(nextProps);
  //   console.log(nextState);
  //   return true;
  // }

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
    // console.log("MoveUp action");
    const data = this.state.data;
    // const index = data.indexOf(rowData);
    const index = data.findIndex(item => (item.title === rowData.title && item.source === rowData.source))
    const previousRowData = data[index - 1];
    data[index] = previousRowData;
    data[index - 1] = rowData;
    this.setState({ data: data });
  }

  moveDown(rowData) {
    // console.log("MoveDown action");
    const data = this.state.data;
    // const index = data.indexOf(rowData);
    const index = data.findIndex(item => (item.title === rowData.title && item.source === rowData.source))
    const nextRowData = data[index + 1];
    // const nextRowDataTableDataId = nextRowData.tableData.id
    // const
    data[index] = nextRowData;
    data[index + 1] = rowData;
    this.setState({ data: data });
  }

  handleClickOpen = () => {
    this.setState({
      open: true,
    });
  };

  handleClose = () => {
    if (typeof this.state.data !== 'undefined' && this.state.data !== null && this.state.data.length > 0) {
      let dataClone = JSON.parse(JSON.stringify(this.state.data));
      for (var i = 0; i < dataClone.length; i++) {
        let col = dataClone[i];
        delete col['tableData'];
      }

      this.props.onChange(dataClone);
    } else
      this.props.onChange(this.state.data);

    this.setState({ open: false });
  };

  getDefaultColumns() {
    return [
      { title: 'Id', field: 'data_id', type: 'numeric', editable: 'never', sorting: true, filtering: true },
      { title: 'Tag', field: 'tag_id', type: 'numeric', editable: 'always', sorting: true, filtering: true },
      { title: 'Locked', field: 'locked', type: 'boolean', editable: 'always', sorting: true, filtering: true },
      { title: 'Locked by', field: 'locked_by', type: 'string', editable: 'never', sorting: true, filtering: true },
      { title: 'Created by', field: 'created_by', type: 'string', editable: 'never', sorting: true, filtering: true },
      { title: 'Created at', field: 'created_at', type: 'datetime', editable: 'never', sorting: true, filtering: true },
      { title: 'Updated by', field: 'updated_by', type: 'string', editable: 'never', sorting: true, filtering: true },
      { title: 'Updated at', field: 'updated_at', type: 'datetime', editable: 'never', sorting: true, filtering: true },
      { title: 'Property', field: '$.property', type: 'numeric', editable: 'always', sorting: true, filtering: true }
    ];
  }

  render() {
    // console.log("ColumnsEditorDialog -> Render table");
    // console.log(this.state.data);
    return (
      <div>
        <Tooltip title='Columns'>
          <IconButton
            onClick={this.handleClickOpen}
            color="default"
          >
            <MoreHoriz />
          </IconButton>
        </Tooltip>
        <Dialog
          onClose={this.handleClose} // Enable this to close editor by clicking outside the dialog
          aria-labelledby="customized-dialog-title"
          open={this.state.open}
          fullWidth={true}
          maxWidth='xl' //'xs' | 'sm' | 'md' | 'lg' | 'xl' | false
        >
          <DialogTitle id="customized-dialog-title" onClose={this.handleClose}>
            Columns configuration
          </DialogTitle>
          <DialogContent dividers>
            <MaterialTable
              icons={tableIcons}
              title={this.state.title}
              tableRef={this.tableRef}
              columns={this.state.columns}
              data={this.state.data}
              options={{
                paging: true,
                pageSize: 15,
                // paginationType: 'stepped',
                pageSizeOptions: [15, 20, 25],
                actionsColumnIndex: -1,
                sorting: false,
                search: false,
                filtering: false,
                padding: 'dense',
                headerStyle: { backgroundColor: '#eeeeee' },
                rowStyle: rowData => ({ backgroundColor: rowData.tableData.id % 2 === 1 ? '#fafafa' : '#FFF' })
              }}
              components={{
                Container: props => <div {...props} />
              }}
              editable={{
                onRowAdd: newData =>
                  new Promise((resolve, reject) => {
                    let data = this.state.data;
                    if (typeof data === 'undefined' || data === null) {
                      data = [];
                    }

                    if (newData.filtering == null)
                      newData.filtering = false;

                    if (newData.sorting == null)
                      newData.sorting = false;

                    data.push(newData);
                    this.setState({ data: data }, () => resolve());
                  }),
                onRowUpdate: (newData, oldData) =>
                  new Promise((resolve, reject) => {
                    const data = this.state.data;
                    // const oldRow = data.filter(r => r.field === oldData.field)[0];
                    // const index = data.indexOf(oldRow);
                    const index = data.findIndex(item => (item.title === oldData.title && item.source === oldData.source))
                    data[index] = newData;
                    this.setState({ data: data }, () => resolve());
                  }),
                onRowDelete: oldData =>
                  new Promise((resolve, reject) => {
                    const data = this.state.data;
                    // const oldRow = data.filter(r => r.field === oldData.field)[0];
                    // const index = data.indexOf(oldRow);
                    const index = data.findIndex(item => (item.title === oldData.title && item.source === oldData.source))
                    data.splice(index, 1);
                    this.setState({ data: data }, () => resolve());
                  }),
              }}
              actions={[
                {
                  icon: () => <ViewColumn />,
                  tooltip: 'Set default columns',
                  isFreeAction: true,
                  onClick: () => this.setState({ data: this.getDefaultColumns() }),
                },
                rowData => ({
                  icon: () => <ArrowDropUp />,
                  tooltip: 'Move up',
                  onClick: (event, rowData) => this.moveUp(rowData),
                  disabled: this.isMoveUpDisabled(rowData)
                }),
                rowData => ({
                  icon: () => <ArrowDropDown />,
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
