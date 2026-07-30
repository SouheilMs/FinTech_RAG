import { useState, useCallback } from 'react'
import { Plus } from 'lucide-react'
import ChatWindow from '@/components/chat/ChatWindow'
import ChatInput from '@/components/chat/ChatInput'
import { useConversationContext } from '@/context/ConversationContext'
import { useConversationChat } from '@/hooks/useConversationChat'
import { useConversations } from '@/hooks/useConversations'

export default function ChatPage() {
  const { activeId, setActiveId } = useConversationContext()
  const { createConversation, invalidate } = useConversations()
  const { messages, isThinking, isLoadingHistory, sendMessage } = useConversationChat(activeId)
  const [draft, setDraft] = useState('')

  const submit = useCallback(async () => {
    if (!draft.trim() || isThinking) return
    const q = draft.trim()
    setDraft('')

    if (!activeId) {
      // No conversation selected — create one then send.
      // The new ID is passed directly to avoid stale React state.
      try {
        const conv = await createConversation(q)
        setActiveId(conv.id)
        await sendMessage(q, conv.id)
        invalidate()
      } catch {
        setDraft(q)
      }
    } else {
      await sendMessage(q)
      invalidate()
    }
  }, [draft, isThinking, activeId, createConversation, setActiveId, sendMessage, invalidate])

  const suggest = useCallback((q: string) => setDraft(q), [])

  return (
      <div className="flex flex-col h-[calc(100vh-3.5rem)]">

        {/* Toolbar — same position as your original */}
        {(messages.length > 0 || activeId) && (
            <div className="flex items-center justify-between px-4 md:px-8 py-2 border-b border-surface-border">
              <p className="text-xs text-text-muted">
                {messages.length} message{messages.length !== 1 ? 's' : ''}
              </p>
              {/* New conversation button — replaces the old "Clear conversation" */}
              <button
                  onClick={() => { setActiveId(null); setDraft('') }}
                  className="flex items-center gap-1.5 text-xs text-text-muted hover:text-primary transition-colors"
              >
                <Plus size={12} />
                New conversation
              </button>
            </div>
        )}

        {/* Loading skeleton while history is being fetched */}
        {isLoadingHistory ? (
            <div className="flex-1 flex items-center justify-center">
              <div className="flex gap-1.5">
                {[0, 1, 2].map(i => (
                    <span
                        key={i}
                        className="w-2 h-2 rounded-full bg-accent animate-pulse-slow"
                        style={{ animationDelay: `${i * 0.2}s` }}
                    />
                ))}
              </div>
            </div>
        ) : (
            <ChatWindow messages={messages} onSuggest={suggest} />
        )}

        <ChatInput
            value={draft}
            onChange={setDraft}
            onSubmit={submit}
            isLoading={isThinking}
            placeholder={activeId ? 'Continue the conversation…' : 'Start a new conversation…'}
        />
      </div>
  )
}