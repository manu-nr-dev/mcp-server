# mcp-tools-server

An MCP (Model Context Protocol) tool server that exposes RAG and database lookup tools to AI agents. Built with Spring Boot, Spring AI, WebFlux, and PostgreSQL with pgvector.

**Companion service:** [mcp-agent](https://github.com/manu-nr-dev/mcp-agent) — the orchestration agent that consumes these tools.

---

## What It Does

`mcp-tools-server` exposes two tools over MCP/SSE that `mcp-agent` calls during task orchestration:

- **`rag_lookup`** — semantic search over a knowledge base using pgvector + full-text search with Reciprocal Rank Fusion (RRF)
- **`lookup_products`** — structured database queries against a product/incident database

---

## Architecture

```
mcp-agent (port 8082)
  │
  │  MCP over SSE
  ▼
mcp-tools-server (port 8083)
  │
  ├── rag_lookup
  │     ├── pgvector semantic search
  │     ├── PostgreSQL full-text search
  │     └── RRF re-ranking → top-k results
  │
  └── lookup_products
        └── PostgreSQL structured query
  │
  ▼
PostgreSQL (pgvector + pg16)
```

---

## Why WebFlux / Netty

MCP communication uses Server-Sent Events (SSE). Tomcat cannot reliably flush SSE streams — connections hang or deliver partial results unpredictably. Netty (via Spring WebFlux) handles SSE correctly. This is a hard infrastructure constraint, not a preference.

`mcp-agent` uses Tomcat/WebMVC — it is an SSE client, not a server, so Tomcat works fine there.

---

## Stack

| Layer | Technology |
|---|---|
| Runtime | Java 21 |
| Framework | Spring Boot 4.0.3 / WebFlux / Netty |
| AI | Spring AI 1.1.2 BOM |
| MCP Server | `spring-ai-starter-mcp-server` |
| Embeddings | `text-embedding-004` via Google GenAI |
| Vector DB | PostgreSQL 16 + pgvector |
| Search | Hybrid: pgvector + full-text via RRF |

---

## RAG Pipeline

Documents are ingested via `DocumentIngestionService` (Apache PDFBox). Each document is chunked and embedded using `text-embedding-004`. Embeddings are stored in PostgreSQL with pgvector.

At query time, `rag_lookup` runs two searches in parallel:
1. **Semantic search** — pgvector cosine similarity
2. **Full-text search** — PostgreSQL `tsvector`

Results are merged using Reciprocal Rank Fusion (RRF) and the top-k are returned.

---

## Prerequisites

- Java 21
- Docker (for PostgreSQL + pgvector)
- Gemini API key

---

## Running

```bash
# Start PostgreSQL with pgvector
docker run -d \
  --name pgvector \
  -e POSTGRES_PASSWORD=postgres \
  -p 5432:5432 \
  pgvector/pgvector:pg16

# Set your Gemini API key
export SPRING_AI_GOOGLE_GENAI_API_KEY=your_key_here

# Run
./mvnw spring-boot:run
```

Service starts on **port 8083**.

---

## MCP Tools

### rag_lookup

Semantic + full-text search over the knowledge base.

```
Input:  query string
Output: top-k relevant document chunks with scores
```

### lookup_products

Structured query against the product/incident database.

```
Input:  query string
Output: matching records from PostgreSQL
```

---

## Related

- [mcp-agent](https://github.com/manu-nr-dev/mcp-agent) — orchestration agent that consumes these tools