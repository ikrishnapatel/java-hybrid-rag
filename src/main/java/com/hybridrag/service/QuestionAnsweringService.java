package com.hybridrag.service;

import com.hybridrag.dto.QuestionAnswerResponseDto;
import com.hybridrag.exception.RagQueryProcessingException;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.ChromaVectorStore;
import org.springframework.ai.vectorstore.SearchRequest;
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

    private final ChromaVectorStore defaultVectorStore;
    private final ChromaVectorStore llmVectorStore;
    private final ChatClient chatClient;

    public QuestionAnsweringService(
            @Qualifier("defaultVectorStore") ChromaVectorStore defaultVectorStore,
            @Qualifier("llmVectorStore") ChromaVectorStore llmVectorStore,
            ChatModel chatModel) {
        this.defaultVectorStore = defaultVectorStore;
        this.llmVectorStore = llmVectorStore;
        this.chatClient = ChatClient.create(chatModel);
    }

    public QuestionAnswerResponseDto answerQuestion(String userQuery, boolean useLLMEmbedding, int topK) {
        if (userQuery == null || userQuery.trim().isEmpty()) {
            throw new IllegalArgumentException("User query cannot be empty.");
        }

        try {
            ChromaVectorStore selectedStore = useLLMEmbedding ? llmVectorStore : defaultVectorStore;

            List<Document> documents = selectedStore.similaritySearch(
                    SearchRequest.query(userQuery)
                            .withTopK(topK)
                            .withFilterExpression(new org.springframework.ai.vectorstore.filter.FilterExpressionBuilder().ne("dummy", "dummy").build())
            );

            List<String> contextChunks = documents.stream()
                    .map(Document::getContent)
                    .filter(text -> text != null && !text.trim().isEmpty())
                    .collect(Collectors.toList());

            String combinedContext = String.join("\n\n--- Chunk ---\n\n", contextChunks);

            String prompt = String.format(
                    "Answer the question based ONLY on the following context: %s. Question: %s",
                    combinedContext,
                    userQuery
            );

            String answer = chatClient.prompt(new Prompt(prompt)).call().content();
            
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
            // 1. Ask LLM to generate multiple queries
            String queryGenerationPrompt = String.format(
                    "You are an AI assistant tasked with generating search queries to find relevant information in a vector database. " +
                    "Generate 3 distinct search queries related to the following question. " +
                    "Return only the queries, one per line, without any numbering or extra text.\nQuestion: %s",
                    userQuery
            );
            
            String generatedQueriesStr = chatClient.prompt(new Prompt(queryGenerationPrompt)).call().content();
            List<String> queries = Arrays.stream(generatedQueriesStr.split("\n"))
                                         .map(String::trim)
                                         .filter(s -> !s.isEmpty())
                                         .collect(Collectors.toList());
            
            if (!queries.contains(userQuery.trim())) {
                queries.add(userQuery.trim());
            }

            // 2. Perform search for each query and combine results
            ChromaVectorStore selectedStore = useLLMEmbedding ? llmVectorStore : defaultVectorStore;
            Set<String> uniqueContextChunks = new LinkedHashSet<>();

            for (String q : queries) {
                List<Document> docs = selectedStore.similaritySearch(
                        SearchRequest.query(q).withTopK(topK).withFilterExpression(new org.springframework.ai.vectorstore.filter.FilterExpressionBuilder().ne("dummy", "dummy").build())
                );

                for (Document doc : docs) {
                    if (doc.getContent() != null && !doc.getContent().trim().isEmpty()) {
                        uniqueContextChunks.add(doc.getContent());
                    }
                }
            }

            List<String> contextChunks = new ArrayList<>(uniqueContextChunks);
            String combinedContext = String.join("\n\n--- Chunk ---\n\n", contextChunks);

            // 3. Construct final Prompt Template
            String prompt = String.format(
                    "Answer the question based ONLY on the following context: %s. Question: %s",
                    combinedContext,
                    userQuery
            );

            // 4. Send prompt to Gemini model and get response
            String answer = chatClient.prompt(new Prompt(prompt)).call().content();
            
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
