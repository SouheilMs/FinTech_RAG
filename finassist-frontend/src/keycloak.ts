import Keycloak from 'keycloak-js'

// One instance shared across the entire app
const keycloak = new Keycloak({
    url:       'http://localhost:8180',
    realm:     'finassist',
    clientId: 'finassist-app',
})
export default keycloak