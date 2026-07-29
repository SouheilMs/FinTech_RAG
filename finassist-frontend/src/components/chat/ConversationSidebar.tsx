import { useState, useMemo } from 'react'
import { Plus, Search, Pin } from 'lucide-react'
import ConversationItem from './ConversationItem'
import type { Conversation } from '@/types'

interface Props {
    conversations: Conversation[]
    activeId: string | null
    onSelect: (id: string) => void
    onNew: () => void
    onRename: (id: string, title: string) => Promise<void>
    onPin: (id: string, pinned: boolean) => Promise<void>
    onDelete: (id: string) => Promise<void>
}

type DateGroup = { label: string; items: Conversation[] }

function groupByDate(conversations: Conversation[]): DateGroup[] {
    const now = Date.now()
    const DAY = 86_400_000
    const todayStart = now - (now % DAY) - new Date().getTimezoneOffset() * 60_000
    const yesterdayStart = todayStart - DAY
    const weekStart = todayStart - 6 * DAY

    const bucket = (c: Conversation) =>
        new Date(c.lastMessageAt ?? c.createdAt).getTime()

    const today = conversations.filter(c => bucket(c) >= todayStart)
    const yesterday = conversations.filter(c => bucket(c) >= yesterdayStart && bucket(c) < todayStart)
    const week = conversations.filter(c => bucket(c) >= weekStart     && bucket(c) < yesterdayStart)
    const older = conversations.filter(c => bucket(c) <  weekStart)

    return [
        { label: 'Today', items: today     },
        { label: 'Yesterday', items: yesterday },
        { label: 'Last 7 days', items: week      },
        { label: 'Older', items: older     },
    ].filter(g => g.items.length > 0)
}

export default function ConversationSidebar({ conversations, activeId, onSelect, onNew, onRename, onPin, onDelete }: Props) {
    const [search, setSearch] = useState('')

    const filtered = useMemo(() => {
        if (!search.trim()) return conversations
        const q = search.toLowerCase()
        return conversations.filter(c => c.title.toLowerCase().includes(q))
    }, [conversations, search])

    const pinned = filtered.filter(c => c.pinned)
    const recent = filtered.filter(c => !c.pinned)
    const groups = groupByDate(recent)

    return (
        <div className="flex flex-col h-full bg-surface-card border-r border-surface-border w-64">

            {/* New chat */}
            <div className="p-3 border-b border-surface-border">
                <button
                    onClick={onNew}
                    className="w-full flex items-center gap-2 px-3 py-2 text-sm font-medium text-text-secondary bg-surface rounded-xl border border-surface-border hover:border-primary/30 hover:text-primary hover:bg-primary-muted transition-all"
                >
                    <Plus size={15} />
                    New Chat
                </button>
            </div>

            {/* Search */}
            <div className="px-3 py-2 border-b border-surface-border">
                <div className="flex items-center gap-2 bg-surface rounded-lg px-2.5 py-1.5 border border-surface-border focus-within:border-primary/30">
                    <Search size={13} className="text-text-muted flex-shrink-0" />
                    <input
                        value={search}
                        onChange={e => setSearch(e.target.value)}
                        placeholder="Search conversations…"
                        className="flex-1 text-xs bg-transparent text-text-primary placeholder:text-text-muted outline-none"
                    />
                </div>
            </div>

            {/* Conversation list */}
            <div className="flex-1 overflow-y-auto px-2 py-2 space-y-1">

                {filtered.length === 0 && (
                    <p className="text-[11px] text-text-muted text-center py-8">
                        {search ? 'No conversations found' : 'No conversations yet'}
                    </p>
                )}

                {/* Pinned */}
                {pinned.length > 0 && (
                    <div className="mb-2">
                        <div className="flex items-center gap-1.5 px-3 py-1">
                            <Pin size={10} className="text-text-muted" />
                            <span className="text-[10px] font-medium text-text-muted uppercase tracking-wider">Pinned</span>
                        </div>
                        {pinned.map(c => (
                            <ConversationItem
                                key={c.id}
                                conversation={c}
                                isActive={c.id === activeId}
                                onSelect={onSelect}
                                onRename={onRename}
                                onPin={onPin}
                                onDelete={onDelete}
                            />
                        ))}
                    </div>
                )}

                {/* Date-grouped recent */}
                {groups.map(group => (
                    <div key={group.label} className="mb-2">
                        <p className="px-3 py-1 text-[10px] font-medium text-text-muted uppercase tracking-wider">
                            {group.label}
                        </p>
                        {group.items.map(c => (
                            <ConversationItem
                                key={c.id}
                                conversation={c}
                                isActive={c.id === activeId}
                                onSelect={onSelect}
                                onRename={onRename}
                                onPin={onPin}
                                onDelete={onDelete}
                            />
                        ))}
                    </div>
                ))}
            </div>
        </div>
    )
}