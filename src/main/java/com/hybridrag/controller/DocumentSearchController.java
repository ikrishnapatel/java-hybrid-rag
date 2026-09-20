package com.hybridrag.controller;

import com.hybridrag.dto.SearchResultDto;
import com.hybridrag.service.DocumentSearchService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/documents")
public class DocumentSearchController {

    private final DocumentSearchService searchService;

    public DocumentSearchController(DocumentSearchService searchService) {
        this.searchService = searchService;
    }

    @GetMapping("/search")
    public ResponseEntity<?> searchDocuments(
            @RequestParam("query") String query,
            @RequestParam(value = "maxResults", defaultValue = "5") int maxResults,
            @RequestParam(value = "minScore", defaultValue = "0.0") double minScore,
            @RequestParam(value = "useLLMEmbedding", defaultValue = "false") boolean useLLMEmbedding) {

        if (query == null || query.trim().isEmpty()) {
            return ResponseEntity.badRequest().body("Query parameter 'query' cannot be empty.");
        }

        List<SearchResultDto> results = searchService.searchSimilarDocuments(query, maxResults, minScore, useLLMEmbedding);
        return ResponseEntity.ok(results);
    }

    @GetMapping("/search-bm25")
    public ResponseEntity<?> searchDocumentsBM25(
            @RequestParam("query") String query,
            @RequestParam(value = "maxResults", defaultValue = "5") int maxResults) {

        if (query == null || query.trim().isEmpty()) {
            return ResponseEntity.badRequest().body("Query parameter 'query' cannot be empty.");
        }

        try {
            List<SearchResultDto> results = searchService.searchBM25(query, maxResults);
            return ResponseEntity.ok(results);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("BM25 Search failed: " + e.getMessage());
        }
    }
}
