package com.example.backend.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
public class PaymentRequestDto {

    @NotNull(message = "Purchase order ID cannot be null")
    private Long purchaseOrderId;

    @NotNull(message = "Payment method ID cannot be null")
    private Long paymentMethodId;

    @NotNull(message = "Payment date cannot be null")
    private LocalDateTime paymentDate;

    @NotNull(message = "Payment amount cannot be null")
    @DecimalMin(value = "0.01", message = "Payment amount must be greater than 0")
    private BigDecimal amount;

    private String note;
}
