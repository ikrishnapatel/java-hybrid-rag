package com.hybridrag.dto;

import java.util.Map;

public class SearchResultDto {
    private String embeddingId;
    private String text;
    private Double score;
    private Map<String, Object> metadata;

    public SearchResultDto() {
    }

    public SearchResultDto(String embeddingId, String text, Double score, Map<String, Object> metadata) {
        this.embeddingId = embeddingId;
        this.text = text;
        this.score = score;
        this.metadata = metadata;
    }

    public String getEmbeddingId() {
        return embeddingId;
    }

    public void setEmbeddingId(String embeddingId) {
        this.embeddingId = embeddingId;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public Double getScore() {
        return score;
    }

    public void setScore(Double score) {
        this.score = score;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }
}
