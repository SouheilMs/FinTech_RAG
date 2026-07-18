import DocumentCard from './DocumentCard'
import type { FinDocument } from '@/types'

interface Props {
  documents: FinDocument[]
  isLoading: boolean
  isError: boolean
  onReindex: (id: string) => Promise<void>
  onDelete:  (id: string) => Promise<void>
}

function Skeleton() {
  return (
      <div className="flex flex-col gap-3 p-4 bg-surface-card border border-surface-border rounded-xl animate-pulse">
        <div className="flex items-start gap-3">
          <div className="w-9 h-9 rounded-lg bg-surface-raised" />
          <div className="flex-1 space-y-2">
            <div className="h-3.5 bg-surface-raised rounded w-3/4" />
            <div className="h-2.5 bg-surface-raised rounded w-1/2" />
          </div>
          <div className="h-5 w-16 bg-surface-raised rounded-full" />
        </div>
        <div className="h-2 bg-surface-raised rounded w-1/3 mt-1" />
        <div className="flex gap-2 pt-2 border-t border-surface-border">
          <div className="flex-1 h-7 bg-surface-raised rounded-lg" />
          <div className="flex-1 h-7 bg-surface-raised rounded-lg" />
        </div>
      </div>
  )
}

export default function DocumentList({
                                       documents, isLoading, isError, onReindex, onDelete,
                                     }: Props) {
  if (isLoading) {
    return (
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
          {Array.from({ length: 6 }).map((_, i) => (
              <Skeleton key={`skeleton-${i}`} />
          ))}
        </div>
    )
  }

  if (isError) {
    return (
        <div className="flex flex-col items-center justify-center py-16 text-center gap-3">
          <p className="text-sm font-medium text-danger">Failed to load documents</p>
          <p className="text-xs text-text-muted">Check that the Spring Boot backend is running on port 8080.</p>
        </div>
    )
  }

  if (documents.length === 0) {
    return (
        <div className="flex flex-col items-center justify-center py-20 text-center gap-3">
          <p className="text-sm font-medium text-text-secondary">No documents yet</p>
          <p className="text-xs text-text-muted">Upload a PDF above to get started.</p>
        </div>
    )
  }

  return (
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
        {documents.map((doc, index) => (
            <DocumentCard
                key={doc.name}
                doc={doc}
                onReindex={onReindex}
                onDelete={onDelete}
            />
        ))}
      </div>
  )
}