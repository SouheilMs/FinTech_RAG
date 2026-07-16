import { BrowserRouter, Routes, Route } from 'react-router-dom'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { AuthProvider }    from '@/context/AuthContext'
import ProtectedRoute      from '@/components/layout/ProtectedRoute'
import Sidebar             from '@/components/layout/Sidebar'
import Navbar              from '@/components/layout/Navbar'
import ChatPage            from '@/pages/ChatPage'
import DocumentsPage       from '@/pages/DocumentsPage'
import RepositoriesPage from "@/pages/RepositoriesPage.tsx";
import UserManagementPage from "@/pages/UserManagementPage.tsx";

const queryClient = new QueryClient({
  defaultOptions: { queries: { retry: 1, refetchOnWindowFocus: false } },
})

function AppLayout() {
  return (
      <div className="flex h-screen bg-surface overflow-hidden">
        <Sidebar />
        <div className="flex flex-col flex-1 min-w-0 overflow-hidden">
          <Navbar />
          <main className="flex-1 overflow-y-auto">
            <Routes>
              <Route path="/" element={
                <ProtectedRoute><ChatPage /></ProtectedRoute>
              }/>
              <Route path="/documents" element={
                <ProtectedRoute adminOnly><DocumentsPage /></ProtectedRoute>
              }/>
                <Route path="/repositories" element={
                    <ProtectedRoute adminOnly><RepositoriesPage /></ProtectedRoute>
                }/>
                <Route path="/users" element={
                    <ProtectedRoute adminOnly><UserManagementPage /></ProtectedRoute>
                }/>
            </Routes>
          </main>
        </div>
      </div>
  )
}

export default function App() {
  return (
      <QueryClientProvider client={queryClient}>
        <AuthProvider>                        {/* no LoginPage route — Keycloak handles login */}
          <BrowserRouter future={{ v7_startTransition: true, v7_relativeSplatPath: true }}>
            <Routes>
              <Route path="/*" element={<AppLayout />} />
            </Routes>
          </BrowserRouter>
        </AuthProvider>
      </QueryClientProvider>
  )
}