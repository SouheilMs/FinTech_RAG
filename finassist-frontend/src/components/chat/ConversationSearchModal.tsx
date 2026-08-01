import { useState, useEffect, useRef } from 'react'
import { X, Search, MessageSquare, Pin } from 'lucide-react'
import { cn } from '@/utils/cn'
import { useConversations } from '@/hooks/useConversations'
import { formatDate } from '@/utils/format'

interface Props {
    open: boolean
    activeId: string | null
    onSelect: (id: string) => void
    onClose: () => void
}

export default function ConversationSearchModal({ open, activeId, onSelect, onClose }: Props) {
    const [query, setQuery]  = useState('')
    const inputRef = useRef<HTMLInputElement>(null)
    const { conversations } = useConversations()

    // Focus input when modal opens and clear search on close
    useEffect(() => {
        if (!open) return
        const timer = setTimeout(() => {
            setQuery('')
            inputRef.current?.focus()
        }, 50)
        return () => clearTimeout(timer)
    }, [open])

    // Close on Escape
    useEffect(() => {
        if (!open) return
        const handler = (e: KeyboardEvent) => { if (e.key === 'Escape') onClose() }
        window.addEventListener('keydown', handler)
        return () => window.removeEventListener('keydown', handler)
    }, [open, onClose])

    if (!open) return null

    const filtered = conversations.filter(c =>
        !query.trim() || c.title.toLowerCase().includes(query.toLowerCase())
    )

    const pinned = filtered.filter(c => c.pinned)
    const recent = filtered.filter(c => !c.pinned)

    const handleSelect = (id: string) => {
        onSelect(id)
        onClose()
    }

    return (
        <div className="fixed inset-0 z-50 flex items-start justify-center pt-[10vh] px-4">
            {/* Backdrop */}
            <div
                className="absolute inset-0 bg-black/60 backdrop-blur-sm"
                onClick={onClose}
            />

            {/* Modal */}
            <div className="relative bg-surface-card border border-surface-border rounded-2xl shadow-panel w-full max-w-lg max-h-[75vh] flex flex-col animate-slide-up">

                {/* Search input */}
                <div className="flex items-center gap-3 px-4 py-3 border-b border-surface-border">
                    <Search size={16} className="text-text-muted flex-shrink-0" />
                    <input
                        ref={inputRef}
                        value={query}
                        onChange={e => setQuery(e.target.value)}
                        placeholder="Search conversations…"
                        className="flex-1 bg-transparent text-sm text-text-primary placeholder:text-text-muted outline-none"
                    />
                    <button
                        onClick={onClose}
                        className="text-text-muted hover:text-text-primary transition-colors"
                    >
                        <X size={16} />
                    </button>
                </div>

                {/* Results */}
                <div className="flex-1 overflow-y-auto p-2">
                    {filtered.length === 0 && (
                        <div className="flex flex-col items-center justify-center py-12 gap-2">
                            <p className="text-sm text-text-secondary">No conversations found</p>
                            {query && (
                                <p className="text-xs text-text-muted">Try a different search term</p>
                            )}
                        </div>
                    )}

                    {/* Pinned */}
                    {pinned.length > 0 && (
                        <div className="mb-2">
                            <div className="flex items-center gap-1.5 px-3 py-1.5">
                                <Pin size={10} className="text-text-muted" />
                                <span className="text-[10px] font-medium text-text-muted uppercase tracking-wider">
                  Pinned
                </span>
                            </div>
                            {pinned.map(c => (
                                <ConversationRow
                                    key={c.id}
                                    title={c.title}
                                    date={c.lastMessageAt ?? c.createdAt}
                                    isActive={c.id === activeId}
                                    isPinned
                                    onClick={() => handleSelect(c.id)}
                                />
                            ))}
                        </div>
                    )}

                    {/* Recent */}
                    {recent.length > 0 && (
                        <div>
                            {pinned.length > 0 && (
                                <div className="px-3 py-1.5">
                  <span className="text-[10px] font-medium text-text-muted uppercase tracking-wider">
                    Recent
                  </span>
                                </div>
                            )}
                            {recent.map(c => (
                                <ConversationRow
                                    key={c.id}
                                    title={c.title}
                                    date={c.lastMessageAt ?? c.createdAt}
                                    isActive={c.id === activeId}
                                    onClick={() => handleSelect(c.id)}
                                />
                            ))}
                        </div>
                    )}
                </div>

                {/* Footer */}
                {filtered.length > 0 && (
                    <div className="px-4 py-2 border-t border-surface-border">
                        <p className="text-[10px] text-text-muted">
                            {filtered.length} conversation{filtered.length !== 1 ? 's' : ''}
                        </p>
                    </div>
                )}
            </div>
        </div>
    )
}

function ConversationRow({ title, date, isActive, isPinned, onClick }: {
    title: string
    date: string | null
    isActive: boolean
    isPinned?: boolean
    onClick: () => void
}) {
    return (
        <button
            onClick={onClick}
            className={cn(
                'w-full flex items-center gap-3 px-3 py-2.5 rounded-xl text-left transition-colors',
                isActive
                    ? 'bg-primary-muted text-primary'
                    : 'text-text-secondary hover:bg-surface-raised hover:text-text-primary',
            )}
        >
            <MessageSquare size={14} className="flex-shrink-0 opacity-60" />
            <div className="flex-1 min-w-0">
                <p className="text-sm truncate">{title}</p>
                {date && (
                    <p className="text-[11px] text-text-muted mt-0.5">
                        {formatDate(date)}
                    </p>
                )}
            </div>
            {isPinned && <Pin size={11} className="flex-shrink-0 text-text-muted" />}
        </button>
    )
}