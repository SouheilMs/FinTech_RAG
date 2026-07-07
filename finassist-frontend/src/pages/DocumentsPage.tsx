import UploadZone from '@/components/documents/UploadZone'
import DocumentList from '@/components/documents/DocumentList'
import ToastContainer from '@/components/ui/ToastContainer'
import { useDocuments } from '@/hooks/useDocuments'
import { useToast } from '@/hooks/useToast'

export default function DocumentsPage() {
  const toast = useToast()
  const {
    documents, isLoading, isError,
    uploadDocument, isUploading,
    reindexDocument, reindexingId,
    deleteDocument, deletingId,
  } = useDocuments()

  const handleUpload = async (file: File, onProgress: (pct: number) => void) => {
    try {
      await uploadDocument({ file, onProgress })
      toast.success(`"${file.name}" uploaded and queued for indexing.`)
    } catch (e: unknown) {
      toast.error((e as { message?: string }).message ?? 'Upload failed')
      throw e  // re-throw so UploadZone shows error state
    }
  }

  const handleReindex = async (id: string) => {
    try {
      await reindexDocument(id)
      toast.success('Reindex started.')
    } catch {
      toast.error('Reindex failed. Try again.')
    }
  }

  const handleDelete = async (id: string) => {
    try {
      await deleteDocument(id)
      toast.success('Document deleted.')
    } catch {
      toast.error('Delete failed. Try again.')
    }
  }

  return (
    <div className="px-4 md:px-8 py-8 max-w-6xl mx-auto space-y-8">
      {/* Page header */}
      <div>
        <h2 className="text-xl font-semibold text-text-primary">Documents</h2>
        <p className="text-sm text-text-secondary mt-1">
          Upload PDFs to index them into the vector store. Then ask questions on the AI Assistant page.
        </p>
      </div>

      {/* Upload */}
      <UploadZone onUpload={handleUpload} isUploading={isUploading} />

      {/* Document count */}
      {!isLoading && !isError && (
        <div className="flex items-center justify-between">
          <h3 className="text-sm font-medium text-text-secondary">
            {documents.length === 0
              ? 'No documents'
              : `${documents.length} document${documents.length !== 1 ? 's' : ''}`}
          </h3>
        </div>
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
