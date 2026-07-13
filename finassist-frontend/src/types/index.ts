// ─── Document types ────────────────────────────────────────────────────────

export type DocumentStatus = 'PENDING' | 'PROCESSING' | 'INDEXED' | 'FAILED'

export interface FinDocument {
  documentId: string
  name: string
  uploadedAt: string      // ISO-8601 returned by Spring Boot
  status: DocumentStatus
  pageCount?: number
  sizeBytes?: number
}

// ─── Chat types ─────────────────────────────────────────────────────────────

export interface SourceReference {
  documentName: string
  pageNumber:   number
  documentId?:  string
  chunkId?:     string
  excerpt?:     string
}

export interface ChatRequest {
  question: string
  topK?: number
}

export interface ChatResponse {
  answer: string
  sources: SourceReference[]
}

// ─── UI-only types ──────────────────────────────────────────────────────────

export type MessageRole = 'user' | 'assistant'

export interface ChatMessage {
  id: string
  role: MessageRole
  content: string
  sources?: SourceReference[]
  timestamp: Date
  isLoading?: boolean
}

// ─── API error shape ────────────────────────────────────────────────────────

export interface ApiError {
  message: string
  status?: number
}
