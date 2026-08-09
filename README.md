# finassist-mini

`finassist-mini` is an educational Retrieval-Augmented Generation (RAG) backend for financial PDF question answering.

The backend has been build with Java 17 + Spring Boot 3.3.5 + Spring AI:

1. Upload PDF documents.
2. Parse page text.
3. Split text into overlapping chunks.
4. Generate embeddings with Spring AI + Ollama.
5. Persist vectors locally in `data/vector-store.json`.
6. Retrieve Top-K relevant chunks for a question.
7. Ground an LLM prompt with retrieved evidence.
8. Return answer + explicit sources.

## Project Structure

```text
finassist-mini/
├── src/main/java/com/finassistmini/
│   ├── config/
│   ├── dto/
│   ├── model/
│   ├── service/
│   └── web/
├── src/main/resources/application.yml
├── data/documents/
├── pom.xml
├── .env.example
└── README.md
```

## API Endpoints

### Documents

- `POST /documents/upload` PDF only, returns `202 Accepted`
- `GET /documents/jobs/{job_id}` poll ingestion status
- `GET /documents`
- `POST /documents/{id}/reindex`
- `DELETE /documents/{id}`

### Chat

- `POST /chat`

Request:

```json
{
  "question": "What are the international transfer fees?"
}
```

Response:

```json
{
  "answer": "...",
  "sources": [
    {
      "document": "fees.pdf",
      "page": 2
    }
  ]
}
```

## Setup

Install Java 17 and Maven, then make sure Ollama is running:

```bash
ollama pull tinyllama
ollama pull nomic-embed-text
ollama serve
```

Copy the environment file if needed:

```bash
cp .env.example .env
```

Run the API:

```bash
mvn spring-boot:run
```

Open the API at `http://127.0.0.1:8080`.

Spring AI 1.0.0 is used with the Ollama starter.

## Docker Deployment

### Architecture

This repository now provides Dockerized services for:

- `backend` (Spring Boot, Java 17)
- `frontend` (React/Vite build served by Nginx)
- `ollama` (official Ollama image)

PostgreSQL (`postgres`) and Keycloak (`keycloak`) are **not** recreated in this compose file and must already exist.

All services communicate on one shared Docker network (`DOCKER_SHARED_NETWORK`, default `fintech-rag-network`) using service names:

- backend -> `postgres:5432`
- backend -> `keycloak:8080`
- backend -> `ollama:11434`
- frontend -> `backend:8080` (via Nginx `/api` proxy)

### Prerequisites

1. Create environment file:

```bash
cp .env.example .env
```

2. Ensure shared network exists:

```bash
docker network create fintech-rag-network
```

3. Ensure existing PostgreSQL and Keycloak containers are attached to that network with resolvable names `postgres` and `keycloak`.

### Build and Start

```bash
docker compose up --build -d
```

### Stop

```bash
docker compose down
```

### Rebuild

```bash
docker compose down
docker compose build --no-cache
docker compose up -d
```

### Logs and Health

```bash
docker compose ps
docker compose logs -f backend
docker compose logs -f frontend
docker compose logs -f ollama
```

Health checks:

- backend: `GET /actuator/health`
- frontend: `GET /`
- ollama: `GET /api/tags`

### Persistent Volumes

- `backend_documents` -> uploaded documents
- `backend_repositories` -> repository clone/indexing data
- `backend_logs` -> backend logs
- `ollama_data` -> Ollama models

Data is retained across container restarts.

### Troubleshooting

- **Backend unhealthy**: verify PostgreSQL/Keycloak are reachable as `postgres` and `keycloak` on the shared network.
- **Frontend cannot call API**: verify backend health and Nginx proxy route `/api`.
- **Keycloak login fails**: verify `VITE_KEYCLOAK_URL`, `KEYCLOAK_ISSUER_URI`, and realm/client values.
- **Ollama unavailable**: verify `OLLAMA_URL=http://ollama:11434` and Ollama container health.
- **Network DNS issues**: inspect network membership:
  ```bash
  docker network inspect fintech-rag-network
  ```
