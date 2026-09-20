package com.hybridrag.service;

import com.hybridrag.dto.QuestionAnswerResponseDto;
import com.hybridrag.exception.RagQueryProcessingException;
import com.hybridrag.repository.ChromaDocumentRepository;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class QuestionAnsweringService {

    private final EmbeddingModel defaultEmbeddingModel;
    private final EmbeddingModel llmEmbeddingModel;
    private final ChromaDocumentRepository documentRepository;
    private final ChatLanguageModel geminiChatModel;

    public QuestionAnsweringService(
            @Qualifier("defaultEmbeddingModel") EmbeddingModel defaultEmbeddingModel,
            @Qualifier("llmEmbeddingModel") EmbeddingModel llmEmbeddingModel,
            ChromaDocumentRepository documentRepository,
            ChatLanguageModel geminiChatModel) {
        this.defaultEmbeddingModel = defaultEmbeddingModel;
        this.llmEmbeddingModel = llmEmbeddingModel;
        this.documentRepository = documentRepository;
        this.geminiChatModel = geminiChatModel;
    }

    public QuestionAnswerResponseDto answerQuestion(String userQuery, boolean useLLMEmbedding, int topK) {
        if (userQuery == null || userQuery.trim().isEmpty()) {
            throw new IllegalArgumentException("User query cannot be empty.");
        }

        try {
            // 1. Convert user search query into vector embedding
            EmbeddingModel selectedModel = useLLMEmbedding ? llmEmbeddingModel : defaultEmbeddingModel;
            Response<Embedding> embeddingResponse = selectedModel.embed(userQuery);
            Embedding queryEmbedding = embeddingResponse.content();

            // 2. Perform similarity search in ChromaDB and retrieve top-K nearest chunks
            List<EmbeddingMatch<TextSegment>> matches = documentRepository.searchRelevant(
                    queryEmbedding,
                    topK,
                    0.0,
                    useLLMEmbedding
            );

            // Extract context chunks
            List<String> contextChunks = matches.stream()
                    .map(match -> match.embedded() != null ? match.embedded().text() : "")
                    .filter(text -> !text.trim().isEmpty())
                    .collect(Collectors.toList());

            String combinedContext = String.join("\n\n--- Chunk ---\n\n", contextChunks);

            // 3. Construct Prompt Template
            String prompt = String.format(
                    "Answer the question based ONLY on the following context: %s. Question: %s",
                    combinedContext,
                    userQuery
            );

            // 4. Send prompt to Gemini model and get response
            String answer = geminiChatModel.generate(prompt);

            // 5. Return structured DTO response
            return new QuestionAnswerResponseDto(
                    200,
                    "Success",
                    answer,
                    userQuery,
                    contextChunks
            );
        } catch (Exception e) {
            throw new RagQueryProcessingException("Failed to process Question-Answering RAG query: " + e.getMessage());
        }
    }
}
