import { Loader2, Trash2, X } from 'lucide-react'

interface Props {
  open: boolean
  documentName: string
  onConfirm: () => Promise<void>
  onCancel: () => void
  isLoading: boolean
}

export default function DeleteDialog({ open, documentName, onConfirm, onCancel, isLoading }: Props) {
  if (!open) return null

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4">
      {/* Backdrop */}
      <div className="absolute inset-0 bg-black/60 backdrop-blur-sm" onClick={!isLoading ? onCancel : undefined} />

      {/* Panel */}
      <div className="relative bg-surface-card border border-surface-border rounded-2xl shadow-panel p-6 w-full max-w-sm animate-slide-up">
        <button
          onClick={onCancel}
          disabled={isLoading}
          className="absolute top-4 right-4 text-text-muted hover:text-text-primary disabled:opacity-40 transition-colors"
        >
          <X size={17} />
        </button>

        <div className="flex items-center justify-center w-11 h-11 rounded-xl bg-danger/10 border border-danger/20 mb-4">
          <Trash2 size={20} className="text-danger" />
        </div>

        <h3 className="text-base font-semibold text-text-primary mb-1">Delete document?</h3>
        <p className="text-sm text-text-secondary mb-1">
          <span className="font-medium text-text-primary">{documentName}</span> will be permanently removed along with all its indexed vectors.
        </p>
        <p className="text-xs text-text-muted mb-6">This action cannot be undone.</p>

        <div className="flex gap-3">
          <button
            onClick={onCancel}
            disabled={isLoading}
            className="flex-1 py-2 text-sm font-medium text-text-secondary border border-surface-border rounded-xl hover:bg-surface-raised disabled:opacity-40 transition-colors"
          >
            Cancel
          </button>
          <button
            onClick={onConfirm}
            disabled={isLoading}
            className="flex-1 flex items-center justify-center gap-2 py-2 text-sm font-medium text-white bg-danger rounded-xl hover:bg-danger/90 disabled:opacity-60 transition-colors"
          >
            {isLoading ? <Loader2 size={14} className="animate-spin" /> : <Trash2 size={14} />}
            Delete
          </button>
        </div>
      </div>
    </div>
  )
}
