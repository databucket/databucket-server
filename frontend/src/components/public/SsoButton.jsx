import React from "react";
import PropTypes from "prop-types";
import {Button, SvgIcon} from "@mui/material";
import LoginIcon from "@mui/icons-material/Login";
import {getBaseUrl} from "../../utils/UrlBuilder";

// Logos follow the providers' sign-in button guidelines (Microsoft, Google);
// Okta and Keycloak come from Simple Icons (CC0).
const MicrosoftLogo = () => (
    <SvgIcon viewBox="0 0 21 21">
        <rect x="1" y="1" width="9" height="9" fill="#F25022"/>
        <rect x="11" y="1" width="9" height="9" fill="#7FBA00"/>
        <rect x="1" y="11" width="9" height="9" fill="#00A4EF"/>
        <rect x="11" y="11" width="9" height="9" fill="#FFB900"/>
    </SvgIcon>
);

const GoogleLogo = () => (
    <SvgIcon viewBox="0 0 48 48">
        <path fill="#EA4335" d="M24 9.5c3.54 0 6.71 1.22 9.21 3.6l6.85-6.85C35.9 2.38 30.47 0 24 0 14.62 0 6.51 5.38 2.56 13.22l7.98 6.19C12.43 13.72 17.74 9.5 24 9.5z"/>
        <path fill="#4285F4" d="M46.98 24.55c0-1.57-.15-3.09-.38-4.55H24v9.02h12.94c-.58 2.96-2.26 5.48-4.78 7.18l7.73 6c4.51-4.18 7.09-10.36 7.09-17.65z"/>
        <path fill="#FBBC05" d="M10.53 28.59c-.48-1.45-.76-2.99-.76-4.59s.27-3.14.76-4.59l-7.98-6.19C.92 16.46 0 20.12 0 24c0 3.88.92 7.54 2.56 10.78l7.97-6.19z"/>
        <path fill="#34A853" d="M24 48c6.48 0 11.93-2.13 15.89-5.81l-7.73-6c-2.15 1.45-4.92 2.3-8.16 2.3-6.26 0-11.57-4.22-13.47-9.91l-7.98 6.19C6.51 42.62 14.62 48 24 48z"/>
    </SvgIcon>
);

const OktaLogo = () => (
    <SvgIcon viewBox="0 0 24 24" htmlColor="#007DC1">
        <path d="M12 0C5.389 0 0 5.35 0 12s5.35 12 12 12 12-5.35 12-12S18.611 0 12 0zm0 18c-3.325 0-6-2.675-6-6s2.675-6 6-6 6 2.675 6 6-2.675 6-6 6z"/>
    </SvgIcon>
);

const KeycloakLogo = () => (
    <SvgIcon viewBox="0 0 24 24" htmlColor="#4D4D4D">
        <path d="m18.742 1.182-12.493.002C4.155 4.784 2.079 8.393 0 12.002c2.071 3.612 4.162 7.214 6.252 10.816l12.49-.004 3.089-5.404h2.158v-.002H24L23.996 6.59h-2.168zM8.327 4.792h2.081l1.04 1.8-3.12 5.413 3.117 5.403-1.035 1.81H8.327a2047.566 2047.566 0 0 0-4.168-7.204C5.547 9.606 6.937 7.2 8.327 4.792Zm6.241 0 2.086.003c1.393 2.405 2.78 4.813 4.166 7.222l-4.167 7.2h-2.08c-.382-.562-1.038-1.808-1.038-1.808l3.123-5.405-3.124-5.413z"/>
    </SvgIcon>
);

const PROVIDERS = {
    microsoft: {name: 'Microsoft', logo: <MicrosoftLogo/>},
    google: {name: 'Google', logo: <GoogleLogo/>},
    okta: {name: 'Okta', logo: <OktaLogo/>},
    keycloak: {name: 'Keycloak', logo: <KeycloakLogo/>},
    generic: {name: 'SSO', logo: <LoginIcon/>}
};

export default function SsoButton({provider, name}) {
    const style = PROVIDERS[provider] || PROVIDERS.generic;
    const label = name != null && name.trim().length > 0 ? name : style.name;

    return (
        <Button
            fullWidth={true}
            variant="outlined"
            size={'large'}
            startIcon={style.logo}
            // the server answers with a redirect to the identity provider
            href={getBaseUrl('public/sso/login')}
            sx={{
                textTransform: 'none',
                color: 'text.primary',
                borderColor: 'divider',
                '&:hover': {borderColor: 'text.secondary'}
            }}
        >
            Sign in with {label}
        </Button>
    );
}

SsoButton.propTypes = {
    provider: PropTypes.string,
    name: PropTypes.string
};
