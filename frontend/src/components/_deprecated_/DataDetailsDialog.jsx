import React, { forwardRef } from 'react';
import { withStyles } from '@material-ui/core/styles';
import PropTypes from 'prop-types';
import MaterialTable from 'material-table';
import Button from '@material-ui/core/Button';
import Dialog from '@material-ui/core/Dialog';
import IconButton from '@material-ui/core/IconButton';
import MuiDialogTitle from '@material-ui/core/DialogTitle';
import MuiDialogContent from '@material-ui/core/DialogContent';
import MuiDialogActions from '@material-ui/core/DialogActions';
import JSONInput from 'react-json-editor-ajrm';
import locale from 'react-json-editor-ajrm/locale/en';
import CloseIcon from '@material-ui/icons/Close';
import Typography from '@material-ui/core/Typography';
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
import { Divider } from '@material-ui/core';


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

const titleStyles = theme => ({
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

const DialogTitle = withStyles(titleStyles)(props => {
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

const styles = theme => ({
  dialogPaper: {
    minHeight: '98vh',
  }
});

class DataDetailsDialog extends React.Component {

  constructor(props) {
    super(props);
    this.changedProperties = null;
    this.state = {
      dataRow: null,
      open: false,
      changed: false,
      valid: true,
      columns: [
        { title: 'Data id', field: 'data_id', type: 'numeric' },
        { title: 'Tag id', field: 'tag_id', type: 'numeric' },
        { title: 'Tag name', field: 'tag_name', type: 'string' },
        { title: 'Locked', field: 'locked', type: 'boolean' },
        { title: 'Locked by', field: 'locked_by', type: 'string' },        
        { title: 'Created at', field: 'created_at', type: 'datetime' },
        { title: 'Created by', field: 'created_by', type: 'string' },        
        { title: 'Updated at', field: 'updated_at', type: 'datetime' },
        { title: 'Updated by', field: 'updated_by', type: 'string' }
      ]
    };
  }

  static getDerivedStateFromProps(props, state) {
    return { dataRow: props.dataRow, open: props.open };
  }

  handleChanged = (contentValues) => {
    this.changedProperties = contentValues.jsObject;
    this.setState({ valid: contentValues.error === false, changed: true });
  }

  handleSave = () => {
    let changedDataRow = this.state.dataRow;
    changedDataRow.properties = this.changedProperties;
    this.props.onChange(changedDataRow, true);
    this.setState({ open: false, changed: false });
  }

  handleClose = () => {
    this.props.onChange(null, false);
    this.setState({ open: false, changed: false });
  };

  render() {
    const { classes } = this.props;
    return (
      <Dialog
        onClose={this.handleClose} // Enable this to close editor by clicking outside the dialog
        aria-labelledby="customized-dialog-title"
        classes={{ paper: classes.dialogPaper }}
        open={this.state.open}
        fullWidth={true}
        maxWidth='xl'  //'xs' | 'sm' | 'md' | 'lg' | 'xl' | false
      >
        <DialogTitle id="customized-dialog-title" onClose={this.handleClose}>
          Data details
        </DialogTitle>
        <Divider />
        <MaterialTable

            tableRef={this.tableRef}
            columns={this.state.columns}
            data={[this.state.dataRow]}
            options={{
              paging: false,
              toolbar: false,
              actionsColumnIndex: -1,
              sorting: false,
              search: false,
              filtering: false,
              padding: 'dense',
              // headerStyle: { backgroundColor: '#fafafa', height: 120 },
              // rowStyle: { backgroundColor: '#fafafa' }
            }}
            components={{
              Container: props => <div {...props} />
            }}
          />
        <DialogContent>          
          <JSONInput
            id='json_editor'
            placeholder={this.state.dataRow !== null ? this.state.dataRow.properties : null}
            theme="light_mitsuketa_tribute"
            locale={locale}
            style={{ body: { fontSize: 'large', fontWeight: 'bold' }, errorMessage: { fontSize: 'large' } }}
            width="100%"
            height="100%"
            onKeyPressUpdate={true}
            waitAfterKeyPress={1000}
            onChange={(jsObject) => this.handleChanged(jsObject)}
          />
        </DialogContent>
        <Divider />
        <DialogActions>
          <Button id="saveButton" onClick={this.handleSave} disabled={!this.state.changed || !this.state.valid} color="primary">
            Save
            </Button>
        </DialogActions>
      </Dialog>
    );
  }
}

DataDetailsDialog.propTypes = {
  classes: PropTypes.object.isRequired,
};

export default withStyles(styles)(DataDetailsDialog);
