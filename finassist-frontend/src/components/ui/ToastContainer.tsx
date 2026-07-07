import { CheckCircle, AlertCircle, Info, X } from 'lucide-react'
import { cn } from '@/utils/cn'
import type { Toast } from '@/hooks/useToast'

interface Props {
  toasts: Toast[]
  onDismiss: (id: string) => void
}

const icons = {
  success: <CheckCircle size={15} className="text-success flex-shrink-0" />,
  error:   <AlertCircle size={15} className="text-danger flex-shrink-0" />,
  info:    <Info size={15} className="text-primary flex-shrink-0" />,
}

const colors = {
  success: 'border-success/20',
  error:   'border-danger/20',
  info:    'border-primary/20',
}

export default function ToastContainer({ toasts, onDismiss }: Props) {
  if (!toasts.length) return null

  return (
    <div className="fixed bottom-5 right-5 z-50 flex flex-col gap-2 w-80">
      {toasts.map((t) => (
        <div
          key={t.id}
          className={cn(
            'flex items-start gap-3 px-4 py-3 bg-surface-card border rounded-xl shadow-panel animate-slide-up',
            colors[t.type],
          )}
        >
          {icons[t.type]}
          <p className="flex-1 text-sm text-text-primary">{t.message}</p>
          <button onClick={() => onDismiss(t.id)} className="text-text-muted hover:text-text-secondary transition-colors">
            <X size={14} />
          </button>
        </div>
      ))}
    </div>
  )
}
