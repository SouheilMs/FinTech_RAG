import { useState } from 'react'
import { FileText, RefreshCw, Trash2, Loader2, Clock, CheckCircle, AlertCircle, Hourglass } from 'lucide-react'
import { cn } from '@/utils/cn'
import { formatDate, formatBytes } from '@/utils/format'
import DeleteDialog from './DeleteDialog'
import type { FinDocument, DocumentStatus } from '@/types'

interface Props {
  doc: FinDocument
  onReindex: (id: string) => Promise<void>
  onDelete:  (id: string) => Promise<void>
}

const statusConfig: Record<DocumentStatus, { label: string; color: string; Icon: React.ElementType }> = {
  PENDING:    { label: 'Pending',    color: 'text-warning  bg-warning/10  border-warning/20',  Icon: Clock },
  PROCESSING: { label: 'Processing', color: 'text-accent   bg-accent/10   border-accent/20',   Icon: Hourglass },
  INDEXED:    { label: 'Indexed',    color: 'text-success  bg-success/10  border-success/20',  Icon: CheckCircle },
  FAILED:     { label: 'Failed',     color: 'text-danger   bg-danger/10   border-danger/20',   Icon: AlertCircle },
}

export default function DocumentCard({ doc, onReindex, onDelete }: Props) {
  const [showDelete,   setShowDelete]   = useState(false)
  const [isReindexing, setIsReindexing] = useState(false)  // ✅ local state per card
  const [isDeleting,   setIsDeleting]   = useState(false)  // ✅ local state per card

  const cfg = statusConfig[doc.status] ?? statusConfig.PENDING
  const StatusIcon = cfg.Icon

  const handleReindex = async () => {
    setIsReindexing(true)
    try {
      await onReindex(doc.documentId)
    } finally {
      setIsReindexing(false)  // ✅ always resets, even on error
    }
  }

  const handleDelete = async () => {
    setIsDeleting(true)
    try {
      await onDelete(doc.documentId)
    } finally {
      setIsDeleting(false)
      setShowDelete(false)
    }
  }

  return (
      <div>
        <div className="flex flex-col gap-3 p-4 bg-surface-card border border-surface-border rounded-xl hover:border-primary/20 hover:shadow-card transition-all duration-200 animate-fade-in">
          {/* Header row */}
          <div className="flex items-start gap-3">
            <div className="flex-shrink-0 flex items-center justify-center w-9 h-9 rounded-lg bg-surface border border-surface-border">
              <FileText size={17} className="text-text-muted" />
            </div>
            <div className="flex-1 min-w-0">
              <p className="text-sm font-medium text-text-primary truncate" title={doc.name}>
                {doc.name}
              </p>
              <p className="text-xs text-text-muted mt-0.5">{formatDate(doc.uploadedAt)}</p>
            </div>
            <span className={cn('flex-shrink-0 flex items-center gap-1 text-[11px] font-medium px-2 py-0.5 rounded-full border', cfg.color)}>
            <StatusIcon size={10} />
              {cfg.label}
          </span>
          </div>

          {/* Meta row */}
          {doc.sizeBytes && (
              <p className="text-[11px] text-text-muted px-0.5">
                {formatBytes(doc.sizeBytes)}{doc.pageCount ? ` · ${doc.pageCount} pages` : ''}
              </p>
          )}

          {/* Actions */}
          <div className="flex gap-2 pt-1 border-t border-surface-border">
            <button
                onClick={handleReindex}
                disabled={isReindexing || isDeleting}
                className="flex-1 flex items-center justify-center gap-1.5 py-1.5 text-xs font-medium text-text-secondary rounded-lg border border-surface-border hover:text-primary hover:border-primary/30 hover:bg-primary-muted disabled:opacity-40 disabled:cursor-not-allowed transition-all"
            >
              {isReindexing ? <Loader2 size={13} className="animate-spin" /> : <RefreshCw size={13} />}
              Reindex
            </button>

            <button
                onClick={() => setShowDelete(true)}
                disabled={isReindexing || isDeleting}
                className="flex-1 flex items-center justify-center gap-1.5 py-1.5 text-xs font-medium text-text-secondary rounded-lg border border-surface-border hover:text-danger hover:border-danger/30 hover:bg-danger/10 disabled:opacity-40 disabled:cursor-not-allowed transition-all"
            >
              {isDeleting ? <Loader2 size={13} className="animate-spin" /> : <Trash2 size={13} />}
              Delete
            </button>
          </div>
        </div>

        <DeleteDialog
            open={showDelete}
            documentName={doc.name}
            onConfirm={handleDelete}
            onCancel={() => setShowDelete(false)}
            isLoading={isDeleting}
        />
      </div>
  )
}