import { useState } from 'react'
import { NavLink, useLocation } from 'react-router-dom'
import {Menu, X, MessageSquare, FileText, Zap, LogOut, GitBranch} from 'lucide-react'
import { cn } from '@/utils/cn'
import { useAuth } from '@/context/AuthContext'

const titles: Record<string, string> = {
  '/':          'AI Assistant',
  '/documents': 'Documents',
  '/repositories': 'Repositories',
}

export default function Navbar() {
  const [open, setOpen]           = useState(false)
  const [confirming, setConfirming] = useState(false)
  const { pathname }              = useLocation()
  const { isAdmin, username, email, role, logout } = useAuth()

  const navItems = [
    { to: '/',          icon: MessageSquare, label: 'AI Assistant', show: true    },
    { to: '/documents', icon: FileText,      label: 'Documents',    show: isAdmin },
    { to: '/repositories', icon: GitBranch,     label: 'Repositories',  show: isAdmin  },
  ].filter((i) => i.show)

  const title = titles[pathname] ?? 'FinAssist'

  const handleLogout = () => {
    setOpen(false)
    setConfirming(false)
    logout()
  }

  return (
      <>
        <header className="sticky top-0 z-30 flex items-center gap-4 px-4 md:px-6 h-14 bg-surface-card/80 backdrop-blur-sm border-b border-surface-border">
          {/* Mobile logo */}
          <div className="flex items-center gap-2 md:hidden">
            <div className="flex items-center justify-center w-7 h-7 rounded-md bg-primary">
              <Zap size={14} className="text-white" />
            </div>
            <span className="text-sm font-semibold text-text-primary">FinAssist</span>
          </div>

          {/* Desktop page title */}
          <h1 className="hidden md:block text-sm font-medium text-text-secondary">{title}</h1>

          <div className="flex-1" />

          {/* Desktop logout */}
          <button
              onClick={() => setConfirming(true)}
              className="hidden md:flex items-center gap-1.5 px-3 py-1.5 text-xs text-text-muted hover:text-danger hover:bg-danger/10 rounded-lg border border-transparent hover:border-danger/20 transition-all"
              title="Sign out"
          >
            <LogOut size={14} />
            Sign out
          </button>

          {/* Mobile hamburger */}
          <button
              className="md:hidden p-1.5 rounded-lg text-text-secondary hover:text-text-primary hover:bg-surface-raised transition-colors"
              onClick={() => { setOpen((v) => !v); setConfirming(false) }}
              aria-label="Toggle menu"
          >
            {open ? <X size={20} /> : <Menu size={20} />}
          </button>
        </header>

        {/* Desktop logout confirm overlay */}
        {confirming && (
            <div className="fixed inset-0 z-50 flex items-center justify-center p-4">
              <div className="absolute inset-0 bg-black/60 backdrop-blur-sm" onClick={() => setConfirming(false)} />
              <div className="relative bg-surface-card border border-surface-border rounded-2xl shadow-panel p-6 w-full max-w-xs animate-slide-up">
                <div className="flex items-center justify-center w-11 h-11 rounded-xl bg-danger/10 border border-danger/20 mb-4 mx-auto">
                  <LogOut size={20} className="text-danger" />
                </div>
                <h3 className="text-base font-semibold text-text-primary text-center mb-1">Sign out?</h3>
                <p className="text-xs text-text-muted text-center mb-6">
                  You will be redirected to the Keycloak login page.
                </p>
                <div className="flex gap-3">
                  <button
                      onClick={() => setConfirming(false)}
                      className="flex-1 py-2 text-sm font-medium text-text-secondary border border-surface-border rounded-xl hover:bg-surface-raised transition-colors"
                  >
                    Cancel
                  </button>
                  <button
                      onClick={handleLogout}
                      className="flex-1 flex items-center justify-center gap-2 py-2 text-sm font-medium text-white bg-danger rounded-xl hover:bg-danger/90 transition-colors"
                  >
                    <LogOut size={14} />
                    Sign out
                  </button>
                </div>
              </div>
            </div>
        )}

        {/* Mobile slide-down menu */}
        {open && (
            <div className="md:hidden fixed inset-x-0 top-14 z-20 bg-surface-card border-b border-surface-border animate-slide-up shadow-panel">
              <nav className="px-4 py-3 space-y-1">
                {navItems.map(({ to, icon: Icon, label }) => (
                    <NavLink
                        key={to}
                        to={to}
                        end={to === '/'}
                        onClick={() => setOpen(false)}
                        className={({ isActive }) =>
                            cn(
                                'flex items-center gap-3 px-3 py-2.5 rounded-lg text-sm font-medium transition-colors',
                                isActive
                                    ? 'bg-primary-muted text-primary'
                                    : 'text-text-secondary hover:text-text-primary hover:bg-surface-raised',
                            )
                        }
                    >
                      <Icon size={17} />
                      {label}
                    </NavLink>
                ))}
              </nav>

              {/* Mobile user info + logout */}
              <div className="px-4 pb-4 pt-2 border-t border-surface-border">
                <div className="flex items-center gap-3 px-3 py-2 mb-2">
                  <div className="flex items-center justify-center w-8 h-8 rounded-full bg-primary/20 text-primary text-xs font-semibold uppercase">
                    {username?.[0] ?? email?.[0] ?? '?'}
                  </div>
                  <div className="min-w-0">
                    <p className="text-xs font-medium text-text-primary truncate">{username || email}</p>
                    <p className="text-[10px] text-text-muted">{role}</p>
                  </div>
                </div>
                <button
                    onClick={() => { setOpen(false); setConfirming(true) }}
                    className="w-full flex items-center justify-center gap-2 py-2 text-sm font-medium text-danger border border-danger/20 bg-danger/10 rounded-xl hover:bg-danger/20 transition-colors"
                >
                  <LogOut size={15} />
                  Sign out
                </button>
              </div>
            </div>
        )}
      </>
  )
}