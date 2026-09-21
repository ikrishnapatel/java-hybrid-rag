# Project Context: Java Hybrid RAG (Spring AI Edition)

## Overview
This project is a Hybrid Retrieval-Augmented Generation (RAG) system built with **Spring Boot** and **Spring AI**. It combines vector-based semantic search (using ChromaDB) with keyword-based search (using Apache Lucene BM25) to provide highly relevant context to a Large Language Model (Google Gemini).

Recently, the project was migrated from **LangChain4j** to **Spring AI** to align better with the Spring ecosystem and reduce boilerplate code.

## Tech Stack
- **Framework**: Spring Boot 3.2.x
- **AI Framework**: Spring AI (`1.0.0-M1` milestones)
- **Vector Database**: ChromaDB
- **LLM / Embeddings**: Google Gemini (via `spring-ai-vertex-ai-gemini-spring-boot-starter`)
- **Keyword Search**: Apache Lucene (BM25 Algorithm)
- **Document Parsing**: Spring AI PDF Document Reader (Apache PDFBox under the hood)

## Architecture & Core Components

### 1. Configuration (`com.hybridrag.config.SpringAiConfig`)
Instead of manual API clients, the project defines Spring Beans. Notably, it configures two distinct `ChromaVectorStore` beans:
- `defaultVectorStore`: Used for standard operations.
- `llmVectorStore`: Used when the `useLLMEmbedding` flag is set to true.

### 2. Document Ingestion (`com.hybridrag.service.DocumentIngestionService`)
Handles the upload and processing of PDF files:
- **Parsing**: Uses Spring AI's `PagePdfDocumentReader`.
- **Chunking**: Uses Spring AI's `TokenTextSplitter`.
- **Storage**: Chunks are added to the configured `ChromaVectorStore` (which automatically embeds the text) and simultaneously indexed in the local Lucene directory for BM25 search.

### 3. Search (`com.hybridrag.service.DocumentSearchService` & `BM25SearchService`)
- **Semantic Search**: Uses Spring AI's `VectorStore.similaritySearch()` to query ChromaDB based on embedding distance.
- **Keyword Search**: Uses a custom Lucene implementation (`BM25SearchService`) to find exact keyword matches in the `data/lucene_index` directory.

### 4. Question Answering (`com.hybridrag.service.QuestionAnsweringService`)
Interacts with the LLM via Spring AI's `ChatClient`. Features two main endpoints:
- **Standard RAG**: Embeds the user query, retrieves similar context from ChromaDB, and prompts the LLM for an answer.
- **Multi-Query Expansion**: A more advanced endpoint (`/api/v1/documents/ask-query-multi`) that first asks the LLM to generate 3 alternative search queries. It queries the Vector Store for *all* queries, deduplicates the context chunks, and then generates a comprehensive final answer, reducing the chance of missing relevant context.

## Environment Variables
The application relies on an `.env` file (loaded via `dotenv-java`) or system environment variables:
- `GEMINI_API_KEY`: API key for Google Gemini.
- `CHROMA_DB_URL`: URL for the ChromaDB instance (default: `http://localhost:8000`).
- `DEFAULT_NAME`: Collection name for the default vector store.
- `LLM_NAME`: Collection name for the LLM vector store.

## Build and Run
- Built using Maven Wrapper (`./mvnw`).
- Requires a running ChromaDB instance (typically launched via the provided `docker-compose.yml`).
