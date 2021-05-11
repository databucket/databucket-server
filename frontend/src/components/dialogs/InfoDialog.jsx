import React, {useState} from 'react';
import Dialog from '@material-ui/core/Dialog';
import Typography from '@material-ui/core/Typography';
import ListItemIcon from "@material-ui/core/ListItemIcon";
import InfoIcon from "@material-ui/icons/InfoOutlined";
import ListItemText from "@material-ui/core/ListItemText";
import ListItem from "@material-ui/core/ListItem";
import DatabucketLogo from "../../images/databucket-logo.png";
import SpringBootLogo from "../../images/spring-boot-logo.png";
import PostgresqlLogo from "../../images/postgresql-logo.png";
import ReactLogo from "../../images/react-js-logo.png";
import MaterialLogo from "../../images/material-ui-logo.png";
import GithubLogoDark from "../../images/github-logo-white.png";
import GithubLogoLight from "../../images/github-logo.png";
import DockerLogo from "../../images/docker-logo.png";
import TravisLogo from "../../images/travis-ci-logo.png";
import Link from "@material-ui/core/Link";
import {useTheme} from "@material-ui/core/styles";

export default function InfoDialog() {

    const theme = useTheme();
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
                <img src={DatabucketLogo} alt=''/>
                <div style={{margin: '20px'}}>
                    <Typography color='secondary'>Version: <b>3.0.0</b></Typography>
                    <Link target='_blank' href='https://www.databucket.pl' color="primary">www.databucket.pl</Link><br/>
                    <Link target='_blank' href='https://github.com/databucket/databucket-app' color="textSecondary">Source code</Link><br/>
                    <Link target='_blank' href='https://github.com/databucket/databucket-app/issues' color="textSecondary">Report a bug, propose a new feature, ask a question...</Link><br/>
                    <Link target='_blank' href='https://github.com/databucket/databucket-app/blob/master/LICENSE' color="textSecondary">Licence: Apache License 2.0</Link><br/>
                </div>
                <div style={{marginLeft: '20px', marginBottom: '5px'}}>
                    <img src={SpringBootLogo} alt='Spring Boot' width='45' style={{margin: '5px', marginRight: '13px'}}/>
                    <img src={PostgresqlLogo} alt='PostgreSQL' width='45' style={{marginLeft: '5px', margin: '0px'}}/>
                    <img src={ReactLogo} alt='React.js' width='75' style={{margin: '0px'}}/>
                    <img src={MaterialLogo} alt='Material UI' width='56' style={{margin: '0px'}}/>
                    <img src={theme.palette.type === 'light' ? GithubLogoLight : GithubLogoDark} alt='Github' width='70' style={{margin: '0px'}}/>
                    <img src={DockerLogo} alt='Docker' width='50' style={{marginLeft: '5px'}}/>
                    <img src={TravisLogo} alt='TravisCi' width='42' style={{marginLeft: '10px', marginBottom: '3px'}}/>
                </div>

            </Dialog>
        </div>
    );
}