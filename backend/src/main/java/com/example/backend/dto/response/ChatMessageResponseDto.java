package com.example.backend.dto.response;

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
public class ChatMessageResponseDto {
    private String reply;

    @Builder.Default
    private List<String> suggestions = new ArrayList<>();

    @Builder.Default
    private List<ChatProductDto> products = new ArrayList<>();
}
