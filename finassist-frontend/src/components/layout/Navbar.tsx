import { useState } from 'react'
import { NavLink, useLocation } from 'react-router-dom'
import { Menu, X, MessageSquare, FileText, Zap } from 'lucide-react'
import { cn } from '@/utils/cn'

const navItems = [
  { to: '/',          icon: MessageSquare, label: 'AI Assistant' },
  { to: '/documents', icon: FileText,      label: 'Documents'    },
]

const titles: Record<string, string> = {
  '/':          'AI Assistant',
  '/documents': 'Documents',
}

export default function Navbar() {
  const [open, setOpen] = useState(false)
  const { pathname } = useLocation()
  const title = titles[pathname] ?? 'FinAssist'

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

        {/* Mobile hamburger */}
        <button
          className="md:hidden p-1.5 rounded-lg text-text-secondary hover:text-text-primary hover:bg-surface-raised transition-colors"
          onClick={() => setOpen((v) => !v)}
          aria-label="Toggle menu"
        >
          {open ? <X size={20} /> : <Menu size={20} />}
        </button>
      </header>

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
        </div>
      )}
    </>
  )
}
