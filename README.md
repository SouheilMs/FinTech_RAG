# FinTech_RAG

FinTech_RAG is a **Retrieval-Augmented Generation (RAG)** application designed to help developers interact with and understand their documents and source-code repositories using an AI assistant.

The system combines **document/repository indexing, vector search, conversation memory, and Large Language Models (LLMs)** to provide contextual answers based on the user's own data.

## 🎯 Main Goal

The objective of FinTech_RAG is to provide an AI-powered assistant capable of:

* Answering questions using the user's documents and code repositories.
* Searching relevant content using semantic vector similarity.
* Maintaining conversation history and context.
* Streaming AI responses in real time.
* Providing source references for generated answers.
* Indexing Git repositories and analyzing their source code.
* Keeping user data isolated through authentication and ownership.

## 🏗️ Architecture

```text
                    ┌──────────────────┐
                    │    React Frontend │
                    │     Port: 3000    │
                    └────────┬─────────┘
                             │ REST / SSE
                             ▼
                    ┌──────────────────┐
                    │  Spring Boot API │
                    │     Port: 8080   │
                    └───────┬───┬──────┘
                            │   │
              ┌─────────────┘   └─────────────┐
              ▼                               ▼
      ┌─────────────────┐             ┌──────────────┐
      │ PostgreSQL      │             │   Ollama     │
      │ + pgvector      │             │     LLM      │
      └─────────────────┘             └──────────────┘
              │
              ▼
      ┌─────────────────┐
      │ Vector Embeddings│
      │  + RAG Search    │
      └─────────────────┘

                    ┌──────────────────┐
                    │     Keycloak     │
                    │ Authentication   │
                    └──────────────────┘
```

## 🛠️ Main Technologies

### Backend

* Java
* Spring Boot
* Spring AI
* Spring Data JPA / Hibernate
* PostgreSQL
* pgvector
* JGit
* SSE for streaming responses

### Frontend

* React
* TypeScript
* Vite
* React Router
* TanStack Query
* Tailwind CSS

### AI

* Ollama
* LLMs such as Gemma
* `nomic-embed-text` for embeddings
* Retrieval-Augmented Generation (RAG)

### Infrastructure

* Docker / Docker Compose
* PostgreSQL + pgvector
* Keycloak
* GitHub Actions

## 🚀 Main Features

### AI Chat

Users can ask questions and receive AI-generated answers based on their indexed data.

### RAG Search

Relevant document or source-code chunks are retrieved from the vector database and provided to the LLM as context.

### Document Management

Users can upload and manage documents that are indexed for semantic search.

### Git Repository Indexing

Users can provide a Git repository which is cloned, scanned, chunked, embedded, and stored in the vector database.

### Conversation History

Conversations and messages are persisted, allowing users to continue previous discussions.

### Streaming Responses

AI responses are streamed progressively to the frontend using Server-Sent Events (SSE).

### Source References

Responses can include references to the documents or repository files used to generate the answer.

### Authentication

Keycloak provides authentication and user isolation. Users can only access their own conversations and indexed data.

## 📁 Project Structure

```text
FinTech_RAG/
│
├── finassist-mini/              # Spring Boot backend
│   ├── src/
│   ├── Dockerfile
│   └── pom.xml
│
├── finassist-frontend/          # React frontend
│   ├── src/
│   ├── Dockerfile
│   └── package.json
│
├── docker-compose.yml            # Application infrastructure
├── .github/                      # CI/CD workflows
└── README.md
```

## 🔄 RAG Workflow

```text
Document / Git Repository
          │
          ▼
      Extraction
          │
          ▼
       Chunking
          │
          ▼
     Embeddings
          │
          ▼
 PostgreSQL + pgvector
          │
          │
      User Question
          │
          ▼
   Similarity Search
          │
          ▼
   Relevant Chunks
          │
          ▼
      LLM + Context
          │
          ▼
   Streaming Response
```

## 🔐 Configuration

Application configuration is provided through environment variables.

Sensitive configuration such as:

* Database credentials
* Keycloak credentials
* Ollama configuration
* Application secrets

should be stored outside the Git repository, for example through environment variables or deployment secrets.

## ▶️ Running the Project

The project is designed to run using Docker Compose.

```bash
docker compose up --build
```

The main components are then available through their configured Docker services.

## 📌 Project Status

FinTech_RAG is an evolving RAG platform focused on **AI-assisted document and code understanding**, with an architecture designed to support additional AI models, repositories, document types, and conversation features.
