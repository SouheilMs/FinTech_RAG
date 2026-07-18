import { useState } from 'react'
import { GitBranch, Lock } from 'lucide-react'
import IndexRepositoryForm     from '@/components/repositories/IndexRepositoryForm'
import RepositoryList          from '@/components/repositories/RepositoryList'
import RepositorySummaryDialog from '@/components/repositories/RepositorySummaryDialog'
import ToastContainer          from '@/components/ui/ToastContainer'
import { useRepositories }     from '@/hooks/useRepositories'
import { useToast }            from '@/hooks/useToast'

export default function RepositoriesPage() {
    const toast = useToast()
    const {
        repositories, isLoading, isError,
        queueRepository,
        reindexRepository, reindexingId,
        deleteRepository,  deletingId,
    } = useRepositories()

    const [summaryId,   setSummaryId]   = useState<number | null>(null)
    const [summaryName, setSummaryName] = useState('')

    const handleReindex = async (id: number) => {
        try {
            await reindexRepository(id)
        } catch {
            toast.error('Reindex failed — try again')
        }
    }

    const handleDelete = async (id: number) => {
        try {
            await deleteRepository(id)
            toast.success('Repository deleted')
        } catch {
            toast.error('Delete failed — try again')
        }
    }

    const openSummary = (id: number) => {
        const repo = repositories.find(r => r.id === id)
        setSummaryName(repo ? `${repo.owner}/${repo.name}` : '')
        setSummaryId(id)
    }

    const completed  = repositories.filter(r => r.status === 'COMPLETED').length
    const inProgress = repositories.filter(r =>
        ['PENDING', 'CLONING', 'INDEXING'].includes(r.status)).length
    const failed     = repositories.filter(r => r.status === 'FAILED').length

    return (
        <div className="px-4 md:px-8 py-8 max-w-6xl mx-auto space-y-8">

            {/* Page header */}
            <div className="flex items-start justify-between gap-4">
                <div>
                    <h2 className="text-xl font-semibold text-text-primary">My Repositories</h2>
                    <p className="text-sm text-text-secondary mt-1">
                        Index Git repositories into your personal vector store and ask
                        questions about the code.
                    </p>
                </div>

                <div className="flex items-center gap-2 flex-shrink-0">
                    {/* Status pills */}
                    {repositories.length > 0 && (
                        <div className="hidden sm:flex items-center gap-2">
                            {completed > 0 && (
                                <span className="text-[11px] font-medium px-2.5 py-1 rounded-full bg-success/10 text-success border border-success/20">
                  {completed} indexed
                </span>
                            )}
                            {inProgress > 0 && (
                                <span className="text-[11px] font-medium px-2.5 py-1 rounded-full bg-accent/10 text-accent border border-accent/20 animate-pulse">
                  {inProgress} processing
                </span>
                            )}
                            {failed > 0 && (
                                <span className="text-[11px] font-medium px-2.5 py-1 rounded-full bg-danger/10 text-danger border border-danger/20">
                  {failed} failed
                </span>
                            )}
                        </div>
                    )}

                    {/* Privacy badge */}
                    <div className="hidden sm:flex items-center gap-1.5 text-[11px] text-text-muted bg-surface border border-surface-border rounded-full px-3 py-1.5">
                        <Lock size={10} />
                        Private to your account
                    </div>
                </div>
            </div>

            {/* Index form */}
            <IndexRepositoryForm onQueue={queueRepository} />

            {/* Count */}
            {!isLoading && !isError && repositories.length > 0 && (
                <div className="flex items-center gap-2">
                    <GitBranch size={14} className="text-text-muted" />
                    <p className="text-sm text-text-secondary">
                        {repositories.length}{' '}
                        {repositories.length === 1 ? 'repository' : 'repositories'} in your library
                    </p>
                </div>
            )}

            {/* List */}
            <RepositoryList
                repositories={repositories}
                isLoading={isLoading}
                isError={isError}
                onSummary={openSummary}
                onReindex={handleReindex}
                onDelete={handleDelete}
                reindexingId={reindexingId}
                deletingId={deletingId}
            />

            {/* Summary dialog */}
            <RepositorySummaryDialog
                open={summaryId !== null}
                repositoryId={summaryId}
                repositoryName={summaryName}
                onClose={() => setSummaryId(null)}
            />

            <ToastContainer toasts={toast.toasts} onDismiss={toast.dismiss} />
        </div>
    )
}