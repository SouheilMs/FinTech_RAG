import { FileText } from 'lucide-react'
import type { SourceReference } from '@/types'

interface Props {
    source: SourceReference
    index:  number
}

export default function SourceCard({ source, index }: Props) {
    return (
        <div className="
      flex items-start gap-2 px-3 py-2
      bg-surface rounded-lg border border-surface-border text-xs
      max-w-[260px] hover:border-primary/30 transition-colors
    ">
            <div className="
        flex-shrink-0 flex items-center justify-center
        w-5 h-5 rounded-full bg-primary-muted text-primary
        font-semibold text-[10px] mt-0.5
      ">
                {index}
            </div>
            <div className="min-w-0">
                <div className="flex items-center gap-1 text-text-secondary font-medium truncate">
                    <FileText size={11} className="flex-shrink-0" />
                    <span className="truncate">{source.documentName}</span>
                </div>
                <p className="text-text-muted mt-0.5">Page {source.pageNumber}</p>
                {source.excerpt && (
                    <p className="text-text-muted mt-1 line-clamp-2 italic">
                        "{source.excerpt}"
                    </p>
                )}
            </div>
        </div>
    )
}