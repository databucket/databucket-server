import React, { forwardRef } from 'react';
import { withStyles } from '@material-ui/core/styles';
// import Divider from '@material-ui/core/Divider';
import PropTypes from 'prop-types';
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
import Stepper from '@material-ui/core/Stepper';
import Step from '@material-ui/core/Step';
import StepLabel from '@material-ui/core/StepLabel';
import Button from '@material-ui/core/Button';
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
import InputLabel from '@material-ui/core/InputLabel';
import FormHelperText from '@material-ui/core/FormHelperText';
import DoneIcon from '@material-ui/icons/Done';
import ErrorIcon from '@material-ui/icons/ErrorOutline';
import CircularProgress from '@material-ui/core/CircularProgress';

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
    minHeight: '80vh',
  },
  content: {
    padding: theme.spacing(0)
  },
  instructions: {
    marginTop: theme.spacing(1),
    marginBottom: theme.spacing(1),
  }
});

const sourceLookup = {
  'const': 'Const',
  'field': 'Field',
  'property': 'Property',
  'function': 'Function'
  // 'string': 'String',
  // 'numeric': 'Numeric',
  // 'boolean': 'Boolean',
  // 'date': 'Date',
  // 'time': 'Time',
  // 'datetime': 'Datetime',
  // 'string-array': 'String array',
  // 'numeric-array': 'Numeric array',
  // 'null': 'Null'  
};

class TaskExecutionEditorDialog extends React.Component {

  constructor(props) {
    super(props);
    this.steps = ['Load configuration', 'Customize conditions', 'Customize actions', 'Execute'];
    this.tableConditionsRef = React.createRef();
    this.tableActionsRef = React.createRef();
    this.state = {
      activeStep: 0,
      data: null,
      open: false,
      tags: null,
      tasks: null,
      selectedTask: null,
      apply: null, // number of dataRow that meet the conditions
      taskLoaded: false,
      processing: false, // true for time when task is executing
      executionResult: null, // response message from execution
      conditionsColumns: [
        { title: 'Left source', field: 'left_source', lookup: sourceLookup },
        { title: "Left value", field: 'left_value', emptyValue: 'null' },
        {
          title: 'Operator', field: 'operator',
          lookup: { '=': '=', '>': '>', '>=': '>=', '<': '<', '<=': '<=', '<>': '<>', 'in': 'in', 'not in': 'not in', 'is': 'is', 'is not': 'is not', 'like': 'like', 'not like': 'not like' }
        },
        { title: 'Right source', field: 'right_source', lookup: sourceLookup },
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

  // onDialogOpen(nextProps) {
  //   let bucket_id = nextProps.bucket.bucket_id;
  //   let class_id = nextProps.bucket.class_id;

  //   this.tags = this.props.tags.filter(tag => (tag.bucket_id === bucket_id || tag.bucket_id === null) || (tag.class_id === class_id || tag.class_id === null));
  // }

  static getDerivedStateFromProps(props, state) {
    if (props.open === true && state.open === false) {
      let initialData = {
        conditions: [],
        actions: { type: 'none', set_tag: false, tag_id: 0, set_lock: false, lock: false, properties: [] }
      };

      let bucket_id = props.bucket.bucket_id;
      let class_id = props.bucket.class_id;

      let filteredTags = props.tags.filter(tag => (tag.bucket_id === bucket_id || tag.bucket_id === null) && (tag.class_id === class_id || tag.class_id === null));

      let filteredTasks = props.tasks.filter(task => (task.bucket_id === bucket_id || task.bucket_id === null) && (task.class_id === class_id || task.class_id === null));

      let selectedTask = null;
      let extendedFilteredTasks = [];
      if (filteredTasks.length > 0) {
        const emptyTask = { task_id: 0, task_name: '---' }
        extendedFilteredTasks.push(emptyTask);
        extendedFilteredTasks = extendedFilteredTasks.concat(filteredTasks)
        selectedTask = emptyTask;
      }

      return {
        open: props.open,
        bucket: props.bucket,
        tags: filteredTags,
        tasks: extendedFilteredTasks,
        selectedTask: selectedTask,
        activeStep: 0,
        executionResult: null,
        data: initialData,
        apply: null
      };
    } else
      return { open: props.open };
  }

  handleClose = () => {
    this.props.onClose();
  };

  handleFirstPage = () => {
    this.setState({ activeStep: 0 });
  };

  handlePreviousPage = () => {
    this.setState({ activeStep: this.state.activeStep - 1 });
  };

  handleNextPage = () => {
    this.setState({ activeStep: this.state.activeStep + 1 });
  };

  handleLastPage = () => {
    this.setState({ activeStep: this.steps.length - 1 });
  };

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

  handleTaskChange = (event) => {
    let newSelectedTask = this.state.tasks.filter(task => task.task_id === event.target.value)[0];

    let newState = this.state;
    newState.selectedTask = newSelectedTask;
    this.setState(newState);
  }

  getApplyDataRowCount(conditions) {
    let payload = {};
    if (conditions != null && conditions.length > 0) {
      payload.conditions = conditions;

      // set user name for @currentUser
      for (const condition of payload.conditions) {
        if (typeof condition.left_value === 'string' || condition.left_value instanceof String)
          if (condition.left_value.includes("@currentUser"))
            condition.left_value = condition.left_value.replace("@currentUser", window.USER);

        if (typeof condition.right_value === 'string' || condition.right_value instanceof String)
          if (condition.right_value.includes("@currentUser"))
            condition.right_value = condition.right_value.replace("@currentUser", window.USER);
      }
    }

    new Promise((resolve, reject) => {
      setTimeout(() => {
        let url = window.API + '/bucket/' + this.state.bucket.bucket_name + '/data/custom?limit=0';
        fetch(url, {
          method: 'POST',
          body: JSON.stringify(payload),
          headers: {
            'Content-Type': 'application/json'
          }
        }).then(response => response.json())
          .then(result => {
            this.setState({ apply: result.total });
            resolve();
          });
      }, 100);
    });
  }

  handleLoadTask = (event) => {
    let newState = this.state;
    let configuration = JSON.parse(JSON.stringify(this.state.selectedTask.configuration));

    if (configuration == null)
      configuration = { conditions: [], actions: null }

    if (configuration.conditions == null)
      configuration.conditions = [];

    if (configuration.actions == null)
      configuration.actions = {};

    newState.data = configuration;
    this.getApplyDataRowCount(configuration.conditions);
    newState.taskLoaded = true;
    this.setState(newState);

    setTimeout(() => {
      this.setState({ taskLoaded: false });
    }, 3000);
  }

  isConfigurationValid = (event) => {
    let actions = this.state.data.actions;
    let result = true;

    if (actions.type == null || actions.type === 'none')
      result = false;

    if (actions.type === 'modify') {
      if (actions.set_tag === false && actions.set_lock === false && actions.properties.length === 0)
        result = false;
    }

    return result;
  }

  handleExecute = (event) => {
    let conditions = this.state.data.conditions;
    let actions = this.state.data.actions;
    let payload = {}

    if (conditions.length > 0) {
      payload.conditions = conditions;

      // set user name for @currentUser
      for (const condition of payload.conditions) {
        if (typeof condition.left_value === 'string' || condition.left_value instanceof String)
          if (condition.left_value.includes("@currentUser"))
            condition.left_value = condition.left_value.replace("@currentUser", window.USER);

        if (typeof condition.right_value === 'string' || condition.right_value instanceof String)
          if (condition.right_value.includes("@currentUser"))
            condition.right_value = condition.right_value.replace("@currentUser", window.USER);
      }
    }

    if (actions.type === 'remove') {
      this.removeData(payload);
    } else if (actions.type === 'modify') {
      //prepare modify payload
      // actions: { type: 'none', set_tag: false, tag_id: 0, set_lock: false, lock: false, properties: [] }
      if (actions.set_lock === true)
        payload['locked'] = actions.lock;

      if (actions.set_tag === true)
        payload['tag_id'] = actions.tag_id;

      if (actions.properties.length > 0) {
        let propertiesToRemoveArray = [];
        let propertiesToModifyMap = {};

        for (let index = 0; index < actions.properties.length; index++) {
          const element = actions.properties[index];

          if (element.action === 'remove')
            propertiesToRemoveArray.push(element.path);
          else if (element.action === 'set') {
            if (element.type === 'numeric')
              propertiesToModifyMap[element.path] = parseFloat(element.value);
            else if (element.type === 'boolean')
              propertiesToModifyMap[element.path] = (element.value.toUpperCase() === 'TRUE');
            else if (element.type === 'null')
              propertiesToModifyMap[element.path] = null;
            else
              propertiesToModifyMap[element.path] = element.value;
          }
        }

        if (Object.keys(propertiesToModifyMap).length > 0)
          payload['update_properties'] = propertiesToModifyMap;

        if (propertiesToRemoveArray.length > 0)
          payload['remove_properties'] = propertiesToRemoveArray;
      }

      this.modifyData(payload);
    }

  }

  removeData(payload) {
    new Promise((resolve, reject) => {
      this.setState({ processing: true, executionResult: null });
      setTimeout(() => {
        let url = window.API + '/bucket/' + this.state.bucket.bucket_name + '/data/custom';
        fetch(url, {
          method: 'DELETE',
          body: JSON.stringify(payload),
          headers: {
            'Content-Type': 'application/json'
          }
        }).then(response => response.json())
          .then(result => {
            this.setState({ processing: false, executionResult: result });
            resolve();
          });
      }, 300);
    });
  }

  modifyData(payload) {
    new Promise((resolve, reject) => {
      this.setState({ processing: true, executionResult: null });
      setTimeout(() => {
        let url = window.API + '/bucket/' + this.state.bucket.bucket_name + '/data/custom?userName=' + window.USER;
        fetch(url, {
          method: 'PUT',
          body: JSON.stringify(payload),
          headers: {
            'Content-Type': 'application/json'
          }
        }).then(response => response.json())
          .then(result => {
            this.setState({ processing: false, executionResult: result });
            resolve();
          });
      }, 300);
    });
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

    if (this.state != null && this.state.data != null && this.state.apply == null) {
      new Promise((resolve, reject) => {
        this.getApplyDataRowCount([]);
      }, 2000);
    }

    return (
      <Dialog
        onClose={this.handleClose}
        aria-labelledby="customized-dialog-title"
        open={this.state.open}
        classes={{ paper: classes.dialogPaper }}
        fullWidth={true}
        maxWidth='lg' //'xs' | 'sm' | 'md' | 'lg' | 'xl' | false
      >
        <DialogTitle id="dialog-title" onClose={this.handleClose}>
          {this.state.apply != null ? 'Remove/modify data (applies to ' + this.state.apply + ' data items)' : 'Remove/modify data'}
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
          {(() => {
            switch (this.state.activeStep) {
              case 0: return (
                <div style={{ marginLeft: '30px', marginTop: '20px' }}>
                  {this.state.tasks != null && this.state.tasks.length > 0 ? (
                    <div>
                      <FormControl disabled={this.state.tasks.length === 0} style={{ marginBottom: 30 }}>
                        <InputLabel htmlFor="task-select-label">Select task</InputLabel>
                        <Select
                          value={this.state.selectedTask.task_id}
                          onChange={this.handleTaskChange}
                          style={{ width: '200px' }}
                        >
                          {this.state.tasks.map(task => (
                            <MenuItem key={task.task_id} value={task.task_id}>
                              {task.task_name}
                            </MenuItem>
                          ))}
                        </Select>
                        <FormHelperText>{this.state.selectedTask.description}</FormHelperText>
                      </FormControl>
                      <FormGroup row>
                        <Button
                          variant="contained"
                          color="primary"
                          size="small"
                          onClick={this.handleLoadTask}
                          disabled={this.state.tasks.length === 0 || this.state.selectedTask.task_id === 0}>
                          Load task configuration
                        </Button>
                        {this.state.taskLoaded ? (<DoneIcon color="primary" style={{ marginLeft: 20 }} />) : (<div />)}
                      </FormGroup>
                    </div>) : (<div><Typography>There is no defined tasks for this bucket.</Typography></div>)}
                </div>
              );
              case 1: return (
                <div>
                  <MaterialTable

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
                          let newState = this.state;
                          if (this.vefiryCondition(newData)) {
                            newState.data.conditions.push(newData);
                            this.setState(newState, () => resolve());   
                            setTimeout(() => {
                              this.getApplyDataRowCount(newState.data.conditions)
                            }, 500);                                                    
                          } else {
                            reject();
                          }
                        }),
                      onRowUpdate: (newData, oldData) =>
                        new Promise((resolve, reject) => {
                          let newState = this.state;
                          if (this.vefiryCondition(newData)) {
                            const index = newState.data.conditions.indexOf(oldData);
                            newState.data.conditions[index] = newData;
                            this.setState(newState, () => resolve());  
                            setTimeout(() => {
                              this.getApplyDataRowCount(newState.data.conditions)
                            }, 500);                                                
                          } else {
                            reject();
                          }
                        }),
                      onRowDelete: oldData =>
                        new Promise((resolve, reject) => {
                          let newState = this.state;
                          const index = newState.data.conditions.indexOf(oldData);
                          newState.data.conditions.splice(index, 1);
                          this.setState(newState, () => resolve());     
                          setTimeout(() => {
                            this.getApplyDataRowCount(newState.data.conditions)
                          }, 500);                     
                        }),
                    }}
                  />
                </div>
              );
              case 2: return (
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
                                    value={this.state.data.actions.tag_id ? this.state.data.actions.tag_id : this.state.tags[0].tag_id}
                                  >
                                    {this.state.tags.map(tag => (
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
              );
              case 3: return (
                <div style={{ marginLeft: '30px', marginTop: '20px' }}>
                  <FormGroup row>
                    <Button
                      variant="contained"
                      color="primary"
                      size="small"
                      style={{ marginRight: 30, marginTop: 5, marginBottom: 20 }}
                      onClick={this.handleExecute}
                      disabled={!this.isConfigurationValid() || this.state.processing}>
                      Execute
                    </Button>
                    {this.state.processing ? (<CircularProgress />) : (<div />)}
                  </FormGroup>
                  {this.state.executionResult ?
                    this.state.executionResult.status === 'OK' ?
                      (<FormGroup row><DoneIcon color="primary" /><Typography style={{ marginLeft: 10 }}>{this.state.executionResult.message}</Typography></FormGroup>) :
                      (<FormGroup row><ErrorIcon color="secondary" /><Typography style={{ marginLeft: 10 }}>{this.state.executionResult.message}</Typography></FormGroup>) :
                    (<div />)}
                </div>
              );
              default: return (<div />);
            }
          })()}
        </MuiDialogContent>
        <DialogActions>
          <IconButton
            id="firstPageButton"
            aria-label="First Page"
            disabled={this.state.activeStep === 0}
            onClick={this.handleFirstPage}
          >
            <FirstPage />
          </IconButton>
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
          <IconButton
            id="lastPageButton"
            aria-label="Last Page"
            disabled={this.state.activeStep >= this.steps.length - 1}
            onClick={this.handleLastPage}
          >
            <LastPage />
          </IconButton>
        </DialogActions>
      </Dialog>
    );
  }
}

TaskExecutionEditorDialog.propTypes = {
  classes: PropTypes.object.isRequired,
};

export default withStyles(styles)(TaskExecutionEditorDialog);