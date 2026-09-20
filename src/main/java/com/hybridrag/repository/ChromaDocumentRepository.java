package com.hybridrag.repository;

import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingStore;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;

import com.hybridrag.service.BM25SearchService;

import java.util.List;

@Repository
public class ChromaDocumentRepository {

    private final EmbeddingStore<TextSegment> defaultEmbeddingStore;
    private final EmbeddingStore<TextSegment> llmEmbeddingStore;
    private final BM25SearchService bm25SearchService;

    public ChromaDocumentRepository(
            @Qualifier("defaultChromaStore") EmbeddingStore<TextSegment> defaultEmbeddingStore,
            @Qualifier("llmChromaStore") EmbeddingStore<TextSegment> llmEmbeddingStore,
            BM25SearchService bm25SearchService) {
        this.defaultEmbeddingStore = defaultEmbeddingStore;
        this.llmEmbeddingStore = llmEmbeddingStore;
        this.bm25SearchService = bm25SearchService;
    }

    private EmbeddingStore<TextSegment> getStore(boolean useLLMEmbedding) {
        return useLLMEmbedding ? llmEmbeddingStore : defaultEmbeddingStore;
    }

    public void saveAll(List<Embedding> embeddings, List<TextSegment> textSegments, boolean useLLMEmbedding) {

        getStore(useLLMEmbedding).addAll(embeddings, textSegments);
        try {
            bm25SearchService.indexSegments(textSegments);
        } catch (Exception e) {
            throw new RuntimeException("Failed to index segments into Lucene BM25", e);
        }
    }

    public List<EmbeddingMatch<TextSegment>> searchRelevant(
            Embedding queryEmbedding, int maxResults, double minScore, boolean useLLMEmbedding) {
        
        EmbeddingSearchRequest request = EmbeddingSearchRequest.builder()
                .queryEmbedding(queryEmbedding)
                .maxResults(maxResults)
                .minScore(minScore)
                .build();

        return getStore(useLLMEmbedding).search(request).matches();
    }

    public void delete(String id, boolean useLLMEmbedding) {
        getStore(useLLMEmbedding).remove(id);
    }

    public void deleteAll(List<String> ids, boolean useLLMEmbedding) {
        getStore(useLLMEmbedding).removeAll(ids);
    }
}
