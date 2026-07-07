import { useState, useRef, DragEvent, ChangeEvent } from 'react'
import { Upload, FileText, Loader2, CheckCircle, AlertCircle } from 'lucide-react'
import { cn } from '@/utils/cn'

interface Props {
  onUpload: (file: File, onProgress: (pct: number) => void) => Promise<void>
  isUploading: boolean
}

type DropState = 'idle' | 'over' | 'uploading' | 'success' | 'error'

export default function UploadZone({ onUpload, isUploading }: Props) {
  const [state, setState]       = useState<DropState>('idle')
  const [progress, setProgress] = useState(0)
  const [errorMsg, setErrorMsg] = useState('')
  const inputRef                = useRef<HTMLInputElement>(null)

  const handleFile = async (file: File) => {
    if (file.type !== 'application/pdf') {
      setState('error')
      setErrorMsg('Only PDF files are accepted.')
      setTimeout(() => setState('idle'), 3000)
      return
    }
    setState('uploading')
    setProgress(0)
    try {
      await onUpload(file, (pct) => setProgress(pct))
      setState('success')
      setTimeout(() => { setState('idle'); setProgress(0) }, 2500)
    } catch (e: unknown) {
      setState('error')
      setErrorMsg((e as { message?: string }).message ?? 'Upload failed')
      setTimeout(() => setState('idle'), 4000)
    }
  }

  const onDrop = (e: DragEvent) => {
    e.preventDefault()
    const file = e.dataTransfer.files[0]
    if (file) handleFile(file)
  }

  const onChange = (e: ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0]
    if (file) handleFile(file)
    e.target.value = ''
  }

  const stateConfig = {
    idle:      { border: 'border-surface-border hover:border-primary/40', bg: 'bg-surface-card hover:bg-surface-raised' },
    over:      { border: 'border-primary',                                 bg: 'bg-primary-muted' },
    uploading: { border: 'border-accent/40',                               bg: 'bg-surface-card' },
    success:   { border: 'border-success/40',                              bg: 'bg-surface-card' },
    error:     { border: 'border-danger/40',                               bg: 'bg-surface-card' },
  }

  const cfg = stateConfig[state]

  return (
    <div
      className={cn(
        'relative flex flex-col items-center justify-center gap-3 border-2 border-dashed rounded-2xl p-10 text-center cursor-pointer transition-all duration-200',
        cfg.border,
        cfg.bg,
        (state === 'uploading' || isUploading) && 'pointer-events-none',
      )}
      onDragOver={(e) => { e.preventDefault(); setState('over') }}
      onDragLeave={() => setState('idle')}
      onDrop={onDrop}
      onClick={() => state === 'idle' && inputRef.current?.click()}
      role="button"
      tabIndex={0}
      aria-label="Upload PDF"
    >
      <input ref={inputRef} type="file" accept=".pdf,application/pdf" className="hidden" onChange={onChange} />

      {/* Icon */}
      <div className={cn(
        'flex items-center justify-center w-14 h-14 rounded-2xl border transition-colors',
        state === 'success' ? 'bg-success/10 border-success/30' :
        state === 'error'   ? 'bg-danger/10 border-danger/30'   :
        state === 'over'    ? 'bg-primary/10 border-primary/30' :
                              'bg-surface border-surface-border',
      )}>
        {state === 'uploading' ? <Loader2 size={26} className="text-accent animate-spin" /> :
         state === 'success'   ? <CheckCircle size={26} className="text-success" /> :
         state === 'error'     ? <AlertCircle size={26} className="text-danger" /> :
         state === 'over'      ? <Upload size={26} className="text-primary" /> :
                                 <FileText size={26} className="text-text-muted" />}
      </div>

      {/* Copy */}
      {state === 'idle' && (
        <>
          <div>
            <p className="text-sm font-medium text-text-primary">Drop a PDF here, or click to browse</p>
            <p className="text-xs text-text-muted mt-1">PDF only · Max 50 MB</p>
          </div>
        </>
      )}
      {state === 'over'      && <p className="text-sm font-medium text-primary">Release to upload</p>}
      {state === 'uploading' && (
        <div className="w-full max-w-xs">
          <p className="text-sm text-text-secondary mb-2">Uploading… {progress}%</p>
          <div className="h-1.5 bg-surface-border rounded-full overflow-hidden">
            <div
              className="h-full bg-accent rounded-full transition-all duration-200"
              style={{ width: `${progress}%` }}
            />
          </div>
        </div>
      )}
      {state === 'success' && <p className="text-sm font-medium text-success">Uploaded successfully!</p>}
      {state === 'error'   && <p className="text-sm font-medium text-danger">{errorMsg}</p>}
    </div>
  )
}
