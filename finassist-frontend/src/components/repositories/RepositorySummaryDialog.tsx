import { useEffect, useState } from 'react'
import { X, Sparkles, Loader2, BookOpen } from 'lucide-react'
import ReactMarkdown from 'react-markdown'
import remarkGfm from 'remark-gfm'
import { repositoryService } from '@/services/repository.service'
import type { RepositorySummaryResponse } from '@/types'

interface Props {
    open:           boolean
    repositoryId:   number | null
    repositoryName: string
    onClose:        () => void
}

export default function RepositorySummaryDialog({ open, repositoryId, repositoryName, onClose }: Props) {
    const [data, setData]       = useState<RepositorySummaryResponse | null>(null)
    const [loading, setLoading] = useState(false)
    const [error, setError]     = useState<string | null>(null)

    // Fetch summary when the dialog opens
    useEffect(() => {
        if (!open || repositoryId == null) return
        setData(null)
        setError(null)
        setLoading(true)

        repositoryService.getSummary(repositoryId)
            .then(setData)
            .catch((e: { message?: string }) => setError(e.message ?? 'Failed to load summary'))
            .finally(() => setLoading(false))
    }, [open, repositoryId])

    if (!open) return null

    return (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4">
            {/* Backdrop */}
            <div
                className="absolute inset-0 bg-black/60 backdrop-blur-sm"
                onClick={!loading ? onClose : undefined}
            />

            {/* Panel */}
            <div className="relative bg-surface-card border border-surface-border rounded-2xl shadow-panel w-full max-w-2xl max-h-[85vh] flex flex-col animate-slide-up">

                {/* Header */}
                <div className="flex items-center justify-between gap-3 px-6 py-4 border-b border-surface-border">
                    <div className="flex items-center gap-2.5">
                        <div className="flex items-center justify-center w-8 h-8 rounded-lg bg-accent/10 border border-accent/20">
                            <Sparkles size={15} className="text-accent" />
                        </div>
                        <div>
                            <p className="text-sm font-medium text-text-primary">AI Summary</p>
                            <p className="text-[11px] text-text-muted">{repositoryName}</p>
                        </div>
                    </div>

                    <div className="flex items-center gap-3">
                        {data?.cached && (
                            <span className="text-[10px] text-text-muted bg-surface border border-surface-border rounded-full px-2 py-0.5">
                Cached
              </span>
                        )}
                        <button
                            onClick={onClose}
                            className="text-text-muted hover:text-text-primary transition-colors"
                        >
                            <X size={18} />
                        </button>
                    </div>
                </div>

                {/* Body */}
                <div className="flex-1 overflow-y-auto px-6 py-5">

                    {/* Loading */}
                    {loading && (
                        <div className="flex flex-col items-center justify-center py-16 gap-3">
                            <Loader2 size={28} className="animate-spin text-accent" />
                            <div className="text-center">
                                <p className="text-sm text-text-secondary">Generating summary…</p>
                                <p className="text-xs text-text-muted mt-1">This may take a few seconds</p>
                            </div>
                        </div>
                    )}

                    {/* Error */}
                    {error && !loading && (
                        <div className="flex flex-col items-center justify-center py-16 gap-3">
                            <p className="text-sm text-danger">{error}</p>
                        </div>
                    )}

                    {/* Summary content */}
                    {data && !loading && (
                        <div className="space-y-5">
                            <div className="prose prose-invert prose-sm max-w-none
                prose-headings:text-text-primary prose-headings:font-medium
                prose-p:text-text-secondary prose-p:leading-relaxed prose-p:my-2
                prose-li:text-text-secondary prose-li:my-0.5
                prose-strong:text-text-primary prose-strong:font-medium
                prose-code:text-accent prose-code:bg-surface prose-code:px-1 prose-code:rounded prose-code:text-xs
                prose-pre:bg-surface prose-pre:border prose-pre:border-surface-border prose-pre:text-xs
                prose-h1:text-base prose-h2:text-sm prose-h3:text-sm
                prose-ul:my-2 prose-ol:my-2
              ">
                                <ReactMarkdown remarkPlugins={[remarkGfm]}>
                                    {data.summary}
                                </ReactMarkdown>
                            </div>

                            {data.fileTree && (
                                <div>
                                    <p className="text-sm font-medium text-text-primary mb-2">File tree</p>
                                    <pre className="text-xs text-text-secondary bg-surface border border-surface-border rounded-xl p-3 overflow-x-auto whitespace-pre">
                    {data.fileTree}
                  </pre>
                                </div>
                            )}
                        </div>
                    )}
                </div>

                {/* Footer */}
                <div className="flex items-center justify-between px-6 py-3 border-t border-surface-border">
                    <div className="flex items-center gap-1.5 text-[11px] text-text-muted">
                        <BookOpen size={11} />
                        Generated by the RAG pipeline
                    </div>
                    <button
                        onClick={onClose}
                        className="px-4 py-1.5 text-sm font-medium text-text-secondary border border-surface-border rounded-xl hover:bg-surface-raised transition-colors"
                    >
                        Close
                    </button>
                </div>
            </div>
        </div>
    )
}