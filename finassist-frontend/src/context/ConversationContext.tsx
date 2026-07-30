import { createContext, useContext, useState, ReactNode } from 'react'

interface ConversationContextValue {
    activeId: string | null
    setActiveId: (id: string | null) => void
}

const ConversationContext = createContext<ConversationContextValue | null>(null)

export function ConversationProvider({ children }: { children: ReactNode }) {
    const [activeId, setActiveId] = useState<string | null>(null)
    return (
        <ConversationContext.Provider value={{ activeId, setActiveId }}>
            {children}
        </ConversationContext.Provider>
    )
}

export function useConversationContext() {
    const ctx = useContext(ConversationContext)
    if (!ctx) throw new Error('useConversationContext must be inside ConversationProvider')
    return ctx
}