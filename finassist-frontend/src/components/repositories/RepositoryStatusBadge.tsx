import { Clock, Download, Loader2, CheckCircle, AlertCircle } from 'lucide-react'
import { cn } from '@/utils/cn'
import type { RepositoryStatus } from '@/types'

interface Props { status: RepositoryStatus }

const config: Record<RepositoryStatus, {
    label: string
    icon:  React.ElementType
    cls:   string
    spin?: boolean
}> = {
    PENDING:  { label: 'Pending',  icon: Clock,        cls: 'text-warning  bg-warning/10  border-warning/20'  },
    CLONING:  { label: 'Cloning',  icon: Download,     cls: 'text-primary  bg-primary/10  border-primary/20', spin: false },
    INDEXING: { label: 'Indexing', icon: Loader2,      cls: 'text-accent   bg-accent/10   border-accent/20',  spin: true  },
    COMPLETED:{ label: 'Indexed',  icon: CheckCircle,  cls: 'text-success  bg-success/10  border-success/20'  },
    FAILED:   { label: 'Failed',   icon: AlertCircle,  cls: 'text-danger   bg-danger/10   border-danger/20'   },
}

export default function RepositoryStatusBadge({ status }: Props) {
    const { label, icon: Icon, cls, spin } = config[status] ?? config.PENDING
    return (
        <span className={cn(
            'inline-flex items-center gap-1 text-[11px] font-medium px-2 py-0.5 rounded-full border',
            cls,
    )}>
    <Icon size={10} className={spin ? 'animate-spin' : ''} />
    {label}
    </span>
)
}