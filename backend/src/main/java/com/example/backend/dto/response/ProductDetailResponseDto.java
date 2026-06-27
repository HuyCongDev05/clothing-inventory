package com.example.backend.dto.response;

import com.example.backend.model.enums.Status;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class ProductDetailResponseDto {
    // Variant Info
    private Long variantId;
    private String sku;
    private BigDecimal purchasePrice;
    private BigDecimal salePrice;
    private Integer quantityOnHand;
    private Status variantStatus;

    // Combined Attributes
    private String size;
    private String color;
    private String material;

    // Product Info
    private Long productId;
    private String productName;
    private String categoryName;
    private String brand;
    private String unit = "Cái"; // Default unit
    private String description;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}