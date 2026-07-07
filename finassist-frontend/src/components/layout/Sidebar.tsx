import { NavLink } from 'react-router-dom'
import { MessageSquare, FileText, Zap } from 'lucide-react'
import { cn } from '@/utils/cn'

const navItems = [
  { to: '/',          icon: MessageSquare, label: 'AI Assistant' },
  { to: '/documents', icon: FileText,      label: 'Documents'    },
]

export default function Sidebar() {
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
                'flex items-center gap-3 px-3 py-2.5 rounded-lg text-sm font-medium transition-all duration-150',
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

      {/* Footer */}
      <div className="px-5 py-4 border-t border-surface-border">
        <p className="text-[10px] text-text-muted">Spring AI · pgvector · Ollama</p>
        <p className="text-[10px] text-text-muted mt-0.5">v0.1.0</p>
      </div>
    </aside>
  )
}
