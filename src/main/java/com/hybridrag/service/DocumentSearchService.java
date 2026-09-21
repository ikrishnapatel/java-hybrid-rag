package com.hybridrag.service;

import com.hybridrag.dto.SearchResultDto;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.ChromaVectorStore;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class DocumentSearchService {

    private final ChromaVectorStore defaultVectorStore;
    private final ChromaVectorStore llmVectorStore;
    private final BM25SearchService bm25SearchService;

    public DocumentSearchService(
            @Qualifier("defaultVectorStore") ChromaVectorStore defaultVectorStore,
            @Qualifier("llmVectorStore") ChromaVectorStore llmVectorStore,
            BM25SearchService bm25SearchService) {
        this.defaultVectorStore = defaultVectorStore;
        this.llmVectorStore = llmVectorStore;
        this.bm25SearchService = bm25SearchService;
    }

    public List<SearchResultDto> searchSimilarDocuments(String query, int maxResults, double minScore, boolean useLLMEmbedding) {

        ChromaVectorStore selectedStore = useLLMEmbedding ? llmVectorStore : defaultVectorStore;

        List<Document> documents = selectedStore.similaritySearch(
                SearchRequest.query(query)
                        .withTopK(maxResults)
                        .withFilterExpression(new org.springframework.ai.vectorstore.filter.FilterExpressionBuilder().ne("dummy", "dummy").build())
        );

        return documents.stream().map(doc -> {
            String text = doc.getContent();
            Double score = doc.getMetadata().containsKey("distance") ? ((Number) doc.getMetadata().get("distance")).doubleValue() : 0.0;
            return new SearchResultDto(
                    doc.getId(),
                    text,
                    score,
                    doc.getMetadata()
            );
        }).filter(res -> (1.0 - res.getScore()) >= minScore).collect(Collectors.toList());
    }

    public List<SearchResultDto> searchBM25(String query, int maxResults) throws Exception {
        List<BM25SearchService.BM25Result> bm25Results = bm25SearchService.search(query, maxResults);
        return bm25Results.stream().map(res -> new SearchResultDto(
                res.getId(),
                res.getText(),
                res.getScore(),
                null
        )).collect(Collectors.toList());
    }
}
