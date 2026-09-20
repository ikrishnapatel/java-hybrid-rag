package com.hybridrag.exception;

public class RagQueryProcessingException extends RuntimeException {

    private final int statusCode;

    public RagQueryProcessingException(String message) {
        super(message);
        this.statusCode = 500;
    }

    public RagQueryProcessingException(String message, int statusCode) {
        super(message);
        this.statusCode = statusCode;
    }

    public int getStatusCode() {
        return statusCode;
    }
}
