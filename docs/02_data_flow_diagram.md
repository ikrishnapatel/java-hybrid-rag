# Data Flow Diagrams (DFD)

This document outlines the step-by-step data flow for the two main processes in the Hybrid RAG system: Document Ingestion and Query Resolution.

## 1. Document Ingestion Flow (Level 1 DFD)

This flow describes how an uploaded document is processed and stored in both the Vector Database (for semantic search) and the Sparse Index (for keyword search).

```mermaid
sequenceDiagram
    participant User
    participant API as Spring Boot API
    participant Loader as Document Loader
    participant Splitter as Document Splitter
    participant Embedding as Gemini Embedding Model
    participant Chroma as ChromaDB (Vector)
    participant Lucene as Apache Lucene (BM25)

    User->>API: POST /upload-doc (PDF/TXT)
    API->>Loader: Extract Text
    Loader-->>API: Raw Text String
    API->>Splitter: Split Text (500 tokens, 50 overlap)
    Splitter-->>API: Array of Text Chunks
    
    par Vector Storage Pipeline
        API->>Embedding: Generate Embeddings for Chunks
        Embedding-->>API: Array of Vectors
        API->>Chroma: Upsert (Vectors + Metadata)
        Chroma-->>API: Success
    and Keyword Storage Pipeline
        API->>Lucene: Index Text Chunks
        Lucene-->>API: Success
    end
    
    API-->>User: 200 OK (Document Processed)
```

## 2. Hybrid Search & Query Generation Flow (Level 1 DFD)

This flow describes how a user's question is answered using parallel retrieval, cross-encoder reranking, and final LLM generation.

```mermaid
sequenceDiagram
    participant User
    participant API as Spring Boot API
    participant Embedding as Gemini Embedding
    participant DB as Vector (Chroma) & Keyword (Lucene)
    participant Reranker as Cross-Encoder Reranker
    participant Prompt as Prompt Builder
    participant LLM as Gemini Generative Model

    User->>API: GET /ask-query?q="User Question"
    
    par Semantic Retrieval
        API->>Embedding: Convert Query to Vector
        Embedding-->>API: Query Vector
        API->>DB: Similarity Search (ChromaDB)
        DB-->>API: Top 5 Semantic Chunks
    and Keyword Retrieval
        API->>DB: BM25 Search (Lucene)
        DB-->>API: Top 5 Keyword Chunks
    end

    API->>Reranker: Send 10 Chunks + User Query
    Note over API, Reranker: Reranker assigns a relevance score (0-1) to each chunk
    Reranker-->>API: Sorted List of Chunks
    
    API->>API: Select Top 3 Highest Scoring Chunks
    
    API->>Prompt: Combine Top 3 Chunks + User Query
    Prompt-->>API: "Answer based ONLY on context: {chunks}. Query: {query}"
    
    API->>LLM: Generate Answer
    LLM-->>API: Final Text Answer
    
    API-->>User: 200 OK (Final Answer)
```
