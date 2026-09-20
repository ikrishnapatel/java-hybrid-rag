package com.hybridrag.config;

import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.embedding.onnx.allminilml6v2.AllMiniLmL6V2EmbeddingModel;
import dev.langchain4j.model.googleai.GoogleAiEmbeddingModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class EmbeddingConfig {

    @Bean(name = "defaultEmbeddingModel")
    @Primary
    public EmbeddingModel defaultEmbeddingModel() {
        return new AllMiniLmL6V2EmbeddingModel();
    }

    @Value("${app.gemini.api.key}")
    private String geminiApiKey;

    @Value("${gemini.embedding.model.name}")
    private String geminiModelName;

    @Bean(name = "llmEmbeddingModel")
    public EmbeddingModel llmEmbeddingModel() {
        try {
            return GoogleAiEmbeddingModel.builder()
                .apiKey(geminiApiKey)
                .modelName(geminiModelName)
                .build();
        } catch (Exception e) { 
            System.err.println("Failed to initialize LLM Embedding model. Ensure API keys are set.");
            return defaultEmbeddingModel();
        }
    }
}
