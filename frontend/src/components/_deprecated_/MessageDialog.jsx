import React from 'react';
import { withStyles } from '@material-ui/core/styles';
import Dialog from '@material-ui/core/Dialog';
import MuiDialogTitle from '@material-ui/core/DialogTitle';
import MuiDialogContent from '@material-ui/core/DialogContent';
import IconButton from '@material-ui/core/IconButton';
import ErrorIcon from '@material-ui/icons/ErrorOutline';
import CloseIcon from '@material-ui/icons/Close';
import Typography from '@material-ui/core/Typography';
import Grid from '@material-ui/core/Grid';

const styles = theme => ({
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
    padding: theme.spacing(2),
  },
}))(MuiDialogContent);

class MessageDialog extends React.Component {

  constructor(props) {
    super(props);

    this.state = {
      initiated: false,
      open: false,
      title: null,
      message: null
    };
  }

  static getDerivedStateFromProps(props, state) {
    if (state.open === false && props.open === true && state.initiated === false) {
      return {
        initiated: true,
        open: props.open,
        title: props.title,
        message: props.message
      };
    } else {
      return {
        initiated: false,
        open: state.open,
        title: state.title,
        message: state.message
      };
    }
  }

  handleClickOpen = () => {
    this.setState({
      open: true,
    });
  };

  handleClose = () => {
    this.setState({ open: false });
    this.props.onClose();
  };

  render() {
    return (
      <Dialog
        onClose={this.handleClose}
        aria-labelledby="message-dialog"
        open={this.state.open}
        // fullWidth={true}
        // maxWidth='sm' //'xs' | 'sm' | 'md' | 'lg' | 'xl' | false
      >
        <DialogTitle id="message-dialog" onClose={this.handleClose}>
          {this.state.title != null ? this.state.title : ''}
        </DialogTitle>
        <DialogContent dividers>
          <Grid container spacing={3}>
            <Grid item>
              <ErrorIcon color="error" />
            </Grid>
            <Grid item>
              <Typography gutterBottom>
                {this.props.message != null ? this.state.message : ''}
              </Typography>
            </Grid>
          </Grid>
        </DialogContent>
      </Dialog>
    );
  }
}

export default MessageDialog;
