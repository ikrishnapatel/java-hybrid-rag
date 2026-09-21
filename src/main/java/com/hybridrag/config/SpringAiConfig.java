package com.hybridrag.config;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestTemplate;
import org.springframework.ai.chroma.ChromaApi;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.ChromaVectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.boot.web.client.RestClientCustomizer;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.util.StreamUtils;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
@Configuration
public class SpringAiConfig {

    @Value("${chromadb.url}")
    private String chromaUrl;

    @Value("${chromadb.collection.default-name}")
    private String defaultCollectionName;

    @Value("${chromadb.collection.llm-name}")
    private String llmCollectionName;

    private static class UsageInjectingClientHttpResponse implements ClientHttpResponse {
        private final ClientHttpResponse delegate;
        private final byte[] modifiedBody;

        public UsageInjectingClientHttpResponse(ClientHttpResponse delegate, byte[] modifiedBody) {
            this.delegate = delegate;
            this.modifiedBody = modifiedBody;
        }

        @Override public HttpHeaders getHeaders() { return delegate.getHeaders(); }
        @Override public InputStream getBody() throws IOException { return new ByteArrayInputStream(modifiedBody); }
        @Override public HttpStatusCode getStatusCode() throws IOException { return delegate.getStatusCode(); }
        @Override public String getStatusText() throws IOException { return delegate.getStatusText(); }
        @Override public void close() { delegate.close(); }
    }

    @Bean
    public RestClientCustomizer usageInterceptorCustomizer() {
        return builder -> builder.requestInterceptor(new ClientHttpRequestInterceptor() {
            @Override
            public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution) throws IOException {
                ClientHttpResponse response = execution.execute(request, body);
                if (request.getURI().toString().contains("embeddings") || request.getURI().toString().contains("chat/completions")) {
                    try {
                        byte[] responseBody = StreamUtils.copyToByteArray(response.getBody());
                        String json = new String(responseBody, StandardCharsets.UTF_8);
                        if (!json.contains("\"usage\"")) {
                            if (json.trim().endsWith("}")) {
                                json = json.substring(0, json.lastIndexOf("}")) + ",\"usage\":{\"prompt_tokens\":0,\"completion_tokens\":0,\"total_tokens\":0}}";
                            }
                        }
                        byte[] modifiedBody = json.getBytes(StandardCharsets.UTF_8);
                        return new UsageInjectingClientHttpResponse(response, modifiedBody);
                    } catch (IOException e) {
                        // Ignore body reading if getBody() throws (e.g., 404 in HttpURLConnection)
                        return response;
                    }
                }
                return response;
            }
        });
    }

    @Bean
    public ChromaApi chromaApi(RestClient.Builder restClientBuilder) {
        return new ChromaApi(chromaUrl, restClientBuilder);
    }

    @Bean(name = "defaultVectorStore")
    @Primary
    public ChromaVectorStore defaultVectorStore(ChromaApi chromaApi, EmbeddingModel embeddingModel) {
        return new ChromaVectorStore(embeddingModel, chromaApi, defaultCollectionName, true);
    }

    @Bean(name = "llmVectorStore")
    public ChromaVectorStore llmVectorStore(ChromaApi chromaApi, EmbeddingModel embeddingModel) {
        return new ChromaVectorStore(embeddingModel, chromaApi, llmCollectionName, true);
    }
}
