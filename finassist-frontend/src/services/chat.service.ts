import api from './api'
import keycloak from '@/keycloak'
import type { ChatRequest, ChatResponse, SourceReference } from '@/types'

export const chatService = {

  async sendQuestion(request: ChatRequest): Promise<ChatResponse> {
    const { data } = await api.post<ChatResponse>('/chat', request)
    return data
  },

  async streamQuestion(
      question: string,
      callbacks: {
        onToken:   (token: string)              => void
        onSources: (sources: SourceReference[]) => void
        onDone:    ()                            => void
        onError:   (message: string)            => void
      }
  ): Promise<void> {
    try {
      await keycloak.updateToken(30)
    } catch {
      keycloak.logout()
      return
    }

    const baseUrl = '/api'

    let response: Response
    try {
      response = await fetch(`${baseUrl}/chat/stream`, {
        method:  'POST',
        headers: {
          'Content-Type':  'application/json',
          'Accept':        'text/event-stream',
          'Cache-Control': 'no-cache',
          'Authorization': `Bearer ${keycloak.token ?? ''}`,
        },
        body: JSON.stringify({ question }),
      })
    } catch {
      callbacks.onError('Cannot connect to the server. Is Spring Boot running?')
      return
    }

    if (!response.ok || !response.body) {
      callbacks.onError(`Server error ${response.status}: ${response.statusText}`)
      return
    }

    const reader  = response.body.getReader()
    const decoder = new TextDecoder()
    let   buffer  = ''

    while (true) {
      const { done, value } = await reader.read()
      if (done) break

      buffer += decoder.decode(value, { stream: true }).replace(/\r\n/g, '\n').replace(/\r/g, '\n')

      const parts = buffer.split('\n\n')
      buffer = parts.pop() ?? ''

      for (const part of parts) {
        const event = parseSSEEvent(part)
        if (!event) continue

        switch (event.name) {
          case 'token':
            callbacks.onToken(event.data)
            break

          case 'sources':
            try {
              const backendSources = JSON.parse(event.data)

              const sources: SourceReference[] = backendSources.map((s: any) => ({
                documentName: s.document,
                pageNumber: s.page,
              }))

              callbacks.onSources(sources)
            } catch (e) {
              // Log so you can see the raw data if parsing fails
              console.error('[SSE] Failed to parse sources JSON:', event.data, e)
            }
            break

          case 'done':
            callbacks.onDone()
            return

          case 'error':
            callbacks.onError(event.data)
            return
        }
      }
    }
  },
}

function parseSSEEvent(raw: string): { name: string; data: string } | null {
  const lines = raw.split('\n')
  let name = 'message'
  let data = ''

  for (const line of lines) {
    if (line.startsWith('event:')) {
      name = line.slice(6).trim()
    } else if (line.startsWith('data:')) {
      data = line.slice(5).replace(/\r$/, '')
    }
  }
  return data ? { name, data } : null
}