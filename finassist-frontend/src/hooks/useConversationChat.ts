import { useState, useEffect, useCallback, useRef } from 'react'
import { conversationService } from '@/services/conversation.service'
import type { ChatMessage, SourceReference } from '@/types'

function toUiMessage(m: {
    id: string; role: string; content: string;
    sources?: SourceReference[]; createdAt: string
}): ChatMessage {
    return {
        id: m.id,
        role: m.role as 'user' | 'assistant',
        content: m.content,
        sources: m.sources,
        timestamp: new Date(m.createdAt),
        isLoading: false,
    }
}

export function useConversationChat(conversationId: string | null) {
    const [messages, setMessages] = useState<ChatMessage[]>([])
    const [isThinking, setThinking] = useState(false)
    const [isLoadingHistory, setLoadingHistory] = useState(false)
    const pendingSources = useRef<SourceReference[]>([])
    // Ref lets sendMessage read the latest conversationId without stale closures
    const convIdRef= useRef<string | null>(conversationId)

    useEffect(() => {
        convIdRef.current = conversationId
        if (!conversationId) { setMessages([]); return }

        setLoadingHistory(true)
        conversationService.getMessages(conversationId)
            .then(msgs => setMessages(msgs.map(toUiMessage)))
            .catch(() => setMessages([]))
            .finally(() => setLoadingHistory(false))
    }, [conversationId])

    const sendMessage = useCallback(async (question: string, overrideId?: string) => {
        const id = overrideId ?? convIdRef.current
        if (!id || !question.trim() || isThinking) return

        pendingSources.current = []

        // Optimistic user bubble
        setMessages(prev => [...prev, {
            id: crypto.randomUUID(), role: 'user',
            content: question.trim(), timestamp: new Date(),
        }])

        // AI placeholder
        const aiId = crypto.randomUUID()
        setMessages(prev => [...prev, {
            id: aiId, role: 'assistant', content: '',
            isLoading: true, timestamp: new Date(),
        }])
        setThinking(true)

        try {
            await conversationService.streamMessage(id, question.trim(), {
                onToken: (token) => {
                    setMessages(prev => prev.map(m =>
                        m.id === aiId ? { ...m, content: m.content + token } : m))
                },
                onSources: (sources) => { pendingSources.current = sources
                    setMessages(prev => prev.map(m =>
                        m.id === aiId ? { ...m, sources: sources } : m))
                },
                onDone: () => {
                    setMessages(prev => prev.map(m =>
                        m.id === aiId
                            ? { ...m, isLoading: false, sources: m.sources ?? pendingSources.current } : m ))
                    setThinking(false)
                },
                onError: (msg) => {
                    setMessages(prev => prev.map(m =>
                        m.id === aiId ? { ...m, content: `⚠️ ${msg}`, isLoading: false } : m))
                    setThinking(false)
                },
            })
        } catch (e: unknown) {
            const msg = (e as { message?: string }).message ?? 'Streaming failed'
            setMessages(prev => prev.map(m =>
                m.id === aiId ? { ...m, content: `⚠️ ${msg}`, isLoading: false } : m))
            setThinking(false)
        }
    }, [isThinking])

    const clearMessages = useCallback(() => setMessages([]), [])

    return { messages, isThinking, isLoadingHistory, sendMessage, clearMessages }
}