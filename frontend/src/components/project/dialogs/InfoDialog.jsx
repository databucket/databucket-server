import React, {useState} from 'react';
import Dialog from '@material-ui/core/Dialog';
import Typography from '@material-ui/core/Typography';
import ListItemIcon from "@material-ui/core/ListItemIcon";
import InfoIcon from "@material-ui/icons/InfoOutlined";
import ListItemText from "@material-ui/core/ListItemText";
import ListItem from "@material-ui/core/ListItem";
import Logo from "../../../images/logo.png";
import {version} from "../../../../package.json";
import Link from "@material-ui/core/Link";

export default function InfoDialog() {

    const [open, setOpen] = useState(false);

    const handleClickOpen = () => {
        setOpen(true);
    };
    const handleClose = () => {
        setOpen(false);
    };

    return (
        <div>
            <ListItem button onClick={handleClickOpen}>
                <ListItemIcon><InfoIcon/></ListItemIcon>
                <ListItemText primary={'Info'}/>
            </ListItem>
            <Dialog
                onClose={handleClose}
                aria-labelledby="customized-dialog-title"
                open={open}
                fullWidth={true}
                // maxWidth={props.maxWidth}
            >
                <img src={Logo} alt=''/>
                <div style={{margin: '20px'}}>
                    <Typography color='secondary'>Version: <b>{version}</b></Typography>
                    <Link target='_blank' href='https://www.databucket.pl' color="primary">www.databucket.pl</Link><br/>
                    <Link target='_blank' href='https://github.com/databucket/databucket-app' color="textSecondary">Source code</Link><br/>
                    <Link target='_blank' href='https://github.com/databucket/databucket-app/issues' color="textSecondary">Report a bug</Link><br/>
                    <Link target='_blank' href='https://github.com/databucket/databucket-app/blob/master/LICENSE' color="textSecondary">Licence</Link><br/>
                </div>
            </Dialog>
        </div>
    );
}