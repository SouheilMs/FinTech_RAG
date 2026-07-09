import axios from 'axios'
import keycloak from '@/keycloak'

const api = axios.create({
    baseURL: '/api',
    timeout: 60_000,
    headers: { 'Content-Type': 'application/json' },
})

api.interceptors.request.use(async (config) => {
    try {
        // Refresh token if it expires in less than 30 seconds
        await keycloak.updateToken(30)
    } catch {
        keycloak.logout()
    }
    if (keycloak.token) {
        config.headers.Authorization = `Bearer ${keycloak.token}`
    }
    return config
})

api.interceptors.response.use(
    (response) => response,
    (error) => {
        if (error.response?.status === 401) keycloak.logout()
        const message = error.response?.data?.message ?? error.message ?? 'Unexpected error'
        return Promise.reject({ message, status: error.response?.status })
    },
)

export default api