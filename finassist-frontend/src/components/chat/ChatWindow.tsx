import { useEffect, useRef } from 'react'
import { Sparkles, FileText, GitBranch } from 'lucide-react'
import MessageBubble from './MessageBubble'
import type { ChatMessage } from '@/types'

interface Props {
  messages:   ChatMessage[]
  onSuggest?: (question: string) => void
}

const SUGGESTIONS = [
  'What are the key risks mentioned in your documents?',
  'Summarise the financial highlights from your reports.',
  'Explain the architecture of your indexed repositories.',
  'What compliance obligations are described in your files?',
]

export default function ChatWindow({ messages, onSuggest }: Props) {
  const bottomRef = useRef<HTMLDivElement>(null)

  useEffect(() => {
    bottomRef.current?.scrollIntoView({ behavior: 'smooth' })
  }, [messages])

  if (messages.length === 0) {
    return (
        <div className="flex-1 flex flex-col items-center justify-center px-6 py-12 text-center gap-6">
          {/* Icon */}
          <div className="flex items-center justify-center w-14 h-14 rounded-2xl bg-accent/10 border border-accent/20">
            <Sparkles size={28} className="text-accent" />
          </div>

          {/* Heading */}
          <div className="max-w-sm">
            <h2 className="text-lg font-semibold text-text-primary">
              Ask anything about your content
            </h2>
            <p className="text-sm text-text-secondary mt-2 leading-relaxed">
              Your documents and repositories are private to your account — no other
              user can access them. Upload PDFs or index a Git repository, then ask
              questions here.
            </p>
          </div>

          {/* Quick-start links */}
          <div className="flex items-center gap-4 text-xs text-text-muted">
            <div className="flex items-center gap-1.5">
              <FileText size={13} />
              <span>Upload PDFs on the Documents page</span>
            </div>
            <span className="text-surface-border">·</span>
            <div className="flex items-center gap-1.5">
              <GitBranch size={13} />
              <span>Index repos on the Repositories page</span>
            </div>
          </div>

          {/* Suggested prompts — shown even before upload so users know what to expect */}
          {onSuggest && (
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-2 w-full max-w-lg">
                {SUGGESTIONS.map(q => (
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
        {messages.map(msg => (
            <div key={msg.id} className="group">
              <MessageBubble message={msg} />
            </div>
        ))}
        <div ref={bottomRef} />
      </div>
  )
}