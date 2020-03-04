import React from 'react';
import PropTypes from 'prop-types';
import { withStyles } from '@material-ui/core/styles';
import Dialog from '@material-ui/core/Dialog';
import Tooltip from '@material-ui/core/Tooltip';
import IconButton from '@material-ui/core/IconButton';
import MuiDialogTitle from '@material-ui/core/DialogTitle';
import MuiDialogContent from '@material-ui/core/DialogContent';
import CloseIcon from '@material-ui/icons/Close';
import Typography from '@material-ui/core/Typography';
import MoreHoriz from '@material-ui/icons/MoreHoriz';
import FormGroup from '@material-ui/core/FormGroup';
import Radio from '@material-ui/core/Radio';
import RadioGroup from '@material-ui/core/RadioGroup';
import FormControlLabel from '@material-ui/core/FormControlLabel';
import { MuiPickersUtilsProvider, KeyboardDateTimePicker } from "@material-ui/pickers";
import DateFnsUtils from "@date-io/date-fns";
import Select from '@material-ui/core/Select';
import MenuItem from '@material-ui/core/MenuItem';
import Checkbox from '@material-ui/core/Checkbox';
import TextField from '@material-ui/core/TextField';
import FormControl from '@material-ui/core/FormControl';
import InputLabel from '@material-ui/core/InputLabel';

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
  },
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

const styles = theme => ({
  root: {
    width: '100%'
  },
  dialogPaper: {
    minHeight: '50vh',
  },
  content: {
    padding: theme.spacing(0)
  }
});

class EventScheduleEditorDialog extends React.Component {

  constructor(props) {
    super(props);
    this.tableRef = React.createRef();
    this.state = {
      schedule: null,
      open: false,
      initiated: false,
    };
  }

  static getDerivedStateFromProps(props, state) {
    console.log(1);
    let newState = state;
    if (state.open === true && !state.initiated) {
      console.log(2);
      let startDate = new Date();
      startDate.setHours(startDate.getHours() + 1);
      let endDate = new Date();
      endDate.setMonth(endDate.getMonth() + 1);
      console.log(3);
      if (props.rowData.schedule == null)
        newState.schedule = { periodically: false, interval: { amount: 1, unit: 3 }, starts: startDate, enable_ends: false, ends: endDate };
      else
        newState.schedule = props.rowData.schedule;

      console.log(4);
      newState.initiated = true;
      return newState;
    } else {
      console.log(5);
      return newState;
    }
  }

  handleClickOpen = () => {
    this.setState({
      open: true,
    });
  };

  handleClose = () => {
    this.props.onChange(this.state.schedule);
    this.setState({ open: false, initiated: false });
  };

  handlePeriodicallyChange = (event) => {
    let newState = this.state;
    newState.schedule.periodically = (event.target.value === 'true');
    this.setState(newState);
  }

  handleEnableEnds = (event) => {
    let newState = this.state;
    newState.schedule.enable_ends = event.target.checked;
    this.setState(newState);
  }

  handleChangedStarts = (date) => {
    let newState = this.state;
    date.setSeconds(0);
    date.setMilliseconds(0);
    newState.schedule.starts = date;
    this.setState(newState);
  }

  handleChangedEnds = (date) => {
    let newState = this.state;
    date.setSeconds(0);
    date.setMilliseconds(0);
    newState.schedule.ends = date;
    this.setState(newState);
  }

  handleChangedIntervalAmount = (event) => {
    let newState = this.state;
    let amount = event.target.value.replace(/[^0-9]/g, '');
    if (amount.length > 0)
      newState.schedule.interval.amount = parseInt(amount);
    else
      newState.schedule.interval.amount = '';
    this.setState(newState);
  }

  handleChangedIntervalUnit = (event) => {
    let newState = this.state;
    newState.schedule.interval.unit = event.target.value;
    this.setState(newState);
  }

  render() {
    const { classes } = this.props;
    console.log('render');
    return (
      <div>
        <Tooltip title='Schedule configuration'>
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
          classes={{ paper: classes.dialogPaper }}
          open={this.state.open}
          fullWidth={true}
          maxWidth='sm' //'xs' | 'sm' | 'md' | 'lg' | 'xl' | false
        >
          <DialogTitle id="customized-dialog-title" onClose={this.handleClose}>
            Event schedule configuration
          </DialogTitle>
          <DialogContent dividers>
            <div style={{ marginLeft: '30px', marginRight: '30px', marginTop: '20px', marginBottom: '50px' }}>
              <MuiPickersUtilsProvider utils={DateFnsUtils}>
                {this.state.schedule != null ? (
                  <div>
                    <FormGroup>
                      <RadioGroup row value={this.state.schedule.periodically} onChange={this.handlePeriodicallyChange} >
                        <FormControlLabel
                          value={false}
                          control={<Radio color="primary" />}
                          label="One time"
                        />
                        <FormControlLabel
                          value={true}
                          control={<Radio color="primary" />}
                          label="Recurring"
                        />
                      </RadioGroup>
                    </FormGroup>
                    {this.state.schedule.periodically === false ? (
                      <div style={{ marginTop: '20px' }}>
                        <KeyboardDateTimePicker
                          variant='inline'
                          ampm={false}
                          autoOk={true}
                          disablePast
                          value={this.state.schedule.starts}
                          onChange={this.handleChangedStarts}
                          format="yyyy/MM/dd HH:mm"
                          label="At"
                          style={{ maxWidth: 180 }}
                        />
                      </div>
                    ) : (
                        <div>
                          <div style={{ marginTop: '20px' }}>
                            <KeyboardDateTimePicker
                              variant='inline'
                              ampm={false}
                              autoOk={true}
                              disablePast
                              value={this.state.schedule.starts}
                              onChange={this.handleChangedStarts}
                              format="yyyy/MM/dd HH:mm"
                              label="Starts"
                              style={{ maxWidth: 180 }}
                            />
                          </div>
                          <FormGroup row style={{ marginTop: '20px' }}>
                            <TextField
                              id="interval-amount"
                              label="Interval"
                              value={this.state.schedule.interval.amount}
                              // type="number"
                              onChange={this.handleChangedIntervalAmount}
                              style={{ maxWidth: 90 }}
                              // InputLabelProps={{
                              //   shrink: true,
                              // }}
                            />
                            <FormControl style={{ marginLeft: 20 }}>
                              <InputLabel htmlFor="interval-unit-label">Unit</InputLabel>
                              <Select
                                id="inteval-unit"
                                style={{ minWidth: 70 }}
                                value={this.state.schedule.interval.unit}
                                onChange={this.handleChangedIntervalUnit}
                              >
                                <MenuItem value={1}>minute</MenuItem>
                                <MenuItem value={2}>hour</MenuItem>
                                <MenuItem value={3}>day</MenuItem>
                                <MenuItem value={4}>week</MenuItem>
                                <MenuItem value={5}>month</MenuItem>
                              </Select>
                            </FormControl>
                          </FormGroup>
                          <FormControlLabel
                            label="Time limited"
                            style={{ marginTop: '20px' }}
                            control={<Checkbox color="primary" checked={this.state.schedule.enable_ends} onChange={this.handleEnableEnds} />}
                          />
                          {this.state.schedule.enable_ends === true ? (
                            <div>
                              <KeyboardDateTimePicker
                                variant='inline'
                                ampm={false}
                                autoOk={true}
                                disablePast
                                value={this.state.schedule.ends}
                                onChange={this.handleChangedEnds}
                                format="yyyy/MM/dd HH:mm"
                                label="Ends"
                                style={{ maxWidth: 180 }}
                              />
                            </div>) : (<div />)}
                        </div>
                      )}
                  </div>
                ) : (<div />)}
              </MuiPickersUtilsProvider>
            </div>
          </DialogContent>
        </Dialog>
      </div>
    );
  }
}

EventScheduleEditorDialog.propTypes = {
  classes: PropTypes.object.isRequired,
};

export default withStyles(styles)(EventScheduleEditorDialog);
