import UploadZone    from '@/components/documents/UploadZone'
import DocumentList  from '@/components/documents/DocumentList'
import ToastContainer from '@/components/ui/ToastContainer'
import { useDocuments } from '@/hooks/useDocuments'
import { useToast }     from '@/hooks/useToast'
import { Lock }         from 'lucide-react'

export default function DocumentsPage() {
  const toast = useToast()
  const {
    documents, isLoading, isError,
    uploadDocument, isUploading,
    reindexDocument, reindexingId,
    deleteDocument,  deletingId,
  } = useDocuments()

  const handleUpload = async (file: File, onProgress: (pct: number) => void) => {
    try {
      await uploadDocument({ file, onProgress })
      toast.success(`"${file.name}" uploaded and queued for indexing`)
    } catch (e: unknown) {
      toast.error((e as { message?: string }).message ?? 'Upload failed')
      throw e
    }
  }

  const handleReindex = async (id: string) => {
    try {
      await reindexDocument(id)
      toast.success('Re-indexing started')
    } catch {
      toast.error('Reindex failed — try again')
    }
  }

  const handleDelete = async (id: string) => {
    try {
      await deleteDocument(id)
      toast.success('Document deleted')
    } catch {
      toast.error('Delete failed — try again')
    }
  }

  return (
      <div className="px-4 md:px-8 py-8 max-w-6xl mx-auto space-y-8">

        {/* Page header */}
        <div className="flex items-start justify-between gap-4">
          <div>
            <h2 className="text-xl font-semibold text-text-primary">My Documents</h2>
            <p className="text-sm text-text-secondary mt-1">
              Upload PDFs to index them into your personal vector store.
            </p>
          </div>

          {/* Privacy badge */}
          <div className="hidden sm:flex items-center gap-1.5 text-[11px] text-text-muted bg-surface border border-surface-border rounded-full px-3 py-1.5 flex-shrink-0">
            <Lock size={10} />
            Private to your account
          </div>
        </div>

        {/* Upload zone */}
        <UploadZone onUpload={handleUpload} isUploading={isUploading} />

        {/* Document count */}
        {!isLoading && !isError && documents.length > 0 && (
            <p className="text-sm text-text-secondary">
              {documents.length} {documents.length === 1 ? 'document' : 'documents'} in your library
            </p>
        )}

        {/* List */}
        <DocumentList
            documents={documents}
            isLoading={isLoading}
            isError={isError}
            onReindex={handleReindex}
            onDelete={handleDelete}
        />

        <ToastContainer toasts={toast.toasts} onDismiss={toast.dismiss} />
      </div>
  )
}