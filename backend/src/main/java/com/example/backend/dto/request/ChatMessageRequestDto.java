package com.example.backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessageRequestDto {

    @NotBlank(message = "Message không được để trống")
    private String message;

    @Builder.Default
    private List<ChatHistoryItem> history = new ArrayList<>();
}
