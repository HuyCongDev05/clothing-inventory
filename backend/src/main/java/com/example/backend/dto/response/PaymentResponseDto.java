package com.example.backend.dto.response;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
public class PaymentResponseDto {
    private Long id;
    private Long purchaseOrderId;
    private String purchaseOrderCode;
    private Long paymentMethodId;
    private String paymentMethodCode;
    private String paymentMethodName;
    private LocalDateTime paymentDate;
    private BigDecimal amount;
    private String note;
    private Long createdById;
    private String createdByName;
    private BigDecimal totalPaidAmount;
    private BigDecimal remainingAmount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
