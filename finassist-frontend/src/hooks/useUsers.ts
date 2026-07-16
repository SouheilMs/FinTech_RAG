import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { userService } from '@/services/user.service'
import type { CreateUserPayload, UpdateUserPayload, ResetPasswordPayload } from '@/types/user'

const QUERY_KEY = ['users'] as const

export function useUsers() {
    const qc = useQueryClient()
    const invalidate = () => qc.invalidateQueries({ queryKey: QUERY_KEY })

    const query = useQuery({
        queryKey: QUERY_KEY,
        queryFn:  userService.getUsers,
        staleTime: 30_000,
    })

    const createMutation = useMutation({
        mutationFn: (payload: CreateUserPayload) => userService.createUser(payload),
        onSuccess: invalidate,
    })

    const updateMutation = useMutation({
        mutationFn: ({ id, payload }: { id: string; payload: UpdateUserPayload }) =>
            userService.updateUser(id, payload),
        onSuccess: invalidate,
    })

    const deleteMutation = useMutation({
        mutationFn: (id: string) => userService.deleteUser(id),
        onSuccess: invalidate,
    })

    const resetPasswordMutation = useMutation({
        mutationFn: ({ id, payload }: { id: string; payload: ResetPasswordPayload }) =>
            userService.resetPassword(id, payload),
    })

    const statusMutation = useMutation({
        mutationFn: ({ id, enabled }: { id: string; enabled: boolean }) =>
            userService.updateStatus(id, enabled),
        onSuccess: invalidate,
    })

    const assignRoleMutation = useMutation({
        mutationFn: ({ id, role }: { id: string; role: string }) =>
            userService.assignRole(id, role),
        onSuccess: invalidate,
    })

    const removeRoleMutation = useMutation({
        mutationFn: ({ id, role }: { id: string; role: string }) =>
            userService.removeRole(id, role),
        onSuccess: invalidate,
    })

    return {
        users: query.data ?? [],
        isLoading: query.isLoading,
        isError: query.isError,

        createUser: createMutation.mutateAsync,
        isCreating: createMutation.isPending,

        updateUser: updateMutation.mutateAsync,
        isUpdating: updateMutation.isPending,

        deleteUser: deleteMutation.mutateAsync,
        isDeleting: deleteMutation.isPending,

        resetPassword: resetPasswordMutation.mutateAsync,
        isResettingPassword:resetPasswordMutation.isPending,

        updateStatus: statusMutation.mutateAsync,
        isUpdatingStatus: statusMutation.isPending,

        assignRole: assignRoleMutation.mutateAsync,
        removeRole: removeRoleMutation.mutateAsync,
    }
}