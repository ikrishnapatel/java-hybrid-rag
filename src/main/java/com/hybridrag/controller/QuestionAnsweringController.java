package com.hybridrag.controller;

import com.hybridrag.dto.QuestionAnswerResponseDto;
import com.hybridrag.service.QuestionAnsweringService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/documents")
public class QuestionAnsweringController {

    private final QuestionAnsweringService questionAnsweringService;

    public QuestionAnsweringController(QuestionAnsweringService questionAnsweringService) {
        this.questionAnsweringService = questionAnsweringService;
    }

    @GetMapping("/ask-query")
    public ResponseEntity<QuestionAnswerResponseDto> askQuestion(
            @RequestParam("query") String query,
            @RequestParam(value = "useLLMEmbedding", defaultValue = "false") boolean useLLMEmbedding,
            @RequestParam(value = "topK", defaultValue = "3") int topK) {

        QuestionAnswerResponseDto response = questionAnsweringService.answerQuestion(query, useLLMEmbedding, topK);
        return ResponseEntity.ok(response);
    }
}
