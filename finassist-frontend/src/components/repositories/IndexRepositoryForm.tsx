import { useState, KeyboardEvent } from 'react'
import { GitBranch, ExternalLink, CheckCircle } from 'lucide-react'
import { cn } from '@/utils/cn'

interface Props {
    onQueue: (url: string) => void
}

const EXAMPLES = [
    'https://github.com/spring-projects/spring-ai',
    'https://github.com/facebook/react',
    'https://gitlab.com/gitlab-org/gitlab',
]

const ALLOWED_HOSTS = ['github.com', 'gitlab.com', 'bitbucket.org']

function validateUrl(url: string): string | null {
    if (!url.trim()) return 'Repository URL is required'
    try {
        const { protocol, hostname, pathname } = new URL(url.trim())
        if (protocol !== 'https:')            return 'Only HTTPS URLs are accepted'
        if (!ALLOWED_HOSTS.includes(hostname)) return 'Only GitHub, GitLab, and Bitbucket are supported'
        if (pathname.split('/').filter(Boolean).length < 2)
            return 'URL must point to a repository (owner/name)'
    } catch {
        return 'Invalid URL format'
    }
    return null
}

export default function IndexRepositoryForm({ onQueue }: Props) {
    const [url, setUrl]     = useState('')
    const [error, setError] = useState<string | null>(null)
    const [queued, setQueued] = useState(false)   // local success flash

    const submit = () => {
        const err = validateUrl(url)
        if (err) { setError(err); return }

        const trimmed = url.trim()

        setQueued(true)
        setError(null)
        setUrl('')

        onQueue(trimmed)

        // Reset the queued flash after 2 seconds
        setTimeout(() => setQueued(false), 2000)
    }

    const handleKey = (e: KeyboardEvent<HTMLInputElement>) => {
        if (e.key === 'Enter') submit()
    }

    return (
        <div className="bg-surface-card border border-surface-border rounded-2xl p-6">
            <div className="flex items-center gap-2.5 mb-4">
                <div className="flex items-center justify-center w-8 h-8 rounded-lg bg-primary/10 border border-primary/20">
                    <GitBranch size={16} className="text-primary" />
                </div>
                <div>
                    <p className="text-sm font-medium text-text-primary">Index a Git Repository</p>
                    <p className="text-xs text-text-muted">
                        GitHub, GitLab, and Bitbucket · Public repos only · HTTPS URLs
                    </p>
                </div>
            </div>

            {/* Input row */}
            <div className={cn(
                'flex items-center gap-2 bg-surface border rounded-xl px-4 py-2.5 transition-all',
                error
                    ? 'border-danger/50 focus-within:border-danger'
                    : 'border-surface-border focus-within:border-primary/40 focus-within:shadow-glow',
            )}>
                <ExternalLink size={15} className="text-text-muted flex-shrink-0" />
                <input
                    type="url"
                    value={url}
                    onChange={e => { setUrl(e.target.value); setError(null) }}
                    onKeyDown={handleKey}
                    placeholder="https://github.com/owner/repository"
                    className="flex-1 bg-transparent text-sm text-text-primary placeholder:text-text-muted outline-none"
                />
                <button
                    onClick={submit}
                    disabled={!url.trim() && !queued}
                    className={cn(
                        'flex-shrink-0 flex items-center gap-1.5 px-3 py-1.5 text-xs font-medium rounded-lg transition-all',
                        queued
                            ? 'bg-success/10 text-success border border-success/30 cursor-default'
                            : !url.trim()
                                ? 'bg-surface-border text-text-muted cursor-not-allowed'
                                : 'bg-primary text-white hover:bg-primary-hover',
                    )}
                >
                    {queued
                        ? <><CheckCircle size={13} /> Queued!</>
                        : 'Index repository'
                    }
                </button>
            </div>

            {/* Validation error */}
            {error && (
                <p className="text-xs text-danger mt-2 px-1">{error}</p>
            )}

            {/* Queued confirmation */}
            {queued && (
                <p className="text-xs text-success mt-2 px-1 animate-fade-in">
                    Repository queued — a card has been added below and will update as indexing progresses.
                </p>
            )}

            {/* Quick examples */}
            <div className="flex flex-wrap gap-2 mt-4">
                <span className="text-[11px] text-text-muted self-center">Examples:</span>
                {EXAMPLES.map(ex => (
                    <button
                        key={ex}
                        onClick={() => { setUrl(ex); setError(null) }}
                        className="text-[11px] text-text-secondary bg-surface border border-surface-border rounded-lg px-2 py-0.5 hover:border-primary/30 hover:text-primary transition-colors"
                    >
                        {ex.replace('https://', '')}
                    </button>
                ))}
            </div>
        </div>
    )
}