import React from 'react';
import Typography from '@material-ui/core/Typography';
import Link from '@material-ui/core/Link';
import Logo from '../../../images/logo.png';
import {version} from '../../../../package.json';


class InfoTab extends React.Component {

    render() {
        return (
            <div
                style={{
                    position: 'absolute', left: '50%', top: '200px',
                    transform: 'translate(-50%, -50%)'
                }}
            >
                <img src={Logo} alt=''/>
                <div
                    style={{
                        position: 'absolute', left: '42%', top: '100%',
                        transform: 'translate(-50%, -50%)'
                    }}
                >
                    <Typography color='textSecondary'>Version: <b>{version}</b></Typography>
                    <Link target='_blank' href='https://www.databucket.pl' color="textSecondary" >www.databucket.pl</Link>
                </div>
                <div
                    style={{
                        position: 'absolute', left: '72%', top: '100%',
                        transform: 'translate(-50%, -50%)'
                    }}
                >
                    <Link target='_blank' href='https://github.com/databucket/databucket-app' color="textSecondary" >Source code</Link><br />
                    <Link target='_blank' href='https://github.com/databucket/databucket-app/issues' color="textSecondary" >Report a bug</Link><br />
                    <Link target='_blank' href='https://github.com/databucket/databucket-app/blob/master/LICENSE' color="textSecondary" >Licence</Link>
                </div>
            </div>
        );
    }
}

export default InfoTab;