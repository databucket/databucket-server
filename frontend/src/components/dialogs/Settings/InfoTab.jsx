import React from 'react';
import Typography from '@material-ui/core/Typography';
import Link from '@material-ui/core/Link';
import Logo from '../../../images/logo.png';

class InfoTab extends React.Component {

    constructor(props) {
        super(props);
        this.getVersion();
        this.state = {
            version: null
        };
    }

    getVersion() {
        fetch(window.API + '/version')
            .then(response => response.json())
            .then(responseJson => responseJson.version)
            .then(version => {
                this.setState({ version: version });
            })
    }

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
                        position: 'absolute', left: '80%', top: '92%',
                        transform: 'translate(-50%, -50%)'
                    }}
                >
                    <Typography color='textSecondary'>Version: {this.state.version}</Typography>
                    <Link target='_blank' href='https://www.databucket.pl' color="textSecondary" >www.databukcet.pl</Link>
                </div>
            </div>
        );
    }
}

export default InfoTab;