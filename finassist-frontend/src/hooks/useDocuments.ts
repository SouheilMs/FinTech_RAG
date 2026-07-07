import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { documentService } from '@/services/document.service'
import type { FinDocument } from '@/types'

const QUERY_KEY = ['documents'] as const

export function useDocuments() {
  const qc = useQueryClient()

  const {
    data: documents = [],
    isLoading,
    isError,
    error,
    refetch,
  } = useQuery<FinDocument[]>({
    queryKey: QUERY_KEY,
    queryFn: documentService.getDocuments,
    staleTime: 30_000,
  })

  const uploadMutation = useMutation({
    mutationFn: ({
                   file,
                   onProgress,
                 }: {
      file: File
      onProgress?: (pct: number) => void
    }) => documentService.uploadDocument(file, onProgress),
    onSuccess: () => qc.invalidateQueries({ queryKey: QUERY_KEY }),
  })

  const reindexMutation = useMutation({
    mutationFn: (id: string) => documentService.reindexDocument(id),
    onSuccess: () => qc.invalidateQueries({ queryKey: QUERY_KEY }),
  })

  const deleteMutation = useMutation({
    mutationFn: (id: string) => documentService.deleteDocument(id),
    onSuccess: () => qc.invalidateQueries({ queryKey: QUERY_KEY }),
  })

  return {
    documents,
    isLoading,
    isError,
    error,
    refetch,
    uploadDocument:  uploadMutation.mutateAsync,
    isUploading:     uploadMutation.isPending,
    reindexDocument: reindexMutation.mutateAsync,
    isReindexing:    reindexMutation.isPending,
    // ✅ only expose the ID while the mutation is actually running
    reindexingId:    reindexMutation.isPending
        ? (reindexMutation.variables as string | undefined)
        : undefined,
    deleteDocument:  deleteMutation.mutateAsync,
    isDeleting:      deleteMutation.isPending,
    // ✅ same fix for delete
    deletingId:      deleteMutation.isPending
        ? (deleteMutation.variables as string | undefined)
        : undefined,
  }
}