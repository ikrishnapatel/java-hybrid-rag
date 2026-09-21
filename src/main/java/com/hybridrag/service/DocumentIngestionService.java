package com.hybridrag.service;

import org.springframework.ai.document.Document;
import org.springframework.ai.reader.ExtractedTextFormatter;
import org.springframework.ai.reader.pdf.PagePdfDocumentReader;
import org.springframework.ai.reader.pdf.config.PdfDocumentReaderConfig;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.ChromaVectorStore;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.io.Resource;
import org.springframework.core.io.InputStreamResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class DocumentIngestionService {

    private final ChromaVectorStore defaultVectorStore;
    private final ChromaVectorStore llmVectorStore;
    private final BM25SearchService bm25SearchService;

    public DocumentIngestionService(
            @Qualifier("defaultVectorStore") ChromaVectorStore defaultVectorStore,
            @Qualifier("llmVectorStore") ChromaVectorStore llmVectorStore,
            BM25SearchService bm25SearchService) {
        this.defaultVectorStore = defaultVectorStore;
        this.llmVectorStore = llmVectorStore;
        this.bm25SearchService = bm25SearchService;
    }

    public void processAndIngestDocument(MultipartFile file, boolean useLLMEmbedding) throws Exception {
        // Parse Document using Spring AI PDF Reader
        List<Document> documents = parseDocument(file);

        // Split Document using Spring AI TokenTextSplitter
        TokenTextSplitter splitter = new TokenTextSplitter();
        List<Document> segments = splitter.apply(documents);

        System.out.println("Document split into " + segments.size() + " chunks.");
        
        ChromaVectorStore selectedStore = useLLMEmbedding ? llmVectorStore : defaultVectorStore;

        System.out.println("Generating embeddings using Spring AI and adding to Chroma...");
        selectedStore.add(segments);

        // Index the same segments for BM25 hybrid search
        bm25SearchService.indexSegments(segments);
        
        System.out.println("Embeddings saved to ChromaDB and segments indexed into BM25!");
    }

    public void deleteChunkById(String id, boolean useLLMEmbedding) {
        ChromaVectorStore selectedStore = useLLMEmbedding ? llmVectorStore : defaultVectorStore;
        selectedStore.delete(List.of(id));
    }

    public void deleteChunksBatch(List<String> ids, boolean useLLMEmbedding) {
        ChromaVectorStore selectedStore = useLLMEmbedding ? llmVectorStore : defaultVectorStore;
        selectedStore.delete(ids);
    }

    private List<Document> parseDocument(MultipartFile file) throws Exception {
        try (InputStream inputStream = file.getInputStream()) {
            Resource resource = new InputStreamResource(inputStream);
            PagePdfDocumentReader pdfReader = new PagePdfDocumentReader(resource,
                    PdfDocumentReaderConfig.builder()
                            .withPageExtractedTextFormatter(ExtractedTextFormatter.builder()
                                    .withNumberOfBottomTextLinesToDelete(0)
                                    .withNumberOfTopPagesToSkipBeforeDelete(0)
                                    .build())
                            .withPagesPerDocument(1)
                            .build());
            return pdfReader.get();
        }
    }
}
