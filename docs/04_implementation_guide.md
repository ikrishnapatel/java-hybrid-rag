# Step-by-Step Implementation Guide

This guide outlines the development process divided into two distinct phases. 

## Phase 1: Basic RAG (The Foundation)

In this phase, we will establish the core retrieval-augmented generation pipeline using vector search.

### Step 1: Setup & Document Loading
- **Action:** Initialize the Spring Boot project.
- **Dependencies:** Add LangChain4j Core, LangChain4j Gemini, LangChain4j ChromaDB, and Web dependencies.
- **Implementation:** Create an API endpoint (`/upload-doc`) to accept PDF or TXT files. Use Apache PDFBox or LangChain4j's inbuilt document loader to extract raw text from the files.

### Step 2: Semantic Chunking
- **Action:** Divide the extracted text into manageable chunks.
- **Implementation:** Create a `DocumentSplitter` service. Configure it to split text into chunks of exactly 500 tokens (words), ensuring a 50-token overlap between consecutive chunks to maintain context continuity.

### Step 3: Embeddings & ChromaDB Storage
- **Action:** Convert text chunks to vectors and store them.
- **Implementation:** 
  1. Pass each chunk through the Gemini Embedding Model (`text-embedding-004`) to convert the text into numerical arrays (vectors).
  2. Start a ChromaDB instance using Docker.
  3. Connect the Spring Boot application to ChromaDB and upsert the vectors along with their original text as metadata.

### Step 4: The Retrieval & Question Answering
- **Action:** Answer user questions based on the stored data.
- **Implementation:**
  1. Expose a `/ask-query` endpoint.
  2. Convert the incoming user query into an embedding using the Gemini model.
  3. Execute a similarity search in ChromaDB to retrieve the top 3 nearest vectors (chunks).
  4. Build a Prompt Template: `"Answer the question based ONLY on the following context: {chunks}. Question: {user_query}"`
  5. Send this prompt to the Gemini LLM and return the generated response to the frontend.

*Milestone: The Basic RAG is now live! The system can answer questions based on the uploaded documents.*

---

## Phase 2: Hybrid Search & Reranking (Enterprise Grade)

In this phase, we fix the issue where specific API names or technical jargon (e.g., "OAuth2", "JWT") are missed by Vector search alone.

### Step 5: Implementing BM25 (Sparse Search)
- **Action:** Enable exact keyword text search alongside vector search.
- **Implementation:** Use Apache Lucene to implement BM25 in Java. Modify the document ingestion pipeline (Step 2) to save chunks in a local Lucene index at the same time they are saved to ChromaDB.

### Step 6: Parallel Retrieval Strategy
- **Action:** Fetch results from both databases simultaneously.
- **Implementation:** Upon receiving a user query, split the execution into two parallel threads:
  - **Thread 1:** Retrieves the top 5 meaning-based results from ChromaDB.
  - **Thread 2:** Retrieves the top 5 keyword-based results from Apache Lucene.

### Step 7: Cross-Encoder Reranking (The Magic Layer)
- **Action:** Re-evaluate and sort the combined 10 results.
- **Implementation:** 
  1. Collect the 10 results (handling any duplicates).
  2. Utilize a Reranker API (such as Cohere Rerank or a local HuggingFace cross-encoder).
  3. Send the user query and the 10 chunks to the Reranker.
  4. The Reranker assigns an accuracy score (0.0 to 1.0) to each chunk and sorts them strictly by relevance.
  5. Extract the top 3 highest-scoring chunks and pass them to the Gemini LLM for final generation, exactly as in Step 4.
