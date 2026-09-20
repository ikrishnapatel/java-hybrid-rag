package com.hybridrag.dto;

import java.util.List;

public class QuestionAnswerResponseDto {

    private int statusCode;
    private String message;
    private String answer;
    private String question;
    private List<String> retrievedContextChunks;

    public QuestionAnswerResponseDto() {
    }

    public QuestionAnswerResponseDto(int statusCode, String message, String answer, String question, List<String> retrievedContextChunks) {
        this.statusCode = statusCode;
        this.message = message;
        this.answer = answer;
        this.question = question;
        this.retrievedContextChunks = retrievedContextChunks;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public void setStatusCode(int statusCode) {
        this.statusCode = statusCode;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getAnswer() {
        return answer;
    }

    public void setAnswer(String answer) {
        this.answer = answer;
    }

    public String getQuestion() {
        return question;
    }

    public void setQuestion(String question) {
        this.question = question;
    }

    public List<String> getRetrievedContextChunks() {
        return retrievedContextChunks;
    }

    public void setRetrievedContextChunks(List<String> retrievedContextChunks) {
        this.retrievedContextChunks = retrievedContextChunks;
    }
}
