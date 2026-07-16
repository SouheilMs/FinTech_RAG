import api from './api'
import type { AppUser, CreateUserPayload, UpdateUserPayload, ResetPasswordPayload } from '@/types/user'

export const userService = {

    async getUsers(): Promise<AppUser[]> {
        const { data } = await api.get<AppUser[]>('/users')
        return data
    },

    async getUser(id: string): Promise<AppUser> {
        const { data } = await api.get<AppUser>(`/users/${id}`)
        return data
    },

    async createUser(payload: CreateUserPayload): Promise<AppUser> {
        const { data } = await api.post<AppUser>('/users', payload)
        return data
    },

    async updateUser(id: string, payload: UpdateUserPayload): Promise<AppUser> {
        const { data } = await api.put<AppUser>(`/users/${id}`, payload)
        return data
    },

    async deleteUser(id: string): Promise<void> {
        await api.delete(`/users/${id}`)
    },

    async resetPassword(id: string, payload: ResetPasswordPayload): Promise<void> {
        await api.put(`/users/${id}/password`, payload)
    },

    async updateStatus(id: string, enabled: boolean): Promise<void> {
        await api.put(`/users/${id}/status`, { enabled })
    },

    async getRoles(id: string): Promise<string[]> {
        const { data } = await api.get<string[]>(`/users/${id}/roles`)
        return data
    },

    async assignRole(id: string, roleName: string): Promise<void> {
        await api.post(`/users/${id}/roles/${roleName}`)
    },

    async removeRole(id: string, roleName: string): Promise<void> {
        await api.delete(`/users/${id}/roles/${roleName}`)
    },
}