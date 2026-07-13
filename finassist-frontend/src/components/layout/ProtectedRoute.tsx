import { Navigate } from 'react-router-dom'
import { useAuth } from '@/context/AuthContext'

interface Props {
    children: React.ReactNode
    adminOnly?: boolean
}

export default function ProtectedRoute({ children, adminOnly = false }: Props) {
    const { isAdmin } = useAuth()

    if (adminOnly && !isAdmin) return <Navigate to="/" replace />

    return <>{children}</>
}