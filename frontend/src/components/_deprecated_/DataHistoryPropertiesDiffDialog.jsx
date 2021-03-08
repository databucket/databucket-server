import React from 'react'
import ReactDiffViewer from 'react-diff-viewer'
import { withStyles } from '@material-ui/core/styles';
import Dialog from '@material-ui/core/Dialog';
import Tooltip from '@material-ui/core/Tooltip';
import IconButton from '@material-ui/core/IconButton';
import MuiDialogTitle from '@material-ui/core/DialogTitle';
import MuiDialogContent from '@material-ui/core/DialogContent';
import CloseIcon from '@material-ui/icons/Close';
import Typography from '@material-ui/core/Typography';
import CompareIcon from '@material-ui/icons/YoutubeSearchedFor';

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

export default class DataHistoryPropertiesDiffDialog extends React.Component {

  constructor(props) {
    super(props);
    this.tableRef = React.createRef();
    this.state = {
      bucket: null,
      dataRowId: null,
      oldValue: '',
      newValue: '',
      history: null,
      selectedRow: null,
      open: false
    };
  }

  static getDerivedStateFromProps(props, state) {
    return {
      bucket: props.bucket,
      dataRowId: props.dataRowId,
      history: props.history,
      selectedRow: props.selectedRow
    };
  }

  handleClickOpen = () => {
    var oValue = '';
    var nValue = '';

    var previousId = this.getPreviousId(this.state.history, this.state.selectedRow);

    new Promise((resolve, reject) => {
      let url = window.API + '/bucket/' + this.state.bucket.bucket_name + '/data/' + this.state.dataRowId + '/history/properties/'
      if (previousId > 0)
        url += previousId + ',' + this.state.selectedRow.id;
      else
        url += this.state.selectedRow.id;

      fetch(url)
        .then(response => response.json())
        .then(result => {
          if (previousId > 0) {
            oValue = result.history.filter(d => (d.id === previousId))[0].properties;
            oValue = JSON.stringify(oValue, null, 2);
          }

          nValue = result.history.filter(d => (d.id === this.state.selectedRow.id))[0].properties;
          nValue = JSON.stringify(nValue, null, 2);
          // console.log(JSON.stringify(oValue, null, 2));
          // console.log(JSON.stringify(nValue, null, 2));

          this.setState({
            oldValue: oValue,
            newValue: nValue,
            openDataHistoryDialog: true,
          });

          resolve();
        });
    });

    this.setState({
      oldValue: oValue,
      newValue: nValue,
      open: true,
    });
  };

  getPreviousId(history, row) {
    var result = -1;
    for (var i = 0; i < history.length; i++) {
      var obj = history[i];
      if (obj.hasOwnProperty('properties') && obj.properties === true && obj.id !== row.id) {
        result = obj.id;
      }
      if (obj.id === row.id)
        return result;
    }
  }

  handleClose = () => {
    this.setState({ open: false });
  };

  render() {
    return (
      <div>
        <Tooltip title='Show changes'>
          <IconButton
            onClick={this.handleClickOpen}
            color="default"
            size="small"
          >
            <CompareIcon />
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
            Properties difference
          </DialogTitle>
          <DialogContent dividers>
            <ReactDiffViewer
              oldValue={this.state.oldValue}
              newValue={this.state.newValue}
              splitView={true}
              disableWordDiff={false}
            />
          </DialogContent>
        </Dialog>
      </div>
    );
  }
}
