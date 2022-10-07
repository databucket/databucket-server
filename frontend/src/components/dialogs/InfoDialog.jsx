import React, {useState} from 'react';
import Dialog from '@material-ui/core/Dialog';
import Typography from '@material-ui/core/Typography';
import ListItemIcon from "@material-ui/core/ListItemIcon";
import ListItemText from "@material-ui/core/ListItemText";
import ListItem from "@material-ui/core/ListItem";
import DatabucketLogo from "../../images/databucket-logo.png";
import SpringBootLogo from "../../images/spring-boot-logo.png";
import PostgresqlLogo from "../../images/postgresql-logo.png";
import ReactLogo from "../../images/react-js-logo.png";
import MaterialLogo from "../../images/material-ui-logo.png";
import MaterialTableLogo from "../../images/material-table-logo.png";
import GithubLogoDark from "../../images/github-logo-white.png";
import GithubLogoLight from "../../images/github-logo.png";
import JsonLogicLight from "../../images/jsonlogic-black.png";
import JsonLogicDark from "../../images/jsonlogic-white.png";
import ReactDiffViewerLogo from "../../images/react-diff-viewer-logo.png";
import DockerLogo from "../../images/docker-logo.png";
import TravisLogo from "../../images/travis-ci-logo.png";
import SwaggerLogo from "../../images/swagger-logo.png";
import Link from "@material-ui/core/Link";
import {useTheme} from "@material-ui/core/styles";
import {Tooltip, withStyles} from "@material-ui/core";
import {getButtonColor} from "../../utils/MaterialTableHelper";


const styles = {
    tooltip: {
        backgroundColor: "silver"
    }
};

const CustomTooltip = withStyles(styles)(Tooltip);

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
                <ListItemIcon>
                    <svg viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg" width="24" height="24" fill={getButtonColor(theme)}>
                        <path d="M11 7h2v2h-2zm0 4h2v6h-2zm1-9C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm0 18c-4.41 0-8-3.59-8-8s3.59-8 8-8 8 3.59 8 8-3.59 8-8 8z"/>
                    </svg>
                </ListItemIcon>
                <ListItemText primary={'Info'}/>
            </ListItem>
            <Dialog
                onClose={handleClose}
                aria-labelledby="customized-dialog-title"
                open={open}
                fullWidth={true}
            >
                <img style={{marginLeft: '100px', marginTop: '20px'}} src={DatabucketLogo} alt='' width='399' height='65'/>
                <div style={{margin: '20px'}}>
                    <Typography color='secondary'>Version: <b>3.3.2</b></Typography>
                    <Link target='_blank' href='https://www.databucket.pl' color="primary">www.databucket.pl</Link><br/>
                    <Link target='_blank' href='https://github.com/databucket/databucket-server' color="textSecondary">Source code</Link><br/>
                    <Link target='_blank' href='https://github.com/databucket/databucket-server/wiki' color="textSecondary">Documentation</Link><br/>
                    <Link target='_blank' href='https://github.com/databucket/databucket-server/issues/new' color="textSecondary">Report a bug, propose a new feature, ask a question...</Link><br/>
                    <Link target='_blank' href='https://databucketworkspace.slack.com/archives/C024LBQ4PQU' color="textSecondary">Let's talk on Slack</Link><br/>
                    <Link target='_blank' href='https://github.com/databucket/databucket-server/blob/master/LICENSE' color="textSecondary">Licence: MIT License</Link><br/>
                </div>

                <div style={{marginLeft: '25px', marginBottom: '10px'}}>
                    <CustomTooltip
                        interactive
                        title={<Link rel="noopener noreferrer" href="https://spring.io/projects/spring-boot" target="_blank">Spring Boot</Link>}
                    >
                        <img src={SpringBootLogo} alt='Spring Boot' width='37' style={{marginLeft: '12px', marginTop: '4px'}}/>
                    </CustomTooltip>

                    <CustomTooltip
                        interactive
                        title={<Link rel="noopener noreferrer" href="https://www.postgresql.org/" target="_blank">PostgreSQL</Link>}
                    >
                        <img src={PostgresqlLogo} alt='PostgreSQL' width='33' style={{marginLeft: '16px', marginTop: '4px'}}/>
                    </CustomTooltip>

                    <CustomTooltip
                        interactive
                        title={<Link rel="noopener noreferrer" href="https://reactjs.org/" target="_blank">React</Link>}
                    >
                        <img src={ReactLogo} alt='React' width='38' style={{marginLeft: '12px', marginTop: '11px'}}/>
                    </CustomTooltip>

                    <CustomTooltip
                        interactive
                        title={<Link rel="noopener noreferrer" href="https://material-ui.com/" target="_blank">Material-UI</Link>}
                    >
                        <img src={MaterialLogo} alt='Material-UI' width='35' style={{marginLeft: '10px', marginTop: '6px'}}/>
                    </CustomTooltip>

                    <CustomTooltip
                        interactive
                        title={<Link rel="noopener noreferrer" href="https://material-table.com/#/" target="_blank">material-table</Link>}
                    >
                        <img src={MaterialTableLogo} alt='material-table' width='40' style={{marginLeft: '10px', marginTop: '12px'}}/>
                    </CustomTooltip>

                    <CustomTooltip
                        interactive
                        title={<Link rel="noopener noreferrer" href="https://jsonlogic.com/" target="_blank">JsonLogic</Link>}
                    >
                        <img src={theme.palette.type === 'light' ? JsonLogicLight : JsonLogicDark} alt='JsonLogic' width='38' style={{marginLeft: '10px', marginTop: '7px'}}/>
                    </CustomTooltip>

                    <CustomTooltip
                        interactive
                        title={<Link rel="noopener noreferrer" href="https://praneshravi.in/react-diff-viewer/" target="_blank">React Diff Viewer</Link>}
                    >
                        <img src={ReactDiffViewerLogo} alt='React Diff Viewer' width='33' style={{marginLeft: '15px', marginTop: '2px', marginBottom: '3px'}}/>
                    </CustomTooltip>

                    <CustomTooltip
                        interactive
                        title={<Link rel="noopener noreferrer" href="https://swagger.io/" target="_blank">Swagger</Link>}
                    >
                        <img src={SwaggerLogo} alt='Swagger' width='32' style={{marginLeft: '15px', marginTop: '0px', marginBottom: '3px'}}/>
                    </CustomTooltip>

                    <CustomTooltip
                        interactive
                        title={<Link rel="noopener noreferrer" href="https://github.com/" target="_blank">Github</Link>}
                    >
                        <img src={theme.palette.type === 'light' ? GithubLogoLight : GithubLogoDark} alt='Github' width='53' style={{marginLeft: '5px', marginTop: '3px', marginBottom: '1px'}}/>
                    </CustomTooltip>

                    <CustomTooltip
                        interactive
                        title={<Link rel="noopener noreferrer" href="https://www.docker.com/" target="_blank">Docker</Link>}
                    >
                        <img src={DockerLogo} alt='Docker' width='40' style={{marginLeft: '7px', marginTop: '5px'}}/>
                    </CustomTooltip>

                    <CustomTooltip
                        interactive
                        title={<Link rel="noopener noreferrer" href="https://www.travis-ci.com/" target="_blank">Travis CI</Link>}
                    >
                        <img src={TravisLogo} alt='Travis CI' width='32' style={{marginLeft: '10px', marginTop: '0px', marginBottom: '2px'}}/>
                    </CustomTooltip>
                </div>
            </Dialog>
        </div>
    );
}