import { useState } from 'react'
import { NavLink, useNavigate } from 'react-router-dom'
import { MessageSquare, FileText, GitBranch, Users, Zap, LogOut, Search, MessageCircle } from 'lucide-react'
import { cn } from '@/utils/cn'
import { useAuth } from '@/context/AuthContext'
import { useConversationContext } from '@/context/ConversationContext'
import { useConversations } from '@/hooks/useConversations'
import SidebarConversationItem from '@/components/layout/SidebarConversationItem'
import ConversationSearchModal from '@/components/chat/ConversationSearchModal'

const MAX_SIDEBAR_CONVERSATIONS = 10

export default function Sidebar() {
    const { username, email, role, isAdmin, logout } = useAuth()
    const { activeId, setActiveId } = useConversationContext()
    const { conversations, renameConversation, pinConversation, deleteConversation } = useConversations()
    const navigate = useNavigate()

    const [confirmLogout, setConfirmLogout] = useState(false)
    const [searchOpen, setSearchOpen] = useState(false)

    const navItems = [
        { to: '/', icon: MessageSquare, label: 'AI Assistant', show: true },
        { to: '/documents', icon: FileText, label: 'Documents', show: isAdmin },
        { to: '/repositories', icon: GitBranch, label: 'Repositories', show: isAdmin },
        { to: '/users', icon: Users, label: 'Users', show: isAdmin },
    ].filter(i => i.show)

    const sidebarConversations = conversations.slice(0, MAX_SIDEBAR_CONVERSATIONS)

    const handleConversationSelect = (id: string) => {
        setActiveId(id)
        navigate('/')
    }

    return (
        <>
            <aside className="hidden md:flex flex-col w-60 shrink-0 bg-surface-card border-r border-surface-border h-screen sticky top-0">

                {/* Logo */}
                <div className="flex items-center gap-2.5 px-5 py-5 border-b border-surface-border">
                    <div className="flex items-center justify-center w-8 h-8 rounded-lg bg-primary">
                        <Zap size={16} className="text-white" />
                    </div>
                    <div>
                        <p className="text-sm font-semibold text-text-primary leading-none">FinAssist</p>
                        <p className="text-[10px] text-text-muted mt-0.5 uppercase tracking-widest">AI Platform</p>
                    </div>
                </div>

                {/* Nav items */}
                <nav className="px-3 pt-4 pb-2 space-y-1">
                    {navItems.map(({ to, icon: Icon, label }) => (
                        <NavLink
                            key={to}
                            to={to}
                            end={to === '/'}
                            onClick={() => { if (to === '/') setActiveId(null) }}
                            className={({ isActive }) => cn(
                                'flex items-center gap-3 px-3 py-2.5 rounded-lg text-sm font-medium transition-all',
                                isActive
                                    ? 'bg-primary-muted text-primary border border-primary/20'
                                    : 'text-text-secondary hover:text-text-primary hover:bg-surface-raised',
                            )}
                        >
                            {({ isActive }) => (
                                <>
                                    <Icon size={17} className={isActive ? 'text-primary' : 'text-text-muted'} />
                                    {label}
                                </>
                            )}
                        </NavLink>
                    ))}
                </nav>

                {/* Recents */}
                <div className="flex-1 overflow-hidden flex flex-col border-t border-surface-border mx-3 pt-3">

                    {/* Section header */}
                    <div className="flex items-center justify-between px-2 mb-1.5">
            <span className="text-[10px] font-medium text-text-muted uppercase tracking-wider">
              Recents
            </span>
                        <button
                            onClick={() => setSearchOpen(true)}
                            className="p-1 rounded-md text-text-muted hover:text-text-primary hover:bg-surface-raised transition-colors"
                            title="Search conversations"
                        >
                            <Search size={13} />
                        </button>
                    </div>

                    {/* Conversation items */}
                    <div className="flex-1 overflow-y-auto space-y-0.5">
                        {sidebarConversations.length === 0 ? (
                            <div className="flex flex-col items-center justify-center py-6 gap-2 px-2">
                                <MessageCircle size={20} className="text-text-muted opacity-40" />
                                <p className="text-[11px] text-text-muted text-center leading-relaxed">
                                    Your conversations will appear here
                                </p>
                            </div>
                        ) : (
                            sidebarConversations.map(conv => (
                                    <SidebarConversationItem
                                        key={conv.id}
                                        conversation={conv}
                                        isActive={conv.id === activeId}
                                        onSelect={() => handleConversationSelect(conv.id)}
                                        onRename={title => renameConversation(conv.id, title)}
                                        onPin={pinned => pinConversation(conv.id, pinned)}
                                        onDelete={async () => {
                                            await deleteConversation(conv.id)
                                            if (activeId === conv.id) setActiveId(null)
                                        }}
                                    />
                                )))}

                        {/* "See all" nudge */}
                        {conversations.length > MAX_SIDEBAR_CONVERSATIONS && (
                            <button
                                onClick={() => setSearchOpen(true)}
                                className="w-full px-2 py-1.5 text-[11px] text-text-muted hover:text-primary transition-colors text-left"
                            >
                                +{conversations.length - MAX_SIDEBAR_CONVERSATIONS} more — search all
                            </button>
                        )}
                    </div>
                </div>

                {/* User panel */}
                <div className="px-3 py-4 border-t border-surface-border">
                    {!confirmLogout ? (
                        <div className="flex items-center gap-2 px-2 py-2 rounded-xl hover:bg-surface-raised transition-colors group">
                            <div className="flex-shrink-0 flex items-center justify-center w-8 h-8 rounded-full bg-primary/20 text-primary text-xs font-semibold uppercase">
                                {username?.[0] ?? email?.[0] ?? '?'}
                            </div>
                            <div className="flex-1 min-w-0">
                                <p className="text-xs font-medium text-text-primary truncate">
                                    {username || email}
                                </p>
                                <span className={cn(
                                    'inline-block text-[10px] font-medium px-1.5 py-0.5 rounded-full mt-0.5',
                                    role === 'ADMIN'
                                        ? 'bg-primary/10 text-primary'
                                        : 'bg-surface-border text-text-muted',
                                )}>
                  {role}
                </span>
                            </div>
                            <button
                                onClick={() => setConfirmLogout(true)}
                                className="flex-shrink-0 p-1.5 rounded-lg text-text-muted hover:text-danger hover:bg-danger/10 transition-colors opacity-0 group-hover:opacity-100"
                                title="Sign out"
                            >
                                <LogOut size={15} />
                            </button>
                        </div>
                    ) : (
                        <div className="px-2 py-2 animate-fade-in">
                            <p className="text-xs text-text-secondary mb-3 text-center">Sign out of FinAssist?</p>
                            <div className="flex gap-2">
                                <button
                                    onClick={() => setConfirmLogout(false)}
                                    className="flex-1 py-1.5 text-xs font-medium text-text-secondary border border-surface-border rounded-lg hover:bg-surface-raised transition-colors"
                                >
                                    Cancel
                                </button>
                                <button
                                    onClick={logout}
                                    className="flex-1 flex items-center justify-center gap-1.5 py-1.5 text-xs font-medium text-white bg-danger rounded-lg hover:bg-danger/90 transition-colors"
                                >
                                    <LogOut size={12} />
                                    Sign out
                                </button>
                            </div>
                        </div>
                    )}
                </div>
            </aside>

            <ConversationSearchModal
                open={searchOpen}
                activeId={activeId}
                onSelect={handleConversationSelect}
                onClose={() => setSearchOpen(false)}
            />
        </>
    )
}