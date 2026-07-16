import { useState } from 'react'
import {
    Pencil, Trash2, KeyRound, ToggleLeft, ToggleRight,
    Loader2, ShieldCheck, User, Shield,
} from 'lucide-react'
import { cn } from '@/utils/cn'
import { formatDate } from '@/utils/format'
import type { AppUser } from '@/types/user'

interface Props {
    users: AppUser[]
    isLoading: boolean
    isError: boolean
    onEdit: (user: AppUser) => void
    onDelete: (user: AppUser) => void
    onResetPassword: (user: AppUser) => void
    onToggleStatus: (user: AppUser) => void
    onToggleRole: (user: AppUser, role: string, assign: boolean) => void
    busyId?: string
}

function Skeleton() {
    return (
        <tr className="border-b border-surface-border">
            {Array.from({ length: 7 }).map((_, i) => (
                <td key={i} className="px-4 py-3">
                    <div className="h-3 bg-surface-raised rounded animate-pulse w-3/4" />
                </td>
            ))}
        </tr>
    )
}

export default function UserTable({ users, isLoading, isError, onEdit, onDelete, onResetPassword,
                                      onToggleStatus, onToggleRole, busyId }: Props) {
    const [expandedId, setExpandedId] = useState<string | null>(null)

    if (isError) return (
        <div className="flex items-center justify-center py-16 text-sm text-danger">
            Failed to load users — check that Keycloak is running
        </div>
    )
    return (
        <div className="bg-surface-card border border-surface-border rounded-xl overflow-hidden">
            <div className="overflow-x-auto">
                <table className="w-full text-sm">
                    <thead>
                    <tr className="border-b border-surface-border bg-surface">
                        {['User', 'Email', 'Status', 'Roles', 'Created', 'Actions'].map(h => (
                            <th key={h} className="px-4 py-3 text-left text-[11px] font-medium text-text-muted uppercase tracking-wider">
                                {h}
                            </th>
                        ))}
                    </tr>
                    </thead>
                    <tbody>
                    {isLoading
                        ? Array.from({ length: 4 }).map((_, i) => <Skeleton key={i} />)
                        : users.length === 0
                            ? (
                                <tr>
                                    <td colSpan={6} className="py-16 text-center text-sm text-text-muted">
                                        No users found
                                    </td>
                                </tr>
                            )
                            : users.map(user => {
                                const isBusy   = busyId === user.id
                                const isExpanded = expandedId === user.id

                                return (
                                    <>
                                        <tr
                                            key={user.id}
                                            className={cn(
                                                'border-b border-surface-border transition-colors',
                                                isExpanded ? 'bg-surface-raised' : 'hover:bg-surface',
                                            )}
                                        >
                                            {/* User */}
                                            <td className="px-4 py-3">
                                                <div className="flex items-center gap-2.5">
                                                    <div className="flex-shrink-0 w-7 h-7 rounded-full bg-primary/20 text-primary flex items-center justify-center text-xs font-semibold uppercase">
                                                        {user.username[0]}
                                                    </div>
                                                    <div>
                                                        <p className="font-medium text-text-primary">{user.username}</p>
                                                        <p className="text-[11px] text-text-muted">{user.firstName} {user.lastName}</p>
                                                    </div>
                                                </div>
                                            </td>

                                            {/* Email */}
                                            <td className="px-4 py-3 text-text-secondary">{user.email}</td>

                                            {/* Status */}
                                            <td className="px-4 py-3">
                          <span className={cn(
                              'inline-flex items-center gap-1 text-[11px] font-medium px-2 py-0.5 rounded-full border',
                              user.enabled
                                  ? 'text-success bg-success/10 border-success/20'
                                  : 'text-text-muted bg-surface-raised border-surface-border',
                          )}>
                            <span className={cn('w-1.5 h-1.5 rounded-full', user.enabled ? 'bg-success' : 'bg-text-muted')} />
                              {user.enabled ? 'Active' : 'Disabled'}
                          </span>
                                            </td>

                                            {/* Roles */}
                                            <td className="px-4 py-3">
                                                <div className="flex gap-1 flex-wrap">
                                                    {user.roles.length === 0
                                                        ? <span className="text-[11px] text-text-muted">No roles</span>
                                                        : user.roles.map(role => (
                                                            <span key={role} className={cn(
                                                                'inline-flex items-center gap-0.5 text-[10px] font-medium px-1.5 py-0.5 rounded-full',
                                                                role === 'ADMIN'
                                                                    ? 'bg-primary/10 text-primary border border-primary/20'
                                                                    : 'bg-surface-border text-text-secondary',
                                                            )}>
                                  {role === 'ADMIN' ? <ShieldCheck size={9} /> : <User size={9} />}
                                                                {role}
                                </span>
                                                        ))
                                                    }
                                                </div>
                                            </td>

                                            {/* Created */}
                                            <td className="px-4 py-3 text-[11px] text-text-muted">
                                                {user.createdTimestamp ? formatDate(new Date(user.createdTimestamp).toISOString()) : '—'}
                                            </td>

                                            {/* Actions */}
                                            <td className="px-4 py-3">
                                                <div className="flex items-center gap-1">
                                                    {isBusy
                                                        ? <Loader2 size={14} className="animate-spin text-text-muted" />
                                                        : (
                                                            <>
                                                                <ActionBtn title="Edit" onClick={() => onEdit(user)}>
                                                                    <Pencil size={13} />
                                                                </ActionBtn>
                                                                <ActionBtn title="Reset password" onClick={() => onResetPassword(user)}>
                                                                    <KeyRound size={13} />
                                                                </ActionBtn>
                                                                <ActionBtn
                                                                    title={user.enabled ? 'Disable' : 'Enable'}
                                                                    onClick={() => onToggleStatus(user)}
                                                                    className={user.enabled ? 'hover:text-warning hover:bg-warning/10' : 'hover:text-success hover:bg-success/10'}
                                                                >
                                                                    {user.enabled ? <ToggleRight size={14} /> : <ToggleLeft size={14} />}
                                                                </ActionBtn>
                                                                <ActionBtn
                                                                    title="Manage roles"
                                                                    onClick={() => setExpandedId(isExpanded ? null : user.id)}
                                                                    className="hover:text-accent hover:bg-accent/10"
                                                                >
                                                                    <Shield size={13} />
                                                                </ActionBtn>
                                                                <ActionBtn
                                                                    title="Delete"
                                                                    onClick={() => onDelete(user)}
                                                                    className="hover:text-danger hover:bg-danger/10"
                                                                >
                                                                    <Trash2 size={13} />
                                                                </ActionBtn>
                                                            </>
                                                        )
                                                    }
                                                </div>
                                            </td>
                                        </tr>

                                        {/* Role management row */}
                                        {isExpanded && (
                                            <tr key={`${user.id}-roles`} className="bg-surface border-b border-surface-border">
                                                <td colSpan={6} className="px-6 py-3">
                                                    <div className="flex items-center gap-3">
                                                        <span className="text-[11px] font-medium text-text-muted uppercase tracking-wider">Roles</span>
                                                        {(['ADMIN', 'USER'] as const).map(role => {
                                                            const hasRole = user.roles.includes(role)
                                                            return (
                                                                <button
                                                                    key={role}
                                                                    onClick={() => onToggleRole(user, role, !hasRole)}
                                                                    className={cn(
                                                                        'flex items-center gap-1.5 text-xs font-medium px-3 py-1.5 rounded-lg border transition-all',
                                                                        hasRole
                                                                            ? 'bg-primary/10 text-primary border-primary/20 hover:bg-danger/10 hover:text-danger hover:border-danger/20'
                                                                            : 'bg-surface-raised text-text-muted border-surface-border hover:bg-primary/10 hover:text-primary hover:border-primary/20',
                                                                    )}
                                                                >
                                                                    {role === 'ADMIN' ? <ShieldCheck size={12} /> : <User size={12} />}
                                                                    {hasRole ? `Remove ${role}` : `Assign ${role}`}
                                                                </button>
                                                            )
                                                        })}
                                                    </div>
                                                </td>
                                            </tr>
                                        )}
                                    </>
                                )
                            })
                    }
                    </tbody>
                </table>
            </div>
        </div>
    )
}

function ActionBtn({ children, title, onClick, className }: {
    children: React.ReactNode
    title:    string
    onClick:  () => void
    className?: string
}) {
    return (
        <button
            title={title}
            onClick={onClick}
            className={cn(
                'p-1.5 rounded-lg text-text-muted transition-colors',
                className ?? 'hover:text-text-primary hover:bg-surface-raised',
            )}
        >
            {children}
        </button>
    )
}