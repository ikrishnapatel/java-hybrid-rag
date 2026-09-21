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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
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
            EmbeddingModel selectedModel = useLLMEmbedding ? llmEmbeddingModel : defaultEmbeddingModel;
            Response<Embedding> embeddingResponse = selectedModel.embed(userQuery);
            Embedding queryEmbedding = embeddingResponse.content();

            List<EmbeddingMatch<TextSegment>> matches = documentRepository.searchRelevant(
                    queryEmbedding,
                    topK,
                    0.0,
                    useLLMEmbedding
            );

            List<String> contextChunks = matches.stream()
                    .map(match -> match.embedded() != null ? match.embedded().text() : "")
                    .filter(text -> !text.trim().isEmpty())
                    .collect(Collectors.toList());

            String combinedContext = String.join("\n\n--- Chunk ---\n\n", contextChunks);

            String prompt = String.format(
                    "Answer the question based ONLY on the following context: %s. Question: %s",
                    combinedContext,
                    userQuery
            );

            String answer = geminiChatModel.generate(prompt);
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

    public QuestionAnswerResponseDto answerQuestionWithMultiQuery(String userQuery, boolean useLLMEmbedding, int topK) {
        if (userQuery == null || userQuery.trim().isEmpty()) {
            throw new IllegalArgumentException("User query cannot be empty.");
        }

        try {
            String queryGenerationPrompt = String.format(
                    "You are an AI assistant tasked with generating search queries to find relevant information in a vector database. " +
                    "Generate 3 distinct search queries related to the following question. " +
                    "Return only the queries, one per line, without any numbering or extra text.\nQuestion: %s",
                    userQuery
            );
            
            String generatedQueriesStr = geminiChatModel.generate(queryGenerationPrompt);
            List<String> queries = Arrays.stream(generatedQueriesStr.split("\n"))
                                         .map(String::trim)
                                         .filter(s -> !s.isEmpty())
                                         .collect(Collectors.toList());
            
            if (!queries.contains(userQuery.trim())) {
                queries.add(userQuery.trim());
            }
            EmbeddingModel selectedModel = useLLMEmbedding ? llmEmbeddingModel : defaultEmbeddingModel;
            Set<String> uniqueContextChunks = new LinkedHashSet<>();

            for (String q : queries) {
                Response<Embedding> embeddingResponse = selectedModel.embed(q);
                Embedding queryEmbedding = embeddingResponse.content();

                List<EmbeddingMatch<TextSegment>> matches = documentRepository.searchRelevant(
                        queryEmbedding,
                        topK,
                        0.0,
                        useLLMEmbedding
                );

                for (EmbeddingMatch<TextSegment> match : matches) {
                    if (match.embedded() != null) {
                        String text = match.embedded().text();
                        if (text != null && !text.trim().isEmpty()) {
                            uniqueContextChunks.add(text);
                        }
                    }
                }
            }

            List<String> contextChunks = new ArrayList<>(uniqueContextChunks);
            String combinedContext = String.join("\n\n--- Chunk ---\n\n", contextChunks);

            String prompt = String.format(
                    "Answer the question based ONLY on the following context: %s. Question: %s",
                    combinedContext,
                    userQuery
            );

            String answer = geminiChatModel.generate(prompt);
            return new QuestionAnswerResponseDto(
                    200,
                    "Success",
                    answer,
                    userQuery,
                    contextChunks
            );
        } catch (Exception e) {
            throw new RagQueryProcessingException("Failed to process multi-query RAG query: " + e.getMessage());
        }
    }
}
