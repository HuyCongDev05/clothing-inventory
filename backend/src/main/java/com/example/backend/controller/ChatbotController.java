package com.example.backend.controller;

import com.example.backend.dto.request.ChatMessageRequestDto;
import com.example.backend.dto.response.ChatMessageResponseDto;
import com.example.backend.dto.response.FormatMessageResponseDto;
import com.example.backend.service.ChatbotService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;

@RestController
@RequestMapping("/api/v1/chatbot")
@RequiredArgsConstructor
public class ChatbotController {

    private final ChatbotService chatbotService;

    @PostMapping("/message")
    public ResponseEntity<FormatMessageResponseDto<ChatMessageResponseDto>> sendMessage(
            @Valid @RequestBody ChatMessageRequestDto request) {

        FormatMessageResponseDto<ChatMessageResponseDto> response = new FormatMessageResponseDto<>();
        response.setSuccess(true);
        response.setStatusCode(200);
        response.setMessage("OK");
        response.setData(chatbotService.processMessage(request));
        response.setTimestamp(Instant.now().toString());

        return ResponseEntity.ok(response);
    }

    @GetMapping("/models")
    public ResponseEntity<FormatMessageResponseDto<java.util.List<String>>> getAvailableModels() {
        FormatMessageResponseDto<java.util.List<String>> response = new FormatMessageResponseDto<>();
        response.setSuccess(true);
        response.setStatusCode(200);
        response.setMessage("OK");
        response.setData(chatbotService.getAvailableModels());
        response.setTimestamp(Instant.now().toString());

        return ResponseEntity.ok(response);
    }
}

