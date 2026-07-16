import { useState } from 'react'
import { X, Loader2, UserPlus, Eye, EyeOff } from 'lucide-react'
import { cn } from '@/utils/cn'
import type { CreateUserPayload } from '@/types/user'

interface Props {
    open:      boolean
    onClose:   () => void
    onSubmit:  (payload: CreateUserPayload) => Promise<void>
    isLoading: boolean
}

const empty: CreateUserPayload = {
    username: '', email: '', firstName: '', lastName: '', password: '',
}

export default function CreateUserModal({ open, onClose, onSubmit, isLoading }: Props) {
    const [form, setForm]         = useState<CreateUserPayload>(empty)
    const [errors, setErrors]     = useState<Partial<CreateUserPayload>>({})
    const [showPass, setShowPass] = useState(false)
    const [apiError, setApiError] = useState<string | null>(null)

    if (!open) return null

    const set = (key: keyof CreateUserPayload) =>
        (e: React.ChangeEvent<HTMLInputElement>) => {
            setForm(f => ({ ...f, [key]: e.target.value }))
            setErrors(er => ({ ...er, [key]: undefined }))
        }

    const validate = (): boolean => {
        const e: Partial<CreateUserPayload> = {}
        if (!form.username.trim())       e.username  = 'Required'
        if (form.username.length < 3)    e.username  = 'Min 3 characters'
        if (!form.email.includes('@'))   e.email     = 'Invalid email'
        if (!form.firstName.trim())      e.firstName = 'Required'
        if (!form.lastName.trim())       e.lastName  = 'Required'
        if (form.password.length < 8)    e.password  = 'Min 8 characters'
        setErrors(e)
        return Object.keys(e).length === 0
    }

    const handleSubmit = async () => {
        if (!validate()) return
        setApiError(null)
        try {
            await onSubmit(form)
            setForm(empty)
            onClose()
        } catch (e: unknown) {
            setApiError((e as { message?: string }).message ?? 'Failed to create user')
        }
    }

    return (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4">
            <div className="absolute inset-0 bg-black/60 backdrop-blur-sm" onClick={!isLoading ? onClose : undefined} />
            <div className="relative bg-surface-card border border-surface-border rounded-2xl shadow-panel w-full max-w-md animate-slide-up">

                {/* Header */}
                <div className="flex items-center justify-between px-6 py-4 border-b border-surface-border">
                    <div className="flex items-center gap-2.5">
                        <div className="flex items-center justify-center w-8 h-8 rounded-lg bg-primary/10 border border-primary/20">
                            <UserPlus size={15} className="text-primary" />
                        </div>
                        <p className="text-sm font-medium text-text-primary">Create User</p>
                    </div>
                    <button onClick={onClose} disabled={isLoading} className="text-text-muted hover:text-text-primary transition-colors">
                        <X size={17} />
                    </button>
                </div>

                {/* Body */}
                <div className="px-6 py-5 space-y-4">
                    {apiError && (
                        <div className="text-xs text-danger bg-danger/10 border border-danger/20 rounded-lg px-3 py-2">
                            {apiError}
                        </div>
                    )}

                    <div className="grid grid-cols-2 gap-3">
                        {([
                            { key: 'firstName', label: 'First name', type: 'text' },
                            { key: 'lastName',  label: 'Last name',  type: 'text' },
                        ] as const).map(({ key, label, type }) => (
                            <Field key={key} label={label} error={errors[key]}>
                                <input
                                    type={type}
                                    value={form[key]}
                                    onChange={set(key)}
                                    className={inputCls(!!errors[key])}
                                />
                            </Field>
                        ))}
                    </div>

                    <Field label="Username" error={errors.username}>
                        <input type="text" value={form.username} onChange={set('username')} className={inputCls(!!errors.username)} />
                    </Field>

                    <Field label="Email" error={errors.email}>
                        <input type="email" value={form.email} onChange={set('email')} className={inputCls(!!errors.email)} />
                    </Field>

                    <Field label="Password" error={errors.password}>
                        <div className="relative">
                            <input
                                type={showPass ? 'text' : 'password'}
                                value={form.password}
                                onChange={set('password')}
                                className={cn(inputCls(!!errors.password), 'pr-9')}
                            />
                            <button
                                type="button"
                                onClick={() => setShowPass(v => !v)}
                                className="absolute right-2.5 top-1/2 -translate-y-1/2 text-text-muted hover:text-text-secondary"
                            >
                                {showPass ? <EyeOff size={14} /> : <Eye size={14} />}
                            </button>
                        </div>
                    </Field>
                </div>

                {/* Footer */}
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
                        Create User
                    </button>
                </div>
            </div>
        </div>
    )
}

function Field({ label, error, children }: { label: string; error?: string; children: React.ReactNode }) {
    return (
        <div>
            <label className="block text-xs font-medium text-text-secondary mb-1.5">{label}</label>
            {children}
            {error && <p className="text-[11px] text-danger mt-1">{error}</p>}
        </div>
    )
}

function inputCls(hasError: boolean) {
    return cn(
        'w-full px-3 py-2 text-sm bg-surface border rounded-xl text-text-primary placeholder:text-text-muted outline-none transition-colors',
        hasError
            ? 'border-danger/50 focus:border-danger'
            : 'border-surface-border focus:border-primary/40',
    )
}