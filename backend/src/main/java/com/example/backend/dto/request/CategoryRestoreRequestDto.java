package com.example.backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CategoryRestoreRequestDto {
    @NotBlank(message = "Category name cannot be blank")
    private String name;
}
