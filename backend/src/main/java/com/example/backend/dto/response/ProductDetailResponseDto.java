package com.example.backend.dto.response;

import com.example.backend.model.enums.Status;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
public class ProductDetailResponseDto {
    private Long variantId;
    private String sku;
    private BigDecimal purchasePrice;
    private BigDecimal salePrice;
    private Integer quantityOnHand;
    private Status variantStatus;

    private String size;
    private String color;
    private String material;

    private Long productId;
    private String productName;
    private String categoryName;
    private String brand;
    private String unit;
    private String description;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}