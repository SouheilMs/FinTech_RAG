import RepositoryCard from './RepositoryCard'
import type { GitRepository } from '@/types'

interface Props {
    repositories:  GitRepository[]
    isLoading:     boolean
    isError:       boolean
    onSummary:     (id: number) => void
    onReindex:     (id: number) => Promise<void>
    onDelete:      (id: number) => Promise<void>
    reindexingId?: number
    deletingId?:   number
}

function Skeleton() {
    return (
        <div className="bg-surface-card border border-surface-border rounded-xl p-4 animate-pulse">
            <div className="flex items-start gap-2.5 mb-3">
                <div className="w-8 h-8 rounded-lg bg-surface-raised" />
                <div className="flex-1 space-y-2">
                    <div className="h-3.5 bg-surface-raised rounded w-2/3" />
                    <div className="h-2.5 bg-surface-raised rounded w-1/2" />
                </div>
                <div className="h-5 w-16 bg-surface-raised rounded-full" />
            </div>
            <div className="grid grid-cols-2 gap-2 mb-3">
                <div className="h-10 bg-surface-raised rounded-lg" />
                <div className="h-10 bg-surface-raised rounded-lg" />
            </div>
            <div className="flex gap-2 pt-2 border-t border-surface-border">
                <div className="flex-1 h-7 bg-surface-raised rounded-lg" />
                <div className="flex-1 h-7 bg-surface-raised rounded-lg" />
                <div className="flex-1 h-7 bg-surface-raised rounded-lg" />
            </div>
        </div>
    )
}

export default function RepositoryList({
                                           repositories, isLoading, isError,
                                           onSummary, onReindex, onDelete,
                                           reindexingId, deletingId,
                                       }: Props) {
    if (isLoading) {
        return (
            <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
                {Array.from({ length: 3 }).map((_, i) => <Skeleton key={`skeleton-${i}`} />)}
            </div>
        )
    }

    if (isError) {
        return (
            <div className="flex flex-col items-center justify-center py-16 text-center gap-2">
                <p className="text-sm font-medium text-danger">Failed to load repositories</p>
                <p className="text-xs text-text-muted">Check that the Spring Boot backend is running</p>
            </div>
        )
    }

    if (repositories.length === 0) {
        return (
            <div className="flex flex-col items-center justify-center py-20 text-center gap-2">
                <p className="text-sm font-medium text-text-secondary">No repositories yet</p>
                <p className="text-xs text-text-muted">Submit a Git URL above to start indexing</p>
            </div>
        )
    }

    return (
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
            {repositories.map((repo, index) => (
                <RepositoryCard
                    key={repo.id ?? `repo-${index}`}
                    repo={repo}
                    onSummary={onSummary}
                    onReindex={onReindex}
                    onDelete={onDelete}
                    isReindexing={reindexingId === repo.id}
                    isDeleting={deletingId === repo.id}
                />
            ))}
        </div>
    )
}