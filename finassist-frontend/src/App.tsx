import { BrowserRouter, Routes, Route } from 'react-router-dom'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'

import Sidebar from '@/components/layout/Sidebar'
import Navbar  from '@/components/layout/Navbar'
import ChatPage      from '@/pages/ChatPage'
import DocumentsPage from '@/pages/DocumentsPage'

const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      retry: 1,
      refetchOnWindowFocus: false,
    },
  },
})

export default function App() {
  return (
      <QueryClientProvider client={queryClient}>
        <BrowserRouter future={{ v7_startTransition: true, v7_relativeSplatPath: true }}>
          <div className="flex h-screen bg-surface overflow-hidden">
            <Sidebar />
            <div className="flex flex-col flex-1 min-w-0 overflow-hidden">
              <Navbar />
              <main className="flex-1 overflow-y-auto">
                <Routes>
                  <Route path="/"          element={<ChatPage />} />
                  <Route path="/documents" element={<DocumentsPage />} />
                </Routes>
              </main>
            </div>
          </div>
        </BrowserRouter>
      </QueryClientProvider>
  )
}