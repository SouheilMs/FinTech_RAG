// ─── Document types ────────────────────────────────────────────────────────

export type DocumentStatus = 'PENDING' | 'PROCESSING' | 'INDEXED' | 'FAILED'

export interface FinDocument {
  documentId: string
  name: string
  ownerId : string
  ownerUsername : string
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

export type RepositoryStatus = 'PENDING' | 'CLONING' | 'INDEXING' | 'COMPLETED' | 'FAILED'

export interface GitRepository {
  id:           number
  url:          string
  name:         string
  owner:        string
  branch:       string | null
  commitHash:   string | null
  status:       RepositoryStatus
  errorMessage: string | null
  indexedFiles: number
  totalChunks:  number
  hasSummary:   boolean
  createdAt:    string
  indexedAt:    string | null
}

export interface RepositorySubmitResponse {
  repositoryId: number
  status:       string
}

export interface RepositorySummaryResponse {
  repositoryId:    number
  repositoryName:  string
  summary:         string
  cached:          boolean
}

// ─── API error shape ────────────────────────────────────────────────────────

export interface ApiError {
  message: string
  status?: number
}
