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

    async streamMessage(
        conversationId: string,
        message: string,
        cb: StreamCallbacks,
    ): Promise<void> {
        try {
            await keycloak.updateToken(30)
        } catch {
            keycloak.logout()
            return
        }
        const url = `/api/conversations/${conversationId}/messages`
        let response: Response
        try {
            response = await fetch(url, {
                method:  'POST',
                headers: {
                    'Content-Type':  'application/json',
                    'Accept': 'text/event-stream',
                    'Cache-Control': 'no-cache',
                    'Authorization':`Bearer ${keycloak.token ?? ''}`,
                },
                body: JSON.stringify({ message }),
            })
        } catch {
            cb.onError('Cannot connect to the server')
            return
        }
        if (!response.ok || !response.body) {
            // Try to extract a meaningful error from the response body
            let detail = `HTTP ${response.status}: ${response.statusText}`
            try {
                const body = await response.json()
                if (body?.message) detail = body.message
            } catch { /* ignore */ }
            cb.onError(detail)
            return
        }
        const reader  = response.body.getReader()
        const decoder = new TextDecoder()
        let buffer  = ''
        // eslint-disable-next-line no-constant-condition
        while (true) {
            let done:  boolean
            let value: Uint8Array | undefined
            try {
                ({ done, value } = await reader.read())
            } catch {
                cb.onError('Connection lost during streaming')
                return
            }
            if (done) break
            buffer += decoder
                .decode(value, { stream: true })
                .replace(/\r\n/g, '\n')
                .replace(/\r/g, '\n')

            const parts = buffer.split('\n\n')
            buffer = parts.pop() ?? ''   // last part may be incomplete
            for (const part of parts) {
                if (!part.trim()) continue
                const event = parseSSEEvent(part)
                if (!event) continue
                switch (event.name) {
                    case 'token':
                        cb.onToken(event.data)
                        break
                    case 'sources':
                        try {
                            const parsed = JSON.parse(event.data)
                            cb.onSources(parsed as SourceReference[])
                        } catch (e) {
                            console.error('[SSE] Failed to parse sources:', event.data, e)
                        }
                        break
                    case 'done':
                        cb.onDone()
                        return   // stream finished — exit the loop
                    case 'error':
                        cb.onError(event.data)
                        return
                }
            }
        }
        // Stream ended without a 'done' event (server closed connection)
        cb.onDone()
    },
}

function parseSSEEvent(raw: string): { name: string; data: string } | null {
    const lines     = raw.split('\n')
    let   name      = 'message'
    const dataLines: string[] = []

    for (const line of lines) {
        if (line.startsWith('event:')) {
            name = line.slice(6).trim()
        } else if (line.startsWith('data:')) {
            dataLines.push(line.slice(5).replace(/\r$/, ''))
        }
    }
    const data = dataLines.join('\n')
    return data ? { name, data } : null
}