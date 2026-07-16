import { useEffect } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { repositoryService } from '@/services/repository.service'
import type { GitRepository, RepositoryStatus } from '@/types'

const QUERY_KEY    = ['repositories'] as const
const IN_PROGRESS: RepositoryStatus[] = ['PENDING', 'CLONING', 'INDEXING']

function extractOwner(url: string): string {
    try {
        const parts = new URL(url).pathname.split('/').filter(Boolean)
        return parts[0] ?? 'unknown'
    } catch { return 'unknown' }
}

function extractName(url: string): string {
    try {
        const parts = new URL(url).pathname.split('/').filter(Boolean)
        const raw   = parts[1] ?? 'unknown'
        return raw.endsWith('.git') ? raw.slice(0, -4) : raw
    } catch { return 'unknown' }
}

export function useRepositories() {
    const qc = useQueryClient()

    const {
        data: repositories = [],
        isLoading,
        isError,
        refetch,
    } = useQuery<GitRepository[]>({
        queryKey: QUERY_KEY,
        queryFn:  repositoryService.getRepositories,
        staleTime: 10_000,
    })
    
    useEffect(() => {
        const hasActive = repositories.some(r => IN_PROGRESS.includes(r.status))
        if (!hasActive) return
        const interval = setInterval(
            () => qc.invalidateQueries({ queryKey: QUERY_KEY }),
        )
        return () => clearInterval(interval)
    }, [repositories, qc])

    const indexMutation = useMutation({
        mutationFn: (url: string) => repositoryService.indexRepository(url),

        //  Runs synchronously BEFORE the fetch — card appears instantly
        onMutate: async (url: string) => {
            await qc.cancelQueries({ queryKey: QUERY_KEY })
            const previous = qc.getQueryData<GitRepository[]>(QUERY_KEY)

            const optimistic: GitRepository = {
                id:           -(Date.now()),
                url,
                name:         extractName(url),
                owner:        extractOwner(url),
                branch:       null,
                commitHash:   null,
                status:       'PENDING',
                errorMessage: null,
                indexedFiles: 0,
                totalChunks:  0,
                hasSummary:   false,
                createdAt:    new Date().toISOString(),
                indexedAt:    null,
            }

            qc.setQueryData<GitRepository[]>(
                QUERY_KEY,
                old => [optimistic, ...(old ?? [])],
            )

            return { previous }
        },

        // Replace optimistic card with real data from backend
        onSuccess: () => qc.invalidateQueries({ queryKey: QUERY_KEY }),

        // Roll back if the API call fails
        onError: (_err, _url, context) => {
            if (context?.previous) qc.setQueryData(QUERY_KEY, context.previous)
        },
    })

    const reindexMutation = useMutation({
        mutationFn: (id: number) => repositoryService.reindexRepository(id),

        onMutate: async (id: number) => {
            await qc.cancelQueries({ queryKey: QUERY_KEY })
            const previous = qc.getQueryData<GitRepository[]>(QUERY_KEY)

            qc.setQueryData<GitRepository[]>(
                QUERY_KEY,
                old => old?.map(r => r.id === id
                    ? { ...r, status: 'PENDING' as RepositoryStatus, indexedFiles: 0, totalChunks: 0 }
                    : r,
                ) ?? [],
            )

            return { previous }
        },

        onSuccess:  () => qc.invalidateQueries({ queryKey: QUERY_KEY }),
        onError:    (_err, _id, context) => {
            if (context?.previous) qc.setQueryData(QUERY_KEY, context.previous)
        },
    })

    const deleteMutation = useMutation({
        mutationFn: (id: number) => repositoryService.deleteRepository(id),

        onMutate: async (id: number) => {
            await qc.cancelQueries({ queryKey: QUERY_KEY })
            const previous = qc.getQueryData<GitRepository[]>(QUERY_KEY)
            qc.setQueryData<GitRepository[]>(
                QUERY_KEY,
                old => old?.filter(r => r.id !== id) ?? [],
            )
            return { previous }
        },

        onSuccess:  () => qc.invalidateQueries({ queryKey: QUERY_KEY }),
        onError:    (_err, _id, context) => {
            if (context?.previous) qc.setQueryData(QUERY_KEY, context.previous)
        },
    })

    return {
        repositories,
        isLoading,
        isError,
        refetch,

        queueRepository: (url: string) => indexMutation.mutate(url),

        reindexRepository:  reindexMutation.mutateAsync,
        isReindexing:       reindexMutation.isPending,
        reindexingId:       reindexMutation.isPending
            ? (reindexMutation.variables as number | undefined)
            : undefined,

        deleteRepository: deleteMutation.mutateAsync,
        isDeleting:       deleteMutation.isPending,
        deletingId:       deleteMutation.isPending
            ? (deleteMutation.variables as number | undefined)
            : undefined,
    }
}