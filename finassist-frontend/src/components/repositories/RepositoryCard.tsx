import { useState } from 'react'
import {
    GitBranch, GitCommit, ExternalLink,
    RefreshCw, Trash2, Sparkles,
    Loader2, FileCode, Database, ChevronDown, ChevronUp,
} from 'lucide-react'
import { cn } from '@/utils/cn'
import { formatDate } from '@/utils/format'
import RepositoryStatusBadge from './RepositoryStatusBadge'
import type { GitRepository } from '@/types'

interface Props {
    repo:        GitRepository
    onSummary:   (id: number) => void
    onReindex:   (id: number) => Promise<void>
    onDelete:    (id: number) => Promise<void>
    isReindexing: boolean
    isDeleting:   boolean
}

const IN_PROGRESS = ['PENDING', 'CLONING', 'INDEXING']

const progressLabel: Record<string, string> = {
    PENDING:  'Queued for processing…',
    CLONING:  'Cloning repository…',
    INDEXING: 'Embedding and indexing files…',
}

export default function RepositoryCard({ repo, onSummary, onReindex, onDelete,
                                           isReindexing, isDeleting  }: Props) {
    const [confirmDelete, setConfirmDelete]   = useState(false)
    const [confirmReindex, setConfirmReindex] = useState(false)
    const [expanded, setExpanded]             = useState(false)

    const inProgress = IN_PROGRESS.includes(repo.status)
    const isCompleted = repo.status === 'COMPLETED'
    const isFailed    = repo.status === 'FAILED'
    const busy        = isReindexing || isDeleting

    const shortHash = repo.commitHash ? repo.commitHash.slice(0, 7) : null

    return (
        <div className={cn(
            'flex flex-col bg-surface-card border rounded-xl overflow-hidden transition-all duration-200 animate-fade-in',
            isFailed     ? 'border-danger/30'   : 'border-surface-border hover:border-primary/20',
            isCompleted  ? 'hover:shadow-card'  : '',
        )}>

            {/* In-progress banner */}
            {inProgress && (
                <div className="h-0.5 w-full bg-surface-raised overflow-hidden">
                    <div className="h-full bg-accent animate-pulse w-full" />
                </div>
            )}

            <div className="p-4 flex flex-col gap-3">

                {/* Header */}
                <div className="flex items-start justify-between gap-2">
                    <div className="flex items-start gap-2.5 min-w-0">
                        <div className="flex-shrink-0 flex items-center justify-center w-8 h-8 rounded-lg bg-surface border border-surface-border mt-0.5">
                            <GitBranch size={15} className="text-text-muted" />
                        </div>
                        <div className="min-w-0">
                            <p className="text-sm font-medium text-text-primary truncate">
                                {repo.owner}/{repo.name}
                            </p>
                            <span className="truncate max-w-[200px]">
                  {repo.url.replace('https://', '')}
                </span>
                    </div>
                </div>
                <RepositoryStatusBadge status={repo.status} />
            </div>

            {/* Progress message */}
            {inProgress && (
                <div className="flex items-center gap-2 text-xs text-text-secondary bg-surface rounded-lg px-3 py-2">
                    <Loader2 size={12} className="animate-spin text-accent flex-shrink-0" />
                    {progressLabel[repo.status]}
                </div>
            )}

            {/* Error message */}
            {isFailed && repo.errorMessage && (
                <div className="text-xs text-danger bg-danger/10 border border-danger/20 rounded-lg px-3 py-2 leading-relaxed">
                    {repo.errorMessage}
                </div>
            )}

            {/* Stats — only when completed */}
            {isCompleted && (
                <div className="grid grid-cols-2 gap-2">
                    <div className="flex items-center gap-1.5 bg-surface rounded-lg px-3 py-2">
                        <FileCode size={13} className="text-text-muted" />
                        <div>
                            <p className="text-xs font-medium text-text-primary">{repo.indexedFiles}</p>
                            <p className="text-[10px] text-text-muted">files</p>
                        </div>
                    </div>
                    <div className="flex items-center gap-1.5 bg-surface rounded-lg px-3 py-2">
                        <Database size={13} className="text-text-muted" />
                        <div>
                            <p className="text-xs font-medium text-text-primary">{repo.totalChunks}</p>
                            <p className="text-[10px] text-text-muted">chunks</p>
                        </div>
                    </div>
                </div>
            )}

            {/* Expandable details */}
            {(repo.branch || shortHash || repo.createdAt) && (
                <button
                    onClick={() => setExpanded(v => !v)}
                    className="flex items-center justify-between text-[11px] text-text-muted hover:text-text-secondary transition-colors"
                >
                    <span>Details</span>
                    {expanded ? <ChevronUp size={12} /> : <ChevronDown size={12} />}
                </button>
            )}

            {expanded && (
                <div className="space-y-1.5 animate-fade-in">
                    {repo.branch && (
                        <div className="flex items-center gap-2 text-[11px] text-text-secondary">
                            <GitBranch size={11} className="text-text-muted" />
                            <span>{repo.branch}</span>
                        </div>
                    )}
                    {shortHash && (
                        <div className="flex items-center gap-2 text-[11px] text-text-secondary font-mono">
                            <GitCommit size={11} className="text-text-muted" />
                            <span>{shortHash}</span>
                        </div>
                    )}
                    {repo.createdAt && (
                        <p className="text-[11px] text-text-muted">
                            Added {formatDate(repo.createdAt)}
                        </p>
                    )}
                    {repo.indexedAt && (
                        <p className="text-[11px] text-text-muted">
                            Indexed {formatDate(repo.indexedAt)}
                        </p>
                    )}
                </div>
            )}

            {/* Actions */}
            <div className="flex gap-2 pt-1 border-t border-surface-border">

                {/* Summary */}
                <button
                    onClick={() => onSummary(repo.id)}
                    disabled={!isCompleted || busy}
                    className="flex-1 flex items-center justify-center gap-1 py-1.5 text-xs font-medium text-text-secondary rounded-lg border border-surface-border hover:text-accent hover:border-accent/30 hover:bg-accent/5 disabled:opacity-40 disabled:cursor-not-allowed transition-all"
                >
                    <Sparkles size={12} />
                    Summary
                </button>

                {/* Reindex */}
                {!confirmReindex ? (
                    <button
                        onClick={() => setConfirmReindex(true)}
                        disabled={inProgress || busy}
                        className="flex-1 flex items-center justify-center gap-1 py-1.5 text-xs font-medium text-text-secondary rounded-lg border border-surface-border hover:text-primary hover:border-primary/30 hover:bg-primary-muted disabled:opacity-40 disabled:cursor-not-allowed transition-all"
                    >
                        {isReindexing
                            ? <Loader2 size={12} className="animate-spin" />
                            : <RefreshCw size={12} />
                        }
                        Reindex
                    </button>
                ) : (
                    <div className="flex-1 flex gap-1">
                        <button
                            onClick={() => setConfirmReindex(false)}
                            className="flex-1 py-1.5 text-[11px] text-text-muted border border-surface-border rounded-lg hover:bg-surface-raised transition-colors"
                        >
                            Cancel
                        </button>
                        <button
                            onClick={async () => { await onReindex(repo.id); setConfirmReindex(false) }}
                            className="flex-1 py-1.5 text-[11px] text-white bg-primary rounded-lg hover:bg-primary-hover transition-colors"
                        >
                            Confirm
                        </button>
                    </div>
                )}

                {/* Delete */}
                {!confirmDelete ? (
                    <button
                        onClick={() => setConfirmDelete(true)}
                        disabled={busy}
                        className="flex-1 flex items-center justify-center gap-1 py-1.5 text-xs font-medium text-text-secondary rounded-lg border border-surface-border hover:text-danger hover:border-danger/30 hover:bg-danger/10 disabled:opacity-40 disabled:cursor-not-allowed transition-all"
                    >
                        {isDeleting
                            ? <Loader2 size={12} className="animate-spin" />
                            : <Trash2 size={12} />
                        }
                        Delete
                    </button>
                ) : (
                    <div className="flex-1 flex gap-1">
                        <button
                            onClick={() => setConfirmDelete(false)}
                            className="flex-1 py-1.5 text-[11px] text-text-muted border border-surface-border rounded-lg hover:bg-surface-raised transition-colors"
                        >
                            Cancel
                        </button>
                        <button
                            onClick={async () => { await onDelete(repo.id); setConfirmDelete(false) }}
                            className="flex-1 py-1.5 text-[11px] text-white bg-danger rounded-lg hover:bg-danger/90 transition-colors"
                        >
                            Delete
                        </button>
                    </div>
                )}
            </div>
        </div>
</div>
)
}