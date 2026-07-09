import React from 'react'
import ReactDOM from 'react-dom/client'
import App from './App'
import keycloak from './keycloak'
import './index.css'

keycloak
    .init({
        onLoad:            'login-required', // redirects to Keycloak login if not authenticated
        checkLoginIframe:  false,            // avoids iframe issues in dev
        pkceMethod:        'S256',           // security best practice
    })
    .then((authenticated) => {
        if (!authenticated) {
            // init() already redirected — this branch is rarely hit
            keycloak.login()
            return
        }

        ReactDOM.createRoot(document.getElementById('root')!).render(
            <React.StrictMode>
                <App />
            </React.StrictMode>,
        )
    })
    .catch(() => {
        console.error('Keycloak init failed — is Keycloak running on port 8180?')
    })