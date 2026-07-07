import { useRef, useEffect, KeyboardEvent } from 'react'
import { Send, Loader2 } from 'lucide-react'
import { cn } from '@/utils/cn'

interface Props {
  value: string
  onChange: (v: string) => void
  onSubmit: () => void
  isLoading: boolean
  placeholder?: string
}

export default function ChatInput({ value, onChange, onSubmit, isLoading, placeholder }: Props) {
  const textareaRef = useRef<HTMLTextAreaElement>(null)

  // Auto-resize
  useEffect(() => {
    const el = textareaRef.current
    if (!el) return
    el.style.height = 'auto'
    el.style.height = `${Math.min(el.scrollHeight, 160)}px`
  }, [value])

  const handleKey = (e: KeyboardEvent<HTMLTextAreaElement>) => {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault()
      if (!isLoading && value.trim()) onSubmit()
    }
  }

  return (
    <div className="border-t border-surface-border bg-surface-card px-4 md:px-8 py-4">
      <div className="relative flex items-end gap-3 max-w-3xl mx-auto bg-surface-raised border border-surface-border rounded-2xl px-4 py-3 focus-within:border-primary/40 focus-within:shadow-glow transition-all">
        <textarea
          ref={textareaRef}
          rows={1}
          value={value}
          onChange={(e) => onChange(e.target.value)}
          onKeyDown={handleKey}
          disabled={isLoading}
          placeholder={placeholder ?? 'Ask about your documents…'}
          className="flex-1 resize-none bg-transparent text-sm text-text-primary placeholder:text-text-muted outline-none leading-relaxed max-h-40 disabled:opacity-60"
        />
        <button
          onClick={onSubmit}
          disabled={isLoading || !value.trim()}
          className={cn(
            'flex-shrink-0 flex items-center justify-center w-8 h-8 rounded-xl transition-all',
            isLoading || !value.trim()
              ? 'bg-surface-border text-text-muted cursor-not-allowed'
              : 'bg-primary text-white hover:bg-primary-hover shadow-md',
          )}
          aria-label="Send"
        >
          {isLoading
            ? <Loader2 size={15} className="animate-spin" />
            : <Send size={15} />
          }
        </button>
      </div>
      <p className="text-[10px] text-text-muted text-center mt-2">
        Press <kbd className="px-1 py-0.5 bg-surface-border rounded text-[9px]">Enter</kbd> to send · <kbd className="px-1 py-0.5 bg-surface-border rounded text-[9px]">Shift + Enter</kbd> for new line
      </p>
    </div>
  )
}
