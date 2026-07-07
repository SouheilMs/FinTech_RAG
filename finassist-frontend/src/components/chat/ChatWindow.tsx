import { useEffect, useRef } from 'react'
import { Sparkles } from 'lucide-react'
import MessageBubble from './MessageBubble'
import type { ChatMessage } from '@/types'

interface Props {
  messages: ChatMessage[]
  onSuggest?: (question: string) => void
}

const SUGGESTIONS = [
  'What are the key risks mentioned in this document?',
  'Summarise the financial highlights.',
  'What compliance obligations are described?',
  'List all mentioned regulatory frameworks.',
]

export default function ChatWindow({ messages, onSuggest }: Props) {
  const bottomRef = useRef<HTMLDivElement>(null)

  useEffect(() => {
    bottomRef.current?.scrollIntoView({ behavior: 'smooth' })
  }, [messages])

  if (messages.length === 0) {
    return (
      <div className="flex-1 flex flex-col items-center justify-center px-6 py-12 text-center gap-6">
        <div className="flex items-center justify-center w-14 h-14 rounded-2xl bg-accent/10 border border-accent/20">
          <Sparkles size={28} className="text-accent" />
        </div>
        <div>
          <h2 className="text-lg font-semibold text-text-primary">Ask anything about your documents</h2>
          <p className="text-sm text-text-secondary mt-1.5 max-w-sm">
            Upload PDFs on the Documents page, then ask questions here. The AI retrieves relevant passages and cites its sources.
          </p>
        </div>
        {onSuggest && (
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-2 w-full max-w-lg">
            {SUGGESTIONS.map((q) => (
              <button
                key={q}
                onClick={() => onSuggest(q)}
                className="px-4 py-3 text-left text-xs text-text-secondary bg-surface-card border border-surface-border rounded-xl hover:border-primary/30 hover:text-text-primary hover:bg-surface-raised transition-all"
              >
                {q}
              </button>
            ))}
          </div>
        )}
      </div>
    )
  }

  return (
    <div className="flex-1 overflow-y-auto px-4 md:px-8 py-6 space-y-6 scroll-smooth">
      {messages.map((msg) => (
        <div key={msg.id} className="group">
          <MessageBubble message={msg} />
        </div>
      ))}
      <div ref={bottomRef} />
    </div>
  )
}
