import { useState } from 'react'
import ReactMarkdown from 'react-markdown'
import remarkGfm from 'remark-gfm'
import { Copy, Check, Bot, User, Upload } from 'lucide-react'
import { cn } from '@/utils/cn'
import type { ChatMessage } from '@/types'
import SourceCard from './SourceCard'

interface Props { message: ChatMessage }

const OUT_OF_SCOPE_PHRASES = [
    "i couldn't find relevant information in the uploaded documents"
]

const isOutOfScope = (content: string): boolean => {
    const lower = content.toLowerCase()
    return OUT_OF_SCOPE_PHRASES.some(p => lower.includes(p))
}

export default function MessageBubble({ message }: Props) {
    const [copied, setCopied] = useState(false)
    const isAI       = message.role === 'assistant'
    const isWaiting  = message.isLoading && message.content === ''
    const isStreaming = message.isLoading && message.content !== ''
    const outOfScope = isAI && !message.isLoading && isOutOfScope(message.content)

    const copy = async () => {
        await navigator.clipboard.writeText(message.content)
        setCopied(true)
        setTimeout(() => setCopied(false), 2000)
    }

    const showSources =
        isAI &&
        !message.isLoading &&
        !outOfScope &&
        Array.isArray(message.sources) &&
        message.sources.length > 0

    return (
        <div className={cn('flex gap-3 animate-fade-in', isAI ? 'flex-row' : 'flex-row-reverse')}>

            {/* Avatar */}
            <div className={cn(
                'flex-shrink-0 flex items-center justify-center w-8 h-8 rounded-full text-white mt-0.5',
                isAI ? 'bg-accent' : 'bg-primary',
            )}>
                {isAI ? <Bot size={15} /> : <User size={15} />}
            </div>

            {/* Bubble */}
            <div className={cn(
                'flex flex-col gap-2 max-w-[80%]',
                isAI ? 'items-start' : 'items-end',
            )}>
                <div className={cn(
                    'relative px-4 py-3 rounded-2xl text-sm leading-relaxed',
                    isAI
                        ? 'bg-surface-card border border-surface-border text-text-primary rounded-tl-sm'
                        : 'bg-primary text-white rounded-tr-sm',
                )}>

                    {/* Waiting for first token */}
                    {isWaiting && (
                        <div className="flex gap-1.5 py-1">
                            {[0, 1, 2].map(i => (
                                <span
                                    key={i}
                                    className="w-2 h-2 rounded-full bg-accent animate-pulse-slow"
                                    style={{ animationDelay: `${i * 0.2}s` }}
                                />
                            ))}
                        </div>
                    )}

                    {/* Streaming or complete AI response */}
                    {!isWaiting && isAI && (
                        <>
                            <div className="
                prose prose-invert prose-sm max-w-none
                prose-p:my-1
                prose-pre:bg-surface prose-pre:border prose-pre:border-surface-border
                prose-code:text-accent prose-code:bg-surface prose-code:px-1 prose-code:rounded
                prose-a:text-primary
              ">
                                <ReactMarkdown remarkPlugins={[remarkGfm]}>
                                    {message.content}
                                </ReactMarkdown>
                            </div>

                            {/* Blinking cursor while tokens are arriving */}
                            {isStreaming && (
                                <span className="inline-block w-0.5 h-4 bg-accent ml-0.5 align-text-bottom animate-pulse" />
                            )}

                            {/* Copy — only when complete */}
                            {!message.isLoading && (
                                <button
                                    onClick={copy}
                                    className="absolute top-2 right-2 p-1 rounded-md text-text-muted hover:text-text-secondary hover:bg-surface-raised transition-colors opacity-0 group-hover:opacity-100"
                                    title="Copy response"
                                >
                                    {copied
                                        ? <Check size={13} className="text-success" />
                                        : <Copy size={13} />
                                    }
                                </button>
                            )}
                        </>
                    )}

                    {/* User message */}
                    {!isAI && (
                        <p className="whitespace-pre-wrap">{message.content}</p>
                    )}
                </div>

                {/* Timestamp */}
                <span className="text-[10px] text-text-muted px-1">
          {message.timestamp.toLocaleTimeString('en-GB', {
              hour: '2-digit', minute: '2-digit',
          })}
        </span>

                {outOfScope && (
                    <div className="flex items-center gap-2 text-[11px] text-text-muted bg-surface border border-surface-border rounded-xl px-3 py-2 animate-fade-in">
                        <Upload size={11} className="text-text-muted flex-shrink-0" />
                        <span>
              Upload documents or index a repository to get answers about your content.
            </span>
                    </div>
                )}

                {/* Sources */}
                {showSources && (
                    <div className="flex flex-col gap-1.5 w-full animate-fade-in">
                        <p className="text-[11px] font-medium text-text-muted px-1 uppercase tracking-wider">
                            Sources
                        </p>
                        <div className="flex flex-wrap gap-2">
                            {message.sources!.map((src, i) => (
                                <SourceCard
                                    key={`${src.documentName}-${src.pageNumber}-${i}`}
                                    source={src}
                                    index={i + 1}
                                />
                            ))}
                        </div>
                    </div>
                )}
            </div>
        </div>
    )
}