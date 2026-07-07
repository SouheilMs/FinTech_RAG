import api from './api'
import type { FinDocument } from '@/types'

export const documentService = {

  async uploadDocument(
    file: File,
    onProgress?: (percent: number) => void,
  ): Promise<FinDocument> {
    const form = new FormData()
    form.append('file', file)

    const { data } = await api.post<FinDocument>('/documents/upload', form, {
      headers: { 'Content-Type': 'multipart/form-data' },
      onUploadProgress: (e) => {
        if (onProgress && e.total) {
          onProgress(Math.round((e.loaded / e.total) * 100))
        }
      },
    })
    return data
  },

  async getDocuments(): Promise<FinDocument[]> {
    const { data } = await api.get<FinDocument[]>('/documents')
    return data
  },

  async reindexDocument(id: string): Promise<FinDocument> {
    const { data } = await api.post<FinDocument>(`/documents/${id}/reindex`)
    return data
  },

  async deleteDocument(id: string): Promise<void> {
    await api.delete(`/documents/${id}`)
  },
}
