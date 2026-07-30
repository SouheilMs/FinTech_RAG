import { useState, useRef, useEffect, KeyboardEvent } from 'react'
import { Pin, PinOff, Pencil, Trash2, Check, X } from 'lucide-react'
import { cn } from '@/utils/cn'
import type { Conversation } from '@/types'

interface Props {
    conversation: Conversation
    isActive: boolean
    onSelect: () => void
    onRename: (title: string) => Promise<void>
    onPin: (pinned: boolean) => Promise<void>
    onDelete: () => Promise<void>
}

export default function SidebarConversationItem({ conversation, isActive, onSelect, onRename, onPin, onDelete }: Props) {
    const [isRenaming, setRenaming] = useState(false)
    const [draft, setDraft] = useState(conversation.title)
    const [confirmDelete, setConfirmDelete] = useState(false)
    const [busy, setBusy] = useState(false)
    const [error, setError] = useState<string | null>(null)

    const inputRef    = useRef<HTMLInputElement>(null)

    const committingRef = useRef(false)

    useEffect(() => {
        setDraft(conversation.title)
    }, [conversation.title])

    useEffect(() => {
        if (isRenaming) {
            committingRef.current = false
            setError(null)
            setTimeout(() => inputRef.current?.focus(), 0)
        }
    }, [isRenaming])

    const startRename = (e: React.MouseEvent) => {
        e.stopPropagation()
        setDraft(conversation.title)
        setRenaming(true)
    }

    const cancelRename = () => {
        if (committingRef.current) return
        setDraft(conversation.title)
        setRenaming(false)
        setError(null)
    }

    const commitRename = async () => {
        if (committingRef.current) return
        const title = draft.trim()
        if (!title) {
            cancelRename()
            return
        }

        if (title === conversation.title) {
            setRenaming(false)
            return
        }

        committingRef.current = true
        setBusy(true)
        setError(null)

        try {
            await onRename(title)
            setRenaming(false)
        } catch {
            setError('Rename failed')
            committingRef.current = false
        } finally {
            setBusy(false)
        }
    }

    const handleKey = (e: KeyboardEvent<HTMLInputElement>) => {
        if (e.key === 'Enter')  { e.preventDefault(); commitRename() }
        if (e.key === 'Escape') { e.preventDefault(); cancelRename() }
    }

    const handlePin = async (e: React.MouseEvent) => {
        e.stopPropagation()
        setBusy(true)
        setError(null)
        try {
            await onPin(!conversation.pinned)
        } catch {
            setError('Failed to update pin')
        } finally {
            setBusy(false)
        }
    }

    const handleDeleteConfirm = async (e: React.MouseEvent) => {
        e.stopPropagation()
        setBusy(true)
        try {
            await onDelete()
        } catch {
            setError('Delete failed')
            setBusy(false)
            setConfirmDelete(false)
        }
    }

    return (
        <div
            onClick={() => !isRenaming && !confirmDelete && !busy && onSelect()}
            className={cn(
                'group relative flex items-center gap-1.5 px-2 py-1.5 rounded-lg text-xs transition-all cursor-pointer select-none',
                isActive
                    ? 'bg-primary-muted text-primary'
                    : 'text-text-secondary hover:bg-surface-raised hover:text-text-primary',
                busy && 'opacity-60 pointer-events-none',
            )}
        >
            {/* Pin dot */}
            {conversation.pinned && !isRenaming && !confirmDelete && (
                <span className={cn(
                    'flex-shrink-0 w-1 h-1 rounded-full',
                    isActive ? 'bg-primary' : 'bg-text-muted',
                )} />
            )}

            {/* Rename mode */}
            {isRenaming ? (
                <div
                    className="flex items-center gap-1 flex-1 min-w-0"
                    onClick={e => e.stopPropagation()}
                >
                    <div className="flex-1 min-w-0">
                        <input
                            ref={inputRef}
                            value={draft}
                            onChange={e => { setDraft(e.target.value); setError(null) }}
                            onKeyDown={handleKey}
                            onBlur={cancelRename}
                            maxLength={120}
                            className="w-full bg-transparent outline-none border-b border-primary text-text-primary text-xs py-0.5"
                        />
                        {error && (
                            <p className="text-[10px] text-danger mt-0.5">{error}</p>
                        )}
                    </div>

                    <button
                        onMouseDown={e => { e.preventDefault(); e.stopPropagation(); commitRename() }}
                        className="flex-shrink-0 p-0.5 rounded text-success hover:bg-success/10 transition-colors"
                        title="Save (Enter)"
                    >
                        <Check size={11} />
                    </button>
                    <button
                        onMouseDown={e => { e.preventDefault(); e.stopPropagation(); cancelRename() }}
                        className="flex-shrink-0 p-0.5 rounded text-text-muted hover:bg-surface-raised transition-colors"
                        title="Cancel (Escape)"
                    >
                        <X size={11} />
                    </button>
                </div>

                /* Delete confirm */
            ) : confirmDelete ? (
                <div
                    className="flex items-center gap-1.5 flex-1 min-w-0"
                    onClick={e => e.stopPropagation()}
                >
          <span className="flex-1 min-w-0 truncate text-danger text-[11px]">
            Delete?
          </span>
                    <button
                        onClick={handleDeleteConfirm}
                        className="flex-shrink-0 p-0.5 rounded bg-danger/10 text-danger hover:bg-danger/20 transition-colors"
                        title="Confirm"
                    >
                        <Check size={11} />
                    </button>
                    <button
                        onClick={e => { e.stopPropagation(); setConfirmDelete(false) }}
                        className="flex-shrink-0 p-0.5 rounded text-text-muted hover:bg-surface-raised transition-colors"
                        title="Cancel"
                    >
                        <X size={11} />
                    </button>
                </div>

                /* Normal mode */
            ) : (
                <>
                    <span className="flex-1 min-w-0 truncate">{conversation.title}</span>

                    {error && (
                        <span className="text-[10px] text-danger flex-shrink-0" title={error}>!</span>
                    )}

                    <div className={cn(
                        'flex items-center gap-0.5 flex-shrink-0 transition-opacity',
                        'opacity-0 group-hover:opacity-100',
                        isActive && 'opacity-100',
                    )}>
                        <ActionButton title="Rename" onClick={startRename}>
                            <Pencil size={11} />
                        </ActionButton>
                        <ActionButton
                            title={conversation.pinned ? 'Unpin' : 'Pin'}
                            onClick={handlePin}
                        >
                            {conversation.pinned ? <PinOff size={11} /> : <Pin size={11} />}
                        </ActionButton>
                        <ActionButton
                            title="Delete"
                            onClick={e => { e.stopPropagation(); setConfirmDelete(true) }}
                            danger
                        >
                            <Trash2 size={11} />
                        </ActionButton>
                    </div>
                </>
            )}
        </div>
    )
}

function ActionButton({ children, title, onClick, danger = false }: {
    children: React.ReactNode
    title: string
    onClick: (e: React.MouseEvent) => void
    danger?:  boolean
}) {
    return (
        <button
            title={title}
            onClick={onClick}
            className={cn(
                'p-0.5 rounded transition-colors',
                danger
                    ? 'text-text-muted hover:text-danger hover:bg-danger/10'
                    : 'text-text-muted hover:text-text-primary hover:bg-surface-raised',
            )}
        >
            {children}
        </button>
    )
}