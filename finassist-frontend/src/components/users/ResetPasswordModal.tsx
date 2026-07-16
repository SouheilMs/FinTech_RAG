import { useState } from 'react'
import { X, Loader2, KeyRound, Eye, EyeOff } from 'lucide-react'
import type { AppUser } from '@/types/user'

interface Props {
    open:      boolean
    user:      AppUser | null
    onClose:   () => void
    onSubmit:  (id: string, password: string, temporary: boolean) => Promise<void>
    isLoading: boolean
}

export default function ResetPasswordModal({ open, user, onClose, onSubmit, isLoading }: Props) {
    const [password,   setPassword]   = useState('')
    const [temporary,  setTemporary]  = useState(false)
    const [showPass,   setShowPass]   = useState(false)
    const [error,      setError]      = useState<string | null>(null)

    if (!open || !user) return null

    const handleSubmit = async () => {
        if (password.length < 8) { setError('Password must be at least 8 characters'); return }
        setError(null)
        try {
            await onSubmit(user.id, password, temporary)
            setPassword('')
            onClose()
        } catch (e: unknown) {
            setError((e as { message?: string }).message ?? 'Failed to reset password')
        }
    }

    return (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4">
            <div className="absolute inset-0 bg-black/60 backdrop-blur-sm" onClick={!isLoading ? onClose : undefined} />
            <div className="relative bg-surface-card border border-surface-border rounded-2xl shadow-panel w-full max-w-sm animate-slide-up">

                <div className="flex items-center justify-between px-6 py-4 border-b border-surface-border">
                    <div className="flex items-center gap-2.5">
                        <div className="flex items-center justify-center w-8 h-8 rounded-lg bg-warning/10 border border-warning/20">
                            <KeyRound size={14} className="text-warning" />
                        </div>
                        <div>
                            <p className="text-sm font-medium text-text-primary">Reset Password</p>
                            <p className="text-[11px] text-text-muted">{user.username}</p>
                        </div>
                    </div>
                    <button onClick={onClose} disabled={isLoading} className="text-text-muted hover:text-text-primary transition-colors">
                        <X size={17} />
                    </button>
                </div>

                <div className="px-6 py-5 space-y-4">
                    {error && (
                        <div className="text-xs text-danger bg-danger/10 border border-danger/20 rounded-lg px-3 py-2">{error}</div>
                    )}

                    <div>
                        <label className="block text-xs font-medium text-text-secondary mb-1.5">New password</label>
                        <div className="relative">
                            <input
                                type={showPass ? 'text' : 'password'}
                                value={password}
                                onChange={e => { setPassword(e.target.value); setError(null) }}
                                placeholder="Min 8 characters"
                                className="w-full px-3 py-2 pr-9 text-sm bg-surface border border-surface-border rounded-xl text-text-primary outline-none focus:border-primary/40 transition-colors"
                            />
                            <button
                                type="button"
                                onClick={() => setShowPass(v => !v)}
                                className="absolute right-2.5 top-1/2 -translate-y-1/2 text-text-muted hover:text-text-secondary"
                            >
                                {showPass ? <EyeOff size={14} /> : <Eye size={14} />}
                            </button>
                        </div>
                    </div>

                    <div className="flex items-center justify-between py-2 px-3 bg-surface rounded-xl border border-surface-border">
                        <div>
                            <p className="text-sm text-text-secondary">Temporary password</p>
                            <p className="text-[11px] text-text-muted">User must change on next login</p>
                        </div>
                        <button
                            onClick={() => setTemporary(v => !v)}
                            className={`relative w-10 h-5 rounded-full transition-colors ${temporary ? 'bg-primary' : 'bg-surface-border'}`}
                        >
                            <span className={`absolute top-0.5 w-4 h-4 bg-white rounded-full shadow transition-transform ${temporary ? 'translate-x-5' : 'translate-x-0.5'}`} />
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
                        disabled={isLoading || !password}
                        className="flex-1 flex items-center justify-center gap-2 py-2 text-sm font-medium text-white bg-warning rounded-xl hover:bg-warning/90 disabled:opacity-60 transition-colors"
                    >
                        {isLoading && <Loader2 size={14} className="animate-spin" />}
                        Reset password
                    </button>
                </div>
            </div>
        </div>
    )
}