package com.example.backend.dto.response;

import com.example.backend.model.enums.Status;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

@Getter
@Setter
public class VariantResponseDto {
    private Long id;
    private Long productId;
    private String sku;
    private BigDecimal purchasePrice;
    private BigDecimal salePrice;
    private Integer quantityOnHand;
    private Status status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Boolean hasTransactions;

    private Map<String, String> attributes;
}