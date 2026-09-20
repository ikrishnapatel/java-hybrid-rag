package com.hybridrag.service;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.parser.apache.pdfbox.ApachePdfBoxDocumentParser;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import dev.langchain4j.data.document.DocumentParser;

import java.io.InputStream;
import java.util.List;
import com.hybridrag.repository.ChromaDocumentRepository;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.model.output.Response;

@Service
public class DocumentIngestionService {

    private final EmbeddingModel defaultEmbeddingModel;
    private final EmbeddingModel llmEmbeddingModel;
    private final ChromaDocumentRepository documentRepository;
    private final BM25SearchService bm25SearchService;

    public DocumentIngestionService(
            @Qualifier("defaultEmbeddingModel") EmbeddingModel defaultEmbeddingModel,
            @Qualifier("llmEmbeddingModel") EmbeddingModel llmEmbeddingModel,
            ChromaDocumentRepository documentRepository,
            BM25SearchService bm25SearchService) {
        this.defaultEmbeddingModel = defaultEmbeddingModel;
        this.llmEmbeddingModel = llmEmbeddingModel;
        this.documentRepository = documentRepository;
        this.bm25SearchService = bm25SearchService;
    }

    public void processAndIngestDocument(MultipartFile file, boolean useLLMEmbedding) throws Exception {
        Document document = parseDocument(file);

        List<TextSegment> segments = splitDocument(document);

        System.out.println("Document split into " + segments.size() + " chunks.");
        EmbeddingModel selectedModel = useLLMEmbedding ? llmEmbeddingModel : defaultEmbeddingModel;

        System.out.println("Generating embeddings using: " + selectedModel.getClass().getSimpleName());

        Response<List<Embedding>> embeddingsResponse = selectedModel.embedAll(segments);
        List<Embedding> embeddings = embeddingsResponse.content();

        documentRepository.saveAll(embeddings, segments, useLLMEmbedding);

        bm25SearchService.indexSegments(segments);
        
        System.out.println("Embeddings saved to ChromaDB and segments indexed into BM25!");
    }

    public void deleteChunkById(String id, boolean useLLMEmbedding) {
        documentRepository.delete(id, useLLMEmbedding);
    }

    public void deleteChunksBatch(List<String> ids, boolean useLLMEmbedding) {
        documentRepository.deleteAll(ids, useLLMEmbedding);
    }

    private Document parseDocument(MultipartFile file) throws Exception {
        try (InputStream inputStream = file.getInputStream()) {
            DocumentParser parser = new ApachePdfBoxDocumentParser();
            return parser.parse(inputStream);
        }
    }

    private List<TextSegment> splitDocument(Document document) {
        String text = document.text();
        int chunkSize = 2000; // rough char estimate for 500 tokens
        java.util.ArrayList<TextSegment> segments = new java.util.ArrayList<>();
        for (int i = 0; i < text.length(); i += chunkSize) {
            int end = Math.min(text.length(), i + chunkSize);
            segments.add(TextSegment.from(text.substring(i, end)));
        }
        return segments;
    }
}
