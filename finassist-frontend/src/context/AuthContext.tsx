import { createContext, useContext, ReactNode, useState, useEffect } from 'react'
import keycloak from '@/keycloak'

interface AuthContextValue {
    email:   string
    username: string
    role:    'ADMIN' | 'USER'
    isAdmin: boolean
    token:   string
    logout:  () => void
}

const AuthContext = createContext<AuthContextValue | null>(null)

function parseRole(kc: typeof keycloak): 'ADMIN' | 'USER' {
    if (kc.hasRealmRole('ADMIN')) return 'ADMIN'
    return 'USER'
}

export function AuthProvider({ children }: { children: ReactNode }) {
    const [token, setToken] = useState(keycloak.token ?? '')

    useEffect(() => {
        // Auto-refresh token 60s before expiry
        const interval = setInterval(async () => {
            try {
                const refreshed = await keycloak.updateToken(60)
                if (refreshed) setToken(keycloak.token ?? '')
            } catch {
                keycloak.logout()
            }
        }, 30_000)

        return () => clearInterval(interval)
    }, [])

    const value: AuthContextValue = {
        email:    keycloak.tokenParsed?.email    ?? '',
        username: keycloak.tokenParsed?.preferred_username ?? '',
        role:     parseRole(keycloak),
        isAdmin:  keycloak.hasRealmRole('ADMIN'),
        token,
        logout:   () => keycloak.logout({ redirectUri: window.location.origin }),
    }

    return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

export function useAuth() {
    const ctx = useContext(AuthContext)
    if (!ctx) throw new Error('useAuth must be inside AuthProvider')
    return ctx
}