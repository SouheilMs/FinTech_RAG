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
