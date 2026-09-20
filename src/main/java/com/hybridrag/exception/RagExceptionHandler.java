package com.hybridrag.exception;

import com.hybridrag.dto.QuestionAnswerResponseDto;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Collections;

@RestControllerAdvice
public class RagExceptionHandler {

    @ExceptionHandler(RagQueryProcessingException.class)
    public ResponseEntity<QuestionAnswerResponseDto> handleRagQueryProcessingException(RagQueryProcessingException ex) {
        QuestionAnswerResponseDto response = new QuestionAnswerResponseDto(
                ex.getStatusCode(),
                "RAG Processing Error: " + ex.getMessage(),
                null,
                null,
                Collections.emptyList()
        );
        return ResponseEntity.status(ex.getStatusCode()).body(response);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<QuestionAnswerResponseDto> handleIllegalArgumentException(IllegalArgumentException ex) {
        QuestionAnswerResponseDto response = new QuestionAnswerResponseDto(
                HttpStatus.BAD_REQUEST.value(),
                "Invalid Request: " + ex.getMessage(),
                null,
                null,
                Collections.emptyList()
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }
}
