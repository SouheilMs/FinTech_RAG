import { chatService } from '@/services/chat.service'
import type { ChatMessage, ChatResponse } from '@/types'
import {useCallback, useState} from "react";

// crypto.randomUUID() is available in all modern browsers
const newId = () => crypto.randomUUID()

export function useChat() {
  const [messages, setMessages]   = useState<ChatMessage[]>([])
  const [isThinking, setThinking] = useState(false)
  const [error, setError]         = useState<string | null>(null)

  const sendMessage = useCallback(async (question: string) => {
    if (!question.trim() || isThinking) return

    setError(null)

    // 1. Append user bubble immediately
    const userMsg: ChatMessage = {
      id:        newId(),
      role:      'user',
      content:   question.trim(),
      timestamp: new Date(),
    }
    setMessages((prev) => [...prev, userMsg])

    // 2. Append placeholder AI bubble
    const aiPlaceholderId = newId()
    setMessages((prev) => [
      ...prev,
      { id: aiPlaceholderId, role: 'assistant', content: '', timestamp: new Date(), isLoading: true },
    ])
    setThinking(true)

    try {
      const response: ChatResponse = await chatService.sendQuestion({ question: question.trim() })

      // 3. Replace placeholder with real answer
      setMessages((prev) =>
        prev.map((m) =>
          m.id === aiPlaceholderId
            ? { ...m, content: response.answer, sources: response.sources, isLoading: false }
            : m,
        ),
      )
    } catch (err: unknown) {
      const msg = (err as { message?: string }).message ?? 'Failed to get a response'
      setError(msg)
      // Replace placeholder with error state
      setMessages((prev) =>
        prev.map((m) =>
          m.id === aiPlaceholderId
            ? { ...m, content: `⚠️ ${msg}`, isLoading: false }
            : m,
        ),
      )
    } finally {
      setThinking(false)
    }
  }, [isThinking])

  const clearConversation = useCallback(() => {
    setMessages([])
    setError(null)
  }, [])

  return { messages, isThinking, error, sendMessage, clearConversation }
}
