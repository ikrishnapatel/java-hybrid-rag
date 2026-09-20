package com.hybridrag.config;

import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.chroma.ChromaEmbeddingStore;
import dev.langchain4j.data.segment.TextSegment;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ChromaDbConfig {

    @Value("${chromadb.url}")
    private String chromaUrl;
    
    @Value("${chromadb.collection.default-name}")
    private String defaultCollectionName;

    @Value("${chromadb.collection.llm-name}")
    private String llmCollectionName;

    @Bean(name = "defaultChromaStore")
    @org.springframework.context.annotation.Primary
    public EmbeddingStore<TextSegment> defaultChromaStore() {
        return ChromaEmbeddingStore.builder()
                .baseUrl(chromaUrl)
                .collectionName(defaultCollectionName)
                .build();
    }

    @Bean(name = "llmChromaStore")
    public EmbeddingStore<TextSegment> llmChromaStore() {
        return ChromaEmbeddingStore.builder()
                .baseUrl(chromaUrl)
                .collectionName(llmCollectionName)
                .build();
    }
}
