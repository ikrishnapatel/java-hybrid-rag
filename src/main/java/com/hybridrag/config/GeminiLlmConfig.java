package com.hybridrag.config;

import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.googleai.GoogleAiGeminiChatModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GeminiLlmConfig {

    @Value("${app.gemini.api.key}")
    private String geminiApiKey;

    @Value("${gemini.chat.model.name:gemini-2.5-flash}")
    private String geminiChatModelName;

    @Bean
    public ChatLanguageModel geminiChatLanguageModel() {
        return GoogleAiGeminiChatModel.builder()
                .apiKey(geminiApiKey)
                .modelName(geminiChatModelName)
                .temperature(0.2)
                .build();
    }
}
