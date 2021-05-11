import React, { forwardRef } from 'react';
import { withStyles } from '@material-ui/core/styles';
// import Divider from '@material-ui/core/Divider';
import PropTypes from 'prop-types';
import Dialog from '@material-ui/core/Dialog';
import Tooltip from '@material-ui/core/Tooltip';
import IconButton from '@material-ui/core/IconButton';
import MoreHoriz from '@material-ui/icons/MoreHoriz';
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
import Stepper from '@material-ui/core/Stepper';
import Step from '@material-ui/core/Step';
import StepLabel from '@material-ui/core/StepLabel';
// import Button from '@material-ui/core/Button';
import Radio from '@material-ui/core/Radio';
import RadioGroup from '@material-ui/core/RadioGroup';
import FormControlLabel from '@material-ui/core/FormControlLabel';
import FormControl from '@material-ui/core/FormControl';
import FormGroup from '@material-ui/core/FormGroup';
import Checkbox from '@material-ui/core/Checkbox';
import Select from '@material-ui/core/Select';
import MenuItem from '@material-ui/core/MenuItem';
import PropertyTypeLookup from './Settings/PropertyTypeLookup';
import PropertyValueLookup from './Settings/PropertyValueLookup';

const BUCKET_DEFAULT = 'every';
const CLASS_DEFAULT = 'none';

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

const DialogActions = withStyles(theme => ({
  root: {
    margin: 0,
    padding: theme.spacing(1),
  },
}))(MuiDialogActions);

const styles = theme => ({
  root: {
    width: '100%'
  },
  dialogPaper: {
    minHeight: '70vh',
  },
  content: {
    padding: theme.spacing(0)
  },
  instructions: {
    marginTop: theme.spacing(1),
    marginBottom: theme.spacing(1),
  }
});

class TaskConfigurationEditorDialog extends React.Component {

  constructor(props) {
    super(props);
    this.steps = ['Define conditions', 'Define actions'];
    this.tableConditionsRef = React.createRef();
    this.tableActionsRef = React.createRef();
    this.tags = null;
    this.state = {
      activeStep: 0,
      data: props.data.configuration != null ? props.data.configuration : {
        conditions: [],
        actions: { type: 'none', set_tag: false, tag_id: 0, set_lock: false, lock: false, properties: [] }
      },
      title: props.title,
      open: false,
      conditionsColumns: [
        { title: 'Left source', field: 'left_source', lookup: { 'const': 'Const', 'field': 'Field', 'property': 'Property', 'function': 'Function' } },
        { title: "Left value", field: 'left_value', emptyValue: 'null' },
        {
          title: 'Operator', field: 'operator',
          lookup: { '=': '=', '>': '>', '>=': '>=', '<': '<', '<=': '<=', '<>': '<>', 'in': 'in', 'not in': 'not in', 'is': 'is', 'is not': 'is not', 'like': 'like', 'not like': 'not like' }
        },
        { title: 'Right source', field: 'right_source', lookup: { 'const': 'Const', 'field': 'Field', 'property': 'Property', 'function': 'Function' } },
        { title: "Right value", field: 'right_value', emptyValue: 'null' }
      ],
      propertiesColumns: [
        { title: 'Action', field: 'action', initialEditValue: 'set', lookup: { 'remove': 'Remove', 'set': 'Set' }, editable: 'onAdd' },
        { title: "Path", field: 'path' },
        {
          title: 'Type', field: 'type', initialEditValue: 'string', emptyValue: '',
          lookup: { 'string': 'String', 'numeric': 'Numeric', 'datetime': 'Datetime', 'date': 'Date', 'time': 'Time', 'boolean': 'Boolean', 'null': 'Null' },
          editComponent: props => props.rowData.action != null ? props.rowData.action === 'set' ? <PropertyTypeLookup type={props.rowData.type} onChange={props.onChange} /> : <div /> : <div />
        },
        {
          title: "Value", field: 'value',
          editComponent: props => props.rowData.action != null ? props.rowData.action === 'set' ? <PropertyValueLookup rowData={props.rowData} onChange={props.onChange} /> : <div /> : <div />
        },
      ]
    };
  }

  shouldComponentUpdate(nextProps, nextState) {
    if (this.props.tags) {
      let bucket_id = null;
      let class_id = null;

      if (nextProps.data.bucket_id !== BUCKET_DEFAULT)
        bucket_id = parseInt(nextProps.data.bucket_id);

      if (nextProps.data.class_id !== CLASS_DEFAULT)
        class_id = parseInt(nextProps.data.class_id);

      this.tags = this.props.tags.filter(tag => (tag.bucket_id === bucket_id || tag.bucket_id === BUCKET_DEFAULT) && (tag.class_id === class_id || tag.class_id === CLASS_DEFAULT));
    }

    return true;
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

  // handleFirstPage = () => {
  //   this.setState({ activeStep: 0 });
  // };

  handlePreviousPage = () => {
    this.setState({ activeStep: this.state.activeStep - 1 });
  };

  handleNextPage = () => {
    this.setState({ activeStep: this.state.activeStep + 1 });
  };

  // handleLastPage = () => {
  //   this.setState({ activeStep: this.steps.length - 1 });
  // };

  handleActionTypeChange = (event) => {
    let newState = this.state;
    newState.data.actions.type = event.target.value;
    this.setState(newState);
  }

  handleActionSetTag = (event) => {
    let newState = this.state;
    newState.data.actions.set_tag = event.target.checked;
    this.setState(newState);
  }

  handleActionSetLock = (event) => {
    let newState = this.state;
    newState.data.actions.set_lock = event.target.checked;
    this.setState(newState);
  }

  handleActionLockChange = (event) => {
    let newState = this.state;
    newState.data.actions.lock = (event.target.value === 'true');
    this.setState(newState);
  }

  handleTagChange = (event) => {
    let newState = this.state;
    newState.data.actions.tag_id = event.target.value;
    this.setState(newState);
  }

  isProperField(value) {
    const fields = ['data_id', 'tag_id', 'tag_name', 'locked', 'locked_by', 'created_by', 'created_at', 'updated_by', 'updated_at', 'properties'];
    let result = fields.includes(value);

    if (!result)
      window.alert("Incorrect field name.\nAcceptable values: \n" + fields);

    return result;
  }

  isProperProperty(value) {
    if (!value.startsWith('$.')) {
      window.alert("Incorrect property name!\nProperty name must be a valid JSON path.\nExamples:\n$.name\n$.address.street\n$.colors[0]");
      return false;
    }
    return true;
  }

  convertConstValue(value) {
    if (typeof value === 'string' || value instanceof String) {
      let val = parseFloat(value);
      if (isNaN(val)) {
        if (value.toUpperCase() === 'TRUE')
          return true;
        else if (value.toUpperCase() === 'FALSE')
          return false;
        else if (value.toUpperCase() === 'NULL')
          return null;
        else
          return value;
      } else
        return val;
    } else
      return value;
  }

  vefiryCondition(condition) {
    if (condition.left_source == null || condition.left_value == null || condition.operator == null || condition.right_source == null | condition.right_value == null) {
      window.alert("All properties are obligatory!");
      return false;
    }

    if (condition.left_source === 'field' && !this.isProperField(condition.left_value))
      return false;

    if (condition.right_source === 'field' && !this.isProperField(condition.right_value))
      return false;

    if (condition.left_source === 'property' && !this.isProperProperty(condition.left_value))
      return false;

    if (condition.right_source === 'property' && !this.isProperProperty(condition.right_value))
      return false;

    if (condition.left_source === 'const')
      condition.left_value = this.convertConstValue(condition.left_value);

    if (condition.right_source === 'const')
      condition.right_value = this.convertConstValue(condition.right_value);

    return true;
  }

  render() {
    const { classes } = this.props;
    // console.log("this.state.data.actions");
    // console.log(this.state.data.actions);
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
          classes={{ paper: classes.dialogPaper }}
          fullWidth={true}
          maxWidth='lg' //'xs' | 'sm' | 'md' | 'lg' | 'xl' | false
        >
          <DialogTitle id="dialog-title" onClose={this.handleClose}>
            {this.state.title}
          </DialogTitle>
          <MuiDialogContent dividers className={classes.content}>
            {/* <Divider variant="fullWidth" /> */}
            <Stepper activeStep={this.state.activeStep}>
              {this.steps.map(label => (
                <Step key={label}>
                  <StepLabel>{label}</StepLabel>
                </Step>
              ))}
            </Stepper>
            {/* <Divider variant="fullWidth" /> */}
            <div>
              {this.state.activeStep === 0 ? (
                <MaterialTable
                  // icons={tableIcons}
                  title="Conditions"
                  tableRef={this.tableConditionsRef}
                  columns={this.state.conditionsColumns}
                  data={this.state.data.conditions}
                  options={{
                    paging: false,
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
                        if (this.vefiryCondition(newData)) {
                          let newState = this.state;
                          newState.data.conditions.push(newData);
                          this.setState(newState, () => resolve());
                        } else
                          reject();
                      }),
                    onRowUpdate: (newData, oldData) =>
                      new Promise((resolve, reject) => {
                        if (this.vefiryCondition(newData)) {
                          let newState = this.state;
                          const index = newState.data.conditions.indexOf(oldData);
                          newState.data.conditions[index] = newData;
                          this.setState(newState, () => resolve());
                        } else
                          reject();
                      }),
                    onRowDelete: oldData =>
                      new Promise((resolve, reject) => {
                        let newState = this.state;
                        const index = newState.data.conditions.indexOf(oldData);
                        newState.data.conditions.splice(index, 1);
                        this.setState(newState, () => resolve());
                      }),
                  }}
                />
              ) : (
                  <div>
                    <div style={{ marginLeft: '30px', marginTop: '20px' }}>
                      <FormControl component="fieldset">
                        <RadioGroup value={this.state.data.actions.type} onChange={this.handleActionTypeChange} >
                          <FormControlLabel
                            value="remove"
                            control={<Radio color="primary" />}
                            label="Remove data"
                          />
                          <FormControlLabel
                            value="modify"
                            control={<Radio color="primary" />}
                            label="Modify data"
                          />
                        </RadioGroup>
                        {this.state.data.actions.type === 'modify' ? (
                          <div>
                            <FormGroup row>
                              <FormControlLabel
                                label="Set tag"
                                // style={{ minWidth: 120 }}
                                control={<Checkbox color="primary" checked={this.state.data.actions.set_tag} onChange={this.handleActionSetTag} />}
                              />
                              {this.state.data.actions.set_tag === true ? (
                                <FormControlLabel
                                  labelPlacement="start"
                                  // style={{ minWidth: 150 }}
                                  control={
                                    <Select
                                      id="tag-select"
                                      // style={{ minWidth: 150 }}
                                      onChange={this.handleTagChange}
                                      value={this.state.data.actions.tag_id ? this.state.data.actions.tag_id : this.tags[0].tag_id}
                                    >
                                      {this.tags.map(tag => (
                                        <MenuItem key={tag.tag_id} value={tag.tag_id}>
                                          {tag.tag_name}
                                        </MenuItem>
                                      ))}
                                    </Select>
                                  }
                                />) : (<div />)}
                            </FormGroup>
                            <FormGroup row>
                              <FormControlLabel
                                label="Set lock"
                                // style={{ minWidth: 120 }}
                                control={<Checkbox color="primary" checked={this.state.data.actions.set_lock} onChange={this.handleActionSetLock} />}
                              />
                              {this.state.data.actions.set_lock === true ? (
                                <RadioGroup row value={this.state.data.actions.lock} onChange={this.handleActionLockChange} >
                                  <FormControlLabel
                                    value={true}
                                    control={<Radio color="primary" />}
                                    label="Locked"
                                  />
                                  <FormControlLabel
                                    value={false}
                                    control={<Radio color="primary" />}
                                    label="Unlocked"
                                  />
                                </RadioGroup>) : (<div />)}
                            </FormGroup>
                          </div>
                        ) : (<div />)}
                      </FormControl>
                    </div>
                    {this.state.data.actions.type === 'modify' ? (
                      <div>
                        <MaterialTable
                          // icons={tableIcons}
                          title="Modify properties"
                          tableRef={this.tableActionsRef}
                          columns={this.state.propertiesColumns}
                          data={this.state.data.actions.properties}
                          options={{
                            paging: false,
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
                                let newState = this.state;

                                if (newData.action === 'remove')
                                  delete newData['type'];

                                if (!(newData.path != null && newData.path.startsWith('$.') && newData.path.length > 2)) {
                                  window.alert("Incorrect property path!\n\nExample paths:\n $.param\n $.parent.param\n $.array[3]");
                                  reject();
                                  return;
                                }

                                newState.data.actions.properties.push(newData);
                                this.setState(newState, () => resolve());
                              }),
                            onRowUpdate: (newData, oldData) =>
                              new Promise((resolve, reject) => {
                                let newState = this.state;
                                const index = newState.data.actions.properties.indexOf(oldData);

                                if (newData.action === 'remove')
                                  delete newData['type'];

                                if (!(newData.path != null && newData.path.startsWith('$.') && newData.path.length > 2)) {
                                  window.alert("Incorrect property path!\n\nExample paths:\n $.param\n $.parent.param\n $.array[3]");
                                  reject();
                                  return;
                                }

                                newState.data.actions.properties[index] = newData;
                                this.setState(newState, () => resolve());
                              }),
                            onRowDelete: oldData =>
                              new Promise((resolve, reject) => {
                                let newState = this.state;
                                const index = newState.data.actions.properties.indexOf(oldData);
                                newState.data.actions.properties.splice(index, 1);
                                this.setState(newState, () => resolve());
                              }),
                          }}
                        />
                      </div>) : (<div />)}
                  </div>
                )}
            </div>
          </MuiDialogContent>
          <DialogActions>
            {/* <IconButton
              id="firstPageButton"
              aria-label="First Page"
              disabled={this.state.activeStep === 0}
              onClick={this.handleFirstPage}
            >
              <FirstPage />
            </IconButton> */}
            <IconButton
              id="previousPageButton"
              aria-label="Previous Page"
              disabled={this.state.activeStep === 0}
              onClick={this.handlePreviousPage}
            >
              <ChevronLeft />
            </IconButton>
            <IconButton
              id="nextPageButton"
              aria-label="Next Page"
              disabled={this.state.activeStep >= this.steps.length - 1}
              onClick={this.handleNextPage}
            >
              <ChevronRight />
            </IconButton>
            {/* <IconButton
              id="lastPageButton"
              aria-label="Last Page"
              disabled={this.state.activeStep >= this.steps.length - 1}
              onClick={this.handleLastPage}
            >
              <LastPage />
            </IconButton> */}
          </DialogActions>
        </Dialog>
      </div>
    );
  }
}

TaskConfigurationEditorDialog.propTypes = {
  classes: PropTypes.object.isRequired,
};

export default withStyles(styles)(TaskConfigurationEditorDialog);