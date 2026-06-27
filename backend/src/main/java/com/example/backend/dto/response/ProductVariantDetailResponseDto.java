package com.example.backend.dto.response;

import com.example.backend.model.enums.Status;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

@Data
public class ProductVariantDetailResponseDto {
    // From ProductVariant
    private Long variantId;
    private String sku;
    private BigDecimal purchasePrice;
    private BigDecimal salePrice;
    private Integer quantityOnHand;
    private Status status;

    // From Product
    private Long productId;
    private String productName;
    private String productCode;
    private String brand;
    private String description;
    private String imageUrl;

    // From Category
    private String categoryName;

    // Combined Attributes
    private Map<String, String> attributes;

    // Timestamps (from variant)
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
