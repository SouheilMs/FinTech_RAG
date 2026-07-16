import api from './api'
import type {
    GitRepository,
    RepositorySubmitResponse,
    RepositorySummaryResponse,
} from '@/types'

export const repositoryService = {

    async indexRepository(url: string): Promise<RepositorySubmitResponse> {
        const { data } = await api.post<RepositorySubmitResponse>(
            '/repositories/index',
            { url },
            { timeout: 0 },
        )
        return data
    },

    async getRepositories(): Promise<GitRepository[]> {
        const { data } = await api.get<GitRepository[]>('/repositories')
        return data
    },

    async getRepository(id: number): Promise<GitRepository> {
        const { data } = await api.get<GitRepository>(`/repositories/${id}`)
        return data
    },

    async deleteRepository(id: number): Promise<void> {
        await api.delete(`/repositories/${id}`)
    },

    async reindexRepository(id: number): Promise<RepositorySubmitResponse> {
        const { data } = await api.post<RepositorySubmitResponse>(`/repositories/${id}/reindex`)
        return data
    },

    async getSummary(id: number): Promise<RepositorySummaryResponse> {
        const { data } = await api.post<RepositorySummaryResponse>(`/repositories/${id}/summary`)
        return data
    },
}