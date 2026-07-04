package com.example.backend.dto.response;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
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
