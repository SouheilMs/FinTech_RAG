import { useState, useRef, useEffect, KeyboardEvent } from 'react'
import { Pin, PinOff, Pencil, Trash2, Check, X, MessageSquare } from 'lucide-react'
import { cn } from '@/utils/cn'
import type { Conversation } from '@/types'

interface Props {
    conversation: Conversation
    isActive: boolean
    onSelect: (id: string) => void
    onRename: (id: string, title: string) => Promise<void>
    onPin: (id: string, pinned: boolean) => Promise<void>
    onDelete: (id: string) => Promise<void>
}

export default function ConversationItem({ conversation, isActive, onSelect, onRename, onPin, onDelete }: Props) {
    const [isRenaming, setRenaming] = useState(false)
    const [draftTitle, setDraft]  = useState(conversation.title)
    const [confirmDelete, setConfirmDelete] = useState(false)
    const inputRef = useRef<HTMLInputElement>(null)

    useEffect(() => {
        if (isRenaming) inputRef.current?.focus()
    }, [isRenaming])

    const commitRename = async () => {
        const title = draftTitle.trim()
        if (title && title !== conversation.title) await onRename(conversation.id, title)
        else setDraft(conversation.title)
        setRenaming(false)
    }

    const handleKey = (e: KeyboardEvent<HTMLInputElement>) => {
        if (e.key === 'Enter')  commitRename()
        if (e.key === 'Escape') { setDraft(conversation.title); setRenaming(false) }
    }

    return (
        <div
            onClick={() => !isRenaming && onSelect(conversation.id)}
            className={cn(
                'group relative flex items-center gap-2 px-3 py-2 rounded-lg cursor-pointer transition-all text-sm',
                isActive
                    ? 'bg-primary-muted text-primary'
                    : 'text-text-secondary hover:bg-surface-raised hover:text-text-primary',
            )}
        >
            {/* Icon */}
            <MessageSquare size={14} className="flex-shrink-0 opacity-60" />

            {/* Title / rename input */}
            {isRenaming ? (
                <input
                    ref={inputRef}
                    value={draftTitle}
                    onChange={e => setDraft(e.target.value)}
                    onKeyDown={handleKey}
                    onBlur={commitRename}
                    onClick={e => e.stopPropagation()}
                    maxLength={120}
                    className="flex-1 min-w-0 bg-transparent outline-none border-b border-primary text-text-primary"
                />
            ) : (
                <span className="flex-1 min-w-0 truncate">{conversation.title}</span>
            )}

            {/* Action buttons — visible on hover or when active */}
            {!isRenaming && !confirmDelete && (
                <div className={cn(
                    'flex items-center gap-0.5 flex-shrink-0',
                    'opacity-0 group-hover:opacity-100 transition-opacity',
                    isActive && 'opacity-100',
                )}>
                    <button
                        onClick={e => { e.stopPropagation(); setRenaming(true) }}
                        className="p-1 rounded hover:bg-surface-raised text-text-muted hover:text-text-primary"
                        title="Rename"
                    >
                        <Pencil size={12} />
                    </button>
                    <button
                        onClick={e => { e.stopPropagation(); onPin(conversation.id, !conversation.pinned) }}
                        className="p-1 rounded hover:bg-surface-raised text-text-muted hover:text-text-primary"
                        title={conversation.pinned ? 'Unpin' : 'Pin'}
                    >
                        {conversation.pinned ? <PinOff size={12} /> : <Pin size={12} />}
                    </button>
                    <button
                        onClick={e => { e.stopPropagation(); setConfirmDelete(true) }}
                        className="p-1 rounded hover:bg-danger/10 text-text-muted hover:text-danger"
                        title="Delete"
                    >
                        <Trash2 size={12} />
                    </button>
                </div>
            )}

            {/* Delete confirm */}
            {confirmDelete && (
                <div className="flex items-center gap-1 flex-shrink-0" onClick={e => e.stopPropagation()}>
                    <span className="text-[10px] text-danger mr-1">Delete?</span>
                    <button
                        onClick={() => { onDelete(conversation.id); setConfirmDelete(false) }}
                        className="p-1 rounded bg-danger/10 text-danger hover:bg-danger/20"
                    >
                        <Check size={11} />
                    </button>
                    <button
                        onClick={() => setConfirmDelete(false)}
                        className="p-1 rounded hover:bg-surface-raised text-text-muted"
                    >
                        <X size={11} />
                    </button>
                </div>
            )}
        </div>
    )
}