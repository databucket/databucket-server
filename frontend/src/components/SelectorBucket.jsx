import React from 'react';
import List from '@material-ui/core/List';
import ListItem from '@material-ui/core/ListItem';
import ListItemIcon from '@material-ui/core/ListItemIcon';
import ListItemText from '@material-ui/core/ListItemText';
import DynamicIcon from './utils/DynamicIcon';

export default class SelectorBucket extends React.Component {

    constructor(props) {
        super(props);

        this.state = {
            buckets: null, 
            selectedBucketId: -1
        };
    }

    static getDerivedStateFromProps(props, state) {
        let selected = -1;
        if (props.selectedBucket != null)
            selected = props.selectedBucket.bucket_id;
        return { buckets: props.buckets,  selectedBucketId: selected }; 
    }

    onClick = (selectedBucket) => {
        if (selectedBucket.bucket_id !== this.state.selectedBucketId)
            this.props.onBucketSelected(selectedBucket);      
    }

    render() {
        return (
            <div>
                {this.state.buckets !== null ? (
                    <List>
                        {this.state.buckets.map((bucket) => (
                            <ListItem button selected={bucket.bucket_id === this.state.selectedBucketId} key={bucket.bucket_name} onClick={() => this.onClick(bucket)}>
                                <ListItemIcon><DynamicIcon iconName={bucket.icon_name} /></ListItemIcon>
                                <ListItemText primary={bucket.bucket_name} />
                            </ListItem>
                        ))}
                    </List>
                ) : (<br />)}
            </div>
        );
    }
}