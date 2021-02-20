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
import ToggleOff from '@material-ui/icons/ToggleOff';
import ToggleOn from '@material-ui/icons/ToggleOn'
import DynamicIcon from '../utils/DynamicIcon';
import EditIconDialog from '../project/dialogs/EditIconDialog';

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

export default class ColumnValuesEditorDialog extends React.Component {

  constructor(props) {
    super(props);
    this.tableRef = React.createRef();
    this.state = {
      text_values: props.rowData.def_values != null ? props.rowData.def_values.text_values : true,
      data: props.rowData.def_values != null && props.rowData.def_values.items != null ? props.rowData.def_values.items : [],
      title: "Column: " + props.rowData.title,
      open: false,
      columns: this.getColumns(props.rowData.def_values != null ? props.rowData.def_values.text_values : true)
    };
  }

  getColumns(text_values) {
    var columnsArray = [];
    columnsArray.push({ title: 'Key', field: 'key' });
    if (text_values)
      columnsArray.push({ title: 'Text', field: 'text_value' });
    else
      columnsArray.push({ title: 'Icon', field: 'icon_name', 
        render: rowData => <DynamicIcon iconName={rowData.icon_name} color='action' />,
        editComponent: props => <EditIconDialog value={props.value} onChange={props.onChange} />});
    return columnsArray;
  }

  handleClickOpen = () => {
    this.setState({
      open: true,
    });
  };

  handleClose = () => {
    let dataClone = JSON.parse(JSON.stringify(this.state.data));
    for (var i = 0; i < dataClone.length; i++) {
      let col = dataClone[i];
      delete col['tableData'];
      delete col['sorting'];
      delete col['filtering'];
    }
    this.props.onChange({text_values: this.state.text_values, items: dataClone});
    this.setState({ open: false });
  };

  render() {
    return (
      <div>
        <Tooltip title='Columns'>
          <IconButton onClick={this.handleClickOpen} color='default' >
            <RepeatIcon />
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
            Values configuration
          </DialogTitle>
          <DialogContent dividers>
            <MaterialTable
              icons={tableIcons}
              title={this.state.title}
              tableRef={this.tableRef}
              columns={this.state.columns}
              data={this.state.data}
              options={{
                // showTitle: false,
                paging: true,
                // pageSize: this.pageSize,
                pageSize: 15,
                paginationType: 'stepped',
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
                    const oldRow = data.filter(r => r.key === oldData.key)[0];
                    oldRow.key = newData.key;
                    oldRow.icon_name = newData.icon_name;
                    oldRow.text_value = newData.text_value;
                    this.setState({ data: data }, () => resolve());
                  }),
                onRowDelete: oldData =>
                  new Promise((resolve, reject) => {
                    const data = this.state.data;
                    const oldRow = data.filter(r => r.key === oldData.key)[0];
                    const index = data.indexOf(oldRow);
                    data.splice(index, 1);
                    this.setState({ data: data }, () => resolve());
                  }),
              }}
              actions={[
                {
                  icon: () => this.state.text_values === true ? <ToggleOff /> : <ToggleOn color='secondary' />,
                  tooltip: this.state.text_values === true ? 'Icon values' : 'Text values',
                  isFreeAction: true,
                  onClick: () => this.setState({ text_values: !this.state.text_values, columns: this.getColumns(!this.state.text_values) })
                }
              ]}
            />
          </DialogContent>
          <br />
        </Dialog>
      </div>
    );
  }
}
