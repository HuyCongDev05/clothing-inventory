package com.example.backend.dto.response;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class PurchaseOrderDetailResponseDto {
    private Long id;
    private Long variantId;
    private String sku;
    private String productName;
    private String option1Value;
    private String option2Value;
    private String option3Value;
    private Integer quantity;
    private BigDecimal unitPrice;
    private BigDecimal lineTotal;
}
