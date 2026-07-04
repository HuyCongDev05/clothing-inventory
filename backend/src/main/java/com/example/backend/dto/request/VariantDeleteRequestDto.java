package com.example.backend.dto.request;

import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class VariantDeleteRequestDto {
    @NotEmpty(message = "Variant IDs cannot be empty")
    private List<Long> variantIds;
}
