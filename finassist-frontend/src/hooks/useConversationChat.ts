import { useState, useEffect, useCallback, useRef } from 'react'
import { conversationService } from '@/services/conversation.service'
import type { ChatMessage, SourceReference, ApiMessage } from '@/types'

function toUiMessage(m: ApiMessage): ChatMessage {
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
    const pendingSources  = useRef<SourceReference[]>([])
    const convIdRef= useRef<string | null>(conversationId)
    const streamingRef= useRef(false)

    useEffect(() => {
        convIdRef.current = conversationId
    }, [conversationId])

    useEffect(() => {
        if (!conversationId) { return }
        if (streamingRef.current) { return }
        let cancelled = false
        setLoadingHistory(true)
        conversationService
            .getMessages(conversationId)
            .then(msgs => {
                if (!cancelled) { setMessages(msgs.map(toUiMessage))}
            })
            .catch(() => {
                if (!cancelled) { setMessages([])}
            })
            .finally(() => {
                if (!cancelled) { setLoadingHistory(false)}
            })
        return () => { cancelled = true }
    }, [conversationId])
    useEffect(() => {
        if (!conversationId) {
            setMessages([])
            setLoadingHistory(false)
        }
    }, [conversationId])
    const visibleMessages = conversationId ? messages : []

    const sendMessage = useCallback(
        async (question: string, overrideId?: string) => {
            const id = overrideId ?? convIdRef.current
            if (!id || !question.trim() || isThinking) return
            pendingSources.current = []
            streamingRef.current   = true
            setThinking(true)

            const userMsgId = crypto.randomUUID()
            setMessages(prev => [
                ...prev,
                {
                    id: userMsgId,
                    role: 'user',
                    content: question.trim(),
                    timestamp: new Date(),
                    isLoading: false,
                },
            ])

            const aiMsgId = crypto.randomUUID()
            setMessages(prev => [
                ...prev,
                {
                    id: aiMsgId,
                    role: 'assistant',
                    content: '',
                    sources: [],
                    timestamp: new Date(),
                    isLoading: true,
                },
            ])
            try {
                await conversationService.streamMessage(id, question.trim(), {
                    onToken: (token) => {
                        setMessages(prev =>
                            prev.map(m =>
                                m.id === aiMsgId
                                    ? { ...m, content: m.content + token }
                                    : m,
                            ),
                        )
                    },
                    onSources: (sources) => {
                        // Store in ref — applied atomically in onDone to avoid stale spread
                        pendingSources.current = sources
                    },
                    onDone: () => {
                        const sources = pendingSources.current
                        setMessages(prev =>
                            prev.map(m =>
                                m.id === aiMsgId
                                    ? { ...m, isLoading: false, sources }
                                    : m,
                            ),
                        )
                        setThinking(false)
                        streamingRef.current = false
                    },
                    onError: (msg) => {
                        setMessages(prev =>
                            prev.map(m =>
                                m.id === aiMsgId
                                    ? { ...m, content: `⚠️ ${msg}`, isLoading: false }
                                    : m,
                            ),
                        )
                        setThinking(false)
                        streamingRef.current = false
                    },
                })
            } catch (e: unknown) {
                const msg = (e as { message?: string }).message ?? 'Streaming failed'
                setMessages(prev =>
                    prev.map(m =>
                        m.id === aiMsgId
                            ? { ...m, content: `⚠️ ${msg}`, isLoading: false }
                            : m,
                    ),
                )
                setThinking(false)
                streamingRef.current = false
            }
        },
        [isThinking],
    )
    const clearMessages = useCallback(() => {
        setMessages([])
        streamingRef.current = false
    }, [])
    return {
        messages: visibleMessages,
        isThinking,
        isLoadingHistory,
        sendMessage,
        clearMessages,
    }
}