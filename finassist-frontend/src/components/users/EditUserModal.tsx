import { useEffect, useState } from 'react'
import { X, Loader2, Pencil } from 'lucide-react'
import { cn } from '@/utils/cn'
import type { AppUser, UpdateUserPayload } from '@/types/user'

interface Props {
    open:      boolean
    user:      AppUser | null
    onClose:   () => void
    onSubmit:  (id: string, payload: UpdateUserPayload) => Promise<void>
    isLoading: boolean
}

export default function EditUserModal({ open, user, onClose, onSubmit, isLoading }: Props) {
    const [form, setForm]     = useState<UpdateUserPayload>({ username: '', email: '', firstName: '', lastName: '', enabled: true })
    const [apiError, setApiError] = useState<string | null>(null)

    useEffect(() => {
        if (user) setForm({
            username:  user.username,
            email:     user.email,
            firstName: user.firstName,
            lastName:  user.lastName,
            enabled:   user.enabled,
        })
        setApiError(null)
    }, [user])

    if (!open || !user) return null

    const set = (key: keyof UpdateUserPayload) =>
        (e: React.ChangeEvent<HTMLInputElement>) =>
            setForm(f => ({ ...f, [key]: e.target.value }))

    const handleSubmit = async () => {
        setApiError(null)
        try {
            await onSubmit(user.id, form)
            onClose()
        } catch (e: unknown) {
            setApiError((e as { message?: string }).message ?? 'Failed to update user')
        }
    }

    return (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4">
            <div className="absolute inset-0 bg-black/60 backdrop-blur-sm" onClick={!isLoading ? onClose : undefined} />
            <div className="relative bg-surface-card border border-surface-border rounded-2xl shadow-panel w-full max-w-md animate-slide-up">

                <div className="flex items-center justify-between px-6 py-4 border-b border-surface-border">
                    <div className="flex items-center gap-2.5">
                        <div className="flex items-center justify-center w-8 h-8 rounded-lg bg-accent/10 border border-accent/20">
                            <Pencil size={14} className="text-accent" />
                        </div>
                        <div>
                            <p className="text-sm font-medium text-text-primary">Edit User</p>
                            <p className="text-[11px] text-text-muted">{user.username}</p>
                        </div>
                    </div>
                    <button onClick={onClose} disabled={isLoading} className="text-text-muted hover:text-text-primary transition-colors">
                        <X size={17} />
                    </button>
                </div>

                <div className="px-6 py-5 space-y-4">
                    {apiError && (
                        <div className="text-xs text-danger bg-danger/10 border border-danger/20 rounded-lg px-3 py-2">{apiError}</div>
                    )}

                    <div className="grid grid-cols-2 gap-3">
                        {(['firstName', 'lastName'] as const).map(key => (
                            <div key={key}>
                                <label className="block text-xs font-medium text-text-secondary mb-1.5 capitalize">
                                    {key === 'firstName' ? 'First name' : 'Last name'}
                                </label>
                                <input
                                    type="text"
                                    value={form[key]}
                                    onChange={set(key)}
                                    className="w-full px-3 py-2 text-sm bg-surface border border-surface-border rounded-xl text-text-primary outline-none focus:border-primary/40 transition-colors"
                                />
                            </div>
                        ))}
                    </div>

                    {(['username', 'email'] as const).map(key => (
                        <div key={key}>
                            <label className="block text-xs font-medium text-text-secondary mb-1.5 capitalize">{key}</label>
                            <input
                                type={key === 'email' ? 'email' : 'text'}
                                value={form[key]}
                                onChange={set(key)}
                                className="w-full px-3 py-2 text-sm bg-surface border border-surface-border rounded-xl text-text-primary outline-none focus:border-primary/40 transition-colors"
                            />
                        </div>
                    ))}

                    <div className="flex items-center justify-between py-2 px-3 bg-surface rounded-xl border border-surface-border">
                        <span className="text-sm text-text-secondary">Account enabled</span>
                        <button
                            onClick={() => setForm(f => ({ ...f, enabled: !f.enabled }))}
                            className={cn(
                                'relative w-10 h-5 rounded-full transition-colors',
                                form.enabled ? 'bg-success' : 'bg-surface-border',
                            )}
                        >
              <span className={cn(
                  'absolute top-0.5 w-4 h-4 bg-white rounded-full shadow transition-transform',
                  form.enabled ? 'translate-x-5' : 'translate-x-0.5',
              )} />
                        </button>
                    </div>
                </div>

                <div className="flex gap-3 px-6 py-4 border-t border-surface-border">
                    <button
                        onClick={onClose}
                        disabled={isLoading}
                        className="flex-1 py-2 text-sm font-medium text-text-secondary border border-surface-border rounded-xl hover:bg-surface-raised disabled:opacity-40 transition-colors"
                    >
                        Cancel
                    </button>
                    <button
                        onClick={handleSubmit}
                        disabled={isLoading}
                        className="flex-1 flex items-center justify-center gap-2 py-2 text-sm font-medium text-white bg-primary rounded-xl hover:bg-primary-hover disabled:opacity-60 transition-colors"
                    >
                        {isLoading && <Loader2 size={14} className="animate-spin" />}
                        Save changes
                    </button>
                </div>
            </div>
        </div>
    )
}