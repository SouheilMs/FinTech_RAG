import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { conversationService } from '@/services/conversation.service'

const KEY = ['conversations'] as const

export function useConversations(search?: string) {
    const qc = useQueryClient()
    const inv = () => qc.invalidateQueries({ queryKey: KEY })

    const query = useQuery({
        queryKey:  [...KEY, search],
        queryFn:   () => conversationService.list(search),
        staleTime: 15_000,
    })

    const createMutation = useMutation({
        mutationFn: (firstMessage: string) => conversationService.create(firstMessage),
        onSuccess:  inv,
    })

    const renameMutation = useMutation({
        mutationFn: ({ id, title }: { id: string; title: string }) =>
            conversationService.rename(id, title),
        onSuccess: inv,
    })

    const pinMutation = useMutation({
        mutationFn: ({ id, pinned }: { id: string; pinned: boolean }) =>
            conversationService.pin(id, pinned),
        onSuccess: inv,
    })

    const deleteMutation = useMutation({
        mutationFn: (id: string) => conversationService.delete(id),
        onSuccess:  inv,
    })

    return {
        conversations: query.data ?? [],
        isLoading: query.isLoading,
        createConversation: createMutation.mutateAsync,
        renameConversation: renameMutation.mutateAsync,
        pinConversation: pinMutation.mutateAsync,
        deleteConversation: deleteMutation.mutateAsync,
        invalidate: inv,
    }
}