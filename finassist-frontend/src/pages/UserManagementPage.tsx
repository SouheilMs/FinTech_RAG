import { useState } from 'react'
import { UserPlus, Users, RefreshCw } from 'lucide-react'
import { useUsers }          from '@/hooks/useUsers'
import { useToast }          from '@/hooks/useToast'
import UserTable             from '@/components/users/UserTable'
import CreateUserModal       from '@/components/users/CreateUserModal'
import EditUserModal         from '@/components/users/EditUserModal'
import ResetPasswordModal    from '@/components/users/ResetPasswordModal'
import ToastContainer        from '@/components/ui/ToastContainer'
import type { AppUser }      from '@/types/user'

export default function UserManagementPage() {
    const toast = useToast()
    const { users, isLoading, isError, createUser, isCreating, updateUser, isUpdating,
        deleteUser, resetPassword, isResettingPassword, updateStatus,
        assignRole, removeRole } = useUsers()

    const [showCreate, setShowCreate] = useState(false)
    const [editTarget, setEditTarget] = useState<AppUser | null>(null)
    const [resetTarget, setResetTarget] = useState<AppUser | null>(null)
    const [busyId, setBusyId] = useState<string | undefined>()

    const handle = async (id: string, action: () => Promise<void>, successMsg: string) => {
        setBusyId(id)
        try {
            await action()
            toast.success(successMsg)
        } catch (e: unknown) {
            toast.error((e as { message?: string }).message ?? 'Operation failed')
        } finally {
            setBusyId(undefined)
        }
    }

    const handleDelete = (user: AppUser) => {
        if (!confirm(`Delete user "${user.username}"? This cannot be undone.`)) return
        handle(user.id, () => deleteUser(user.id), 'User deleted')
    }

    const handleToggleStatus = (user: AppUser) => {
        handle(
            user.id,
            () => updateStatus({ id: user.id, enabled: !user.enabled }),
            user.enabled ? 'User disabled' : 'User enabled',
        )
    }

    const handleToggleRole = (user: AppUser, role: string, assign: boolean) => {
        handle(
            user.id,
            () => assign ? assignRole({ id: user.id, role }) : removeRole({ id: user.id, role }),
            assign ? `Role ${role} assigned` : `Role ${role} removed`,
        )
    }
    const active   = users.filter(u => u.enabled).length
    const admins   = users.filter(u => u.roles.includes('ADMIN')).length
    const disabled = users.filter(u => !u.enabled).length

    return (
        <div className="px-4 md:px-8 py-8 max-w-7xl mx-auto space-y-6">
            {/* Header */}
            <div className="flex items-center justify-between">
                <div>
                    <h2 className="text-xl font-semibold text-text-primary">User Management</h2>
                    <p className="text-sm text-text-secondary mt-1">
                        Manage user accounts and roles via Keycloak
                    </p>
                </div>
                <button
                    onClick={() => setShowCreate(true)}
                    className="flex items-center gap-2 px-4 py-2 text-sm font-medium text-white bg-primary rounded-xl hover:bg-primary-hover transition-colors"
                >
                    <UserPlus size={15} />
                    Create User
                </button>
            </div>
            {/* Stats */}
            {!isLoading && users.length > 0 && (
                <div className="grid grid-cols-2 sm:grid-cols-4 gap-3">
                    {[
                        { label: 'Total',    value: users.length, color: 'text-text-primary' },
                        { label: 'Active',   value: active,       color: 'text-success'      },
                        { label: 'Admins',   value: admins,       color: 'text-primary'      },
                        { label: 'Disabled', value: disabled,     color: 'text-text-muted'   },
                    ].map(stat => (
                        <div key={stat.label} className="bg-surface-card border border-surface-border rounded-xl px-4 py-3">
                            <p className={`text-xl font-semibold ${stat.color}`}>{stat.value}</p>
                            <p className="text-[11px] text-text-muted mt-0.5">{stat.label}</p>
                        </div>
                    ))}
                </div>
            )}
            {/* Table */}
            <UserTable
                users={users}
                isLoading={isLoading}
                isError={isError}
                onEdit={setEditTarget}
                onDelete={handleDelete}
                onResetPassword={setResetTarget}
                onToggleStatus={handleToggleStatus}
                onToggleRole={handleToggleRole}
                busyId={busyId}
            />
            {/* Modals */}
            <CreateUserModal
                open={showCreate}
                onClose={() => setShowCreate(false)}
                onSubmit={payload => createUser(payload).then(() => toast.success('User created'))}
                isLoading={isCreating}
            />
            <EditUserModal
                open={editTarget !== null}
                user={editTarget}
                onClose={() => setEditTarget(null)}
                onSubmit={(id, payload) =>
                    updateUser({ id, payload }).then(() => {
                        toast.success('User updated')
                        setEditTarget(null)
                    })
                }
                isLoading={isUpdating}
            />
            <ResetPasswordModal
                open={resetTarget !== null}
                user={resetTarget}
                onClose={() => setResetTarget(null)}
                onSubmit={(id, password, temporary) =>
                    resetPassword({ id, payload: { password, temporary } }).then(() => {
                        toast.success('Password reset successfully')
                        setResetTarget(null)
                    })
                }
                isLoading={isResettingPassword}
            />
            <ToastContainer toasts={toast.toasts} onDismiss={toast.dismiss} />
        </div>
    )
}