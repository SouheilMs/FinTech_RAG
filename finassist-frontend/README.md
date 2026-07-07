# FinAssist Frontend

React 19 + TypeScript + Vite frontend for the `finassist-mini` Spring Boot RAG backend.

## Quick start

```bash
# 1. Install dependencies
npm install

# 2. Copy environment file
cp .env.example .env.local

# 3. Start dev server (proxies /api → localhost:8080)
npm run dev
```

Open http://localhost:3000

> The Spring Boot backend must be running on port 8080.

## Stack

| Layer        | Library                  |
|--------------|--------------------------|
| UI framework | React 19 + TypeScript    |
| Build tool   | Vite                     |
| Styling      | Tailwind CSS             |
| Data fetching| TanStack Query v5        |
| HTTP client  | Axios                    |
| Routing      | React Router DOM v6      |
| Forms        | React Hook Form          |
| Markdown     | react-markdown + remark-gfm |
| Icons        | Lucide React             |

## Project structure

```
src/
├── components/
│   ├── layout/     Sidebar, Navbar
│   ├── chat/       ChatWindow, ChatInput, MessageBubble, SourceCard
│   ├── documents/  UploadZone, DocumentCard, DocumentList, DeleteDialog
│   └── ui/         ToastContainer
├── hooks/          useChat, useDocuments, useToast
├── pages/          ChatPage, DocumentsPage
├── services/       api.ts, chat.service.ts, document.service.ts
├── types/          index.ts
└── utils/          cn.ts, format.ts
```

## Backend API

| Method | Path                        | Description          |
|--------|-----------------------------|----------------------|
| POST   | /documents/upload           | Upload PDF           |
| GET    | /documents                  | List documents       |
| POST   | /documents/{id}/reindex     | Re-embed document    |
| DELETE | /documents/{id}             | Delete document      |
| POST   | /chat                       | RAG question answer  |

## Production build

```bash
npm run build
# Output in dist/ — serve with any static host or Nginx
```
