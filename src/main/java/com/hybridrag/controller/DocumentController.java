package com.hybridrag.controller;

import com.hybridrag.service.DocumentIngestionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/documents")
public class DocumentController {

    private final DocumentIngestionService ingestionService;

    public DocumentController(DocumentIngestionService ingestionService) {
        this.ingestionService = ingestionService;
    }

    @PostMapping("/upload-doc")
    public ResponseEntity<String> uploadDocument(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "useLLMEmbedding", defaultValue = "false") boolean useLLMEmbedding) {
        
        try {
            ingestionService.processAndIngestDocument(file, useLLMEmbedding);
            return ResponseEntity.ok("Document processed successfully! Embeddings generated.");
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Failed to process document: " + e.getMessage());
        }
    }

    @DeleteMapping("/chunks/{id}")
    public ResponseEntity<String> deleteChunkById(
            @PathVariable("id") String id,
            @RequestParam(value = "useLLMEmbedding", defaultValue = "false") boolean useLLMEmbedding) {
        try {
            ingestionService.deleteChunkById(id, useLLMEmbedding);
            return ResponseEntity.ok("Chunk with ID '" + id + "' deleted successfully from ChromaDB.");
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Failed to delete chunk: " + e.getMessage());
        }
    }

    @DeleteMapping("/chunks/batch")
    public ResponseEntity<String> deleteChunksBatch(
            @RequestBody java.util.List<String> ids,
            @RequestParam(value = "useLLMEmbedding", defaultValue = "false") boolean useLLMEmbedding) {
        if (ids == null || ids.isEmpty()) {
            return ResponseEntity.badRequest().body("List of IDs to delete cannot be empty.");
        }
        try {
            ingestionService.deleteChunksBatch(ids, useLLMEmbedding);
            return ResponseEntity.ok("Successfully deleted " + ids.size() + " chunks from ChromaDB.");
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Failed to delete batch chunks: " + e.getMessage());
        }
    }
}
