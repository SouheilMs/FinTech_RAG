import { useState, useCallback } from 'react'
import { chatService } from '@/services/chat.service'
import type { ChatMessage } from '@/types'

const newId = () => crypto.randomUUID()

export function useChat() {
  const [messages, setMessages]   = useState<ChatMessage[]>([])
  const [isThinking, setThinking] = useState(false)
  const [error, setError]         = useState<string | null>(null)

  const sendMessage = useCallback(async (question: string) => {
    if (!question.trim() || isThinking) return
    setError(null)

    // 1 ── Append user bubble immediately
    setMessages(prev => [...prev, {
      id:        newId(),
      role:      'user',
      content:   question.trim(),
      timestamp: new Date(),
    }])

    const aiId = newId()
    setMessages(prev => [...prev, {
      id:        aiId,
      role:      'assistant',
      content:   '',
      sources:   [],
      timestamp: new Date(),
      isLoading: true,
    }])
    setThinking(true)

    try {
      await chatService.streamQuestion(question.trim(), {

        onToken: (token) => {
          setMessages(prev =>
              prev.map(m => m.id === aiId
                  ? { ...m, content: m.content + token }
                  : m
              )
          )
        },

        onSources: (sources) => {
          setMessages(prev =>
              prev.map(m => m.id === aiId ? { ...m, sources } : m)
          )
        },

        onDone: () => {
          setMessages(prev =>
              prev.map(m => m.id === aiId ? { ...m, isLoading: false } : m)
          )
          setThinking(false)
        },

        onError: (msg) => {
          setMessages(prev =>
              prev.map(m => m.id === aiId
                  ? { ...m, content: `⚠️ ${msg}`, isLoading: false }
                  : m
              )
          )
          setError(msg)
          setThinking(false)
        },
      })
    } catch (e: unknown) {
      const msg = (e as { message?: string }).message ?? 'Streaming failed'
      setError(msg)
      setMessages(prev =>
          prev.map(m => m.id === aiId
              ? { ...m, content: `⚠️ ${msg}`, isLoading: false }
              : m
          )
      )
      setThinking(false)
    }
  }, [isThinking])

  const clearConversation = useCallback(() => {
    setMessages([])
    setError(null)
  }, [])

  return { messages, isThinking, error, sendMessage, clearConversation }
}