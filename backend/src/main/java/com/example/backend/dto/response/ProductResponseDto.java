package com.example.backend.dto.response;

import com.example.backend.model.enums.Status;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class ProductResponseDto {
    private Long id;
    private String code;
    private String name;
    private String categoryName;
    private String brand;
    private String description;
    private String imageUrl;
    private String option1Name;
    private String option2Name;
    private String option3Name;
    private Status status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<VariantResponseDto> variants;
}