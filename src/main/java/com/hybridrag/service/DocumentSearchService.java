package com.hybridrag.service;

import com.hybridrag.dto.SearchResultDto;
import com.hybridrag.repository.ChromaDocumentRepository;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class DocumentSearchService {

    private final EmbeddingModel defaultEmbeddingModel;
    private final EmbeddingModel llmEmbeddingModel;
    private final ChromaDocumentRepository documentRepository;
    private final BM25SearchService bm25SearchService;

    public DocumentSearchService(
            @Qualifier("defaultEmbeddingModel") EmbeddingModel defaultEmbeddingModel,
            @Qualifier("llmEmbeddingModel") EmbeddingModel llmEmbeddingModel,
            ChromaDocumentRepository documentRepository,
            BM25SearchService bm25SearchService) {
        this.defaultEmbeddingModel = defaultEmbeddingModel;
        this.llmEmbeddingModel = llmEmbeddingModel;
        this.documentRepository = documentRepository;
        this.bm25SearchService = bm25SearchService;
    }

    public List<SearchResultDto> searchSimilarDocuments(String query, int maxResults, double minScore, boolean useLLMEmbedding) {

        EmbeddingModel selectedModel = useLLMEmbedding ? llmEmbeddingModel : defaultEmbeddingModel;


        Response<Embedding> queryEmbeddingResponse = selectedModel.embed(query);
        Embedding queryEmbedding = queryEmbeddingResponse.content();

        List<EmbeddingMatch<TextSegment>> matches = documentRepository.searchRelevant(queryEmbedding, maxResults, minScore, useLLMEmbedding);


        return matches.stream().map(match -> {
            TextSegment segment = match.embedded();
            String text = segment != null ? segment.text() : null;
            Double score = match.score();
            java.util.Map<String, Object> metadata = null;
            if (segment != null && segment.metadata() != null) {
                metadata = segment.metadata().toMap();
            }
            return new SearchResultDto(
                    match.embeddingId(),
                    text,
                    score,
                    metadata
            );
        }).collect(Collectors.toList());
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
