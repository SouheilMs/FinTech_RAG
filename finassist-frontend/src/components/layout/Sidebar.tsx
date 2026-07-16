import { NavLink } from 'react-router-dom'
import {MessageSquare, FileText, Zap, LogOut, ChevronDown, GitBranch, Users} from 'lucide-react'
import { cn } from '@/utils/cn'
import { useAuth } from '@/context/AuthContext'
import { useState } from 'react'

export default function Sidebar() {
    const { username, email, role, isAdmin, logout } = useAuth()
    const [confirmLogout, setConfirmLogout] = useState(false)

    const navItems = [
        { to: '/', icon: MessageSquare, label: 'AI Assistant', show: true     },
        { to: '/documents', icon: FileText, label: 'Documents', show: isAdmin  },
        { to: '/repositories', icon: GitBranch, label: 'Repositories', show: isAdmin  },
        { to: '/users', icon: Users, label: 'Users', show: isAdmin  },
    ].filter((i) => i.show)

    return (
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

            {/* Nav */}
            <nav className="flex-1 px-3 py-4 space-y-1">
                {navItems.map(({ to, icon: Icon, label }) => (
                    <NavLink
                        key={to}
                        to={to}
                        end={to === '/'}
                        className={({ isActive }) =>
                            cn(
                                'flex items-center gap-3 px-3 py-2.5 rounded-lg text-sm font-medium transition-all',
                                isActive
                                    ? 'bg-primary-muted text-primary border border-primary/20'
                                    : 'text-text-secondary hover:text-text-primary hover:bg-surface-raised',
                            )
                        }
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

            {/* User panel */}
            <div className="px-3 py-4 border-t border-surface-border">
                {!confirmLogout ? (
                    /* Normal state — user info + logout button */
                    <div className="flex items-center gap-2 px-2 py-2 rounded-xl hover:bg-surface-raised transition-colors group">
                        {/* Avatar */}
                        <div className="flex-shrink-0 flex items-center justify-center w-8 h-8 rounded-full bg-primary/20 text-primary text-xs font-semibold uppercase">
                            {username?.[0] ?? email?.[0] ?? '?'}
                        </div>

                        {/* Info */}
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
                    </div>
                ) : (
                    /* Confirm state */
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
    )
}