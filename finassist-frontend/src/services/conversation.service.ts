import api from './api'
import keycloak from '@/keycloak'
import type { Conversation, ApiMessage, SourceReference } from '@/types'

type StreamCallbacks = {
    onToken: (token: string) => void
    onSources: (sources: SourceReference[]) => void
    onDone: () => void
    onError: (msg: string) => void
}

export const conversationService = {

    async list(q?: string): Promise<Conversation[]> {
        const { data } = await api.get<Conversation[]>('/conversations', {
            params: q ? { q } : {},
        })
        return data
    },

    async create(firstMessage: string): Promise<Conversation> {
        const { data } = await api.post<Conversation>('/conversations', { firstMessage })
        return data
    },

    async rename(id: string, title: string): Promise<Conversation> {
        const { data } = await api.patch<Conversation>(`/conversations/${id}/rename`, { title })
        return data
    },

    async pin(id: string, pinned: boolean): Promise<Conversation> {
        const { data } = await api.patch<Conversation>(`/conversations/${id}/pin`, { pinned })
        return data
    },

    async delete(id: string): Promise<void> {
        await api.delete(`/conversations/${id}`)
    },

    async getMessages(id: string): Promise<ApiMessage[]> {
        const { data } = await api.get<ApiMessage[]>(`/conversations/${id}/messages`)
        return data
    },

    async streamMessage(conversationId: string, message: string, cb: StreamCallbacks): Promise<void> {
        try { await keycloak.updateToken(30) } catch { keycloak.logout(); return }

        let response: Response
        try {
            response = await fetch(`/api/conversations/${conversationId}/messages`, {
                method:  'POST',
                headers: {
                    'Content-Type':  'application/json',
                    'Accept': 'text/event-stream',
                    'Cache-Control': 'no-cache',
                    'Authorization': `Bearer ${keycloak.token ?? ''}`,
                },
                body: JSON.stringify({ message }),
            })
        } catch {
            cb.onError('Cannot connect to the server')
            return
        }

        if (!response.ok || !response.body) {
            cb.onError(`Server error ${response.status}: ${response.statusText}`)
            return
        }

        const reader  = response.body.getReader()
        const decoder = new TextDecoder()
        let   buffer  = ''

        while (true) {
            const { done, value } = await reader.read()
            if (done) break

            buffer += decoder.decode(value, { stream: true })
                .replace(/\r\n/g, '\n')
                .replace(/\r/g, '\n')

            const parts = buffer.split('\n\n')
            buffer = parts.pop() ?? ''

            for (const part of parts) {
                if (!part.trim()) continue
                const event = parseSSE(part)
                if (!event) continue

                switch (event.name) {
                    case 'token':   cb.onToken(event.data); break
                    case 'sources':
                        try { cb.onSources(JSON.parse(event.data)) } catch ( error ) {
                            console.warn(`Failed to parse sources: ${error}`)
                        }
                        break
                    case 'done': cb.onDone(); return
                    case 'error': cb.onError(event.data); return
                }
            }
        }
    },
}

function parseSSE(raw: string): { name: string; data: string } | null {
    const lines= raw.split('\n')
    let   name= 'message'
    const dataLines: string[] = []

    for (const line of lines) {
        if (line.startsWith('event:')) name = line.slice(6).trim()
        else if (line.startsWith('data:')) dataLines.push(line.slice(5).replace(/\r$/, ''))
    }

    const data = dataLines.join('\n')
    return data ? { name, data } : null
}