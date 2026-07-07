import { useState } from 'react'
import { Trash2 } from 'lucide-react'
import ChatWindow from '@/components/chat/ChatWindow'
import ChatInput from '@/components/chat/ChatInput'
import { useChat } from '@/hooks/useChat'

export default function ChatPage() {
  const { messages, isThinking, sendMessage, clearConversation } = useChat()
  const [draft, setDraft] = useState('')

  const submit = async () => {
    if (!draft.trim() || isThinking) return
    const q = draft
    setDraft('')
    await sendMessage(q)
  }

  const suggest = (q: string) => {
    setDraft(q)
  }

  return (
    <div className="flex flex-col h-[calc(100vh-3.5rem)]"> {/* subtract Navbar height */}
      {/* Toolbar */}
      {messages.length > 0 && (
        <div className="flex items-center justify-between px-4 md:px-8 py-2 border-b border-surface-border">
          <p className="text-xs text-text-muted">{messages.length} message{messages.length !== 1 ? 's' : ''}</p>
          <button
            onClick={clearConversation}
            className="flex items-center gap-1.5 text-xs text-text-muted hover:text-danger transition-colors"
          >
            <Trash2 size={12} />
            Clear conversation
          </button>
        </div>
      )}

      {/* Messages */}
      <ChatWindow messages={messages} onSuggest={suggest} />

      {/* Input */}
      <ChatInput
        value={draft}
        onChange={setDraft}
        onSubmit={submit}
        isLoading={isThinking}
      />
    </div>
  )
}
