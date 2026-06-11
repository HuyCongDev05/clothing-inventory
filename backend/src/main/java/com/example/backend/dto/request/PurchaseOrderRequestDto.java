package com.example.backend.dto.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class PurchaseOrderRequestDto {
    @NotNull(message = "Supplier ID cannot be null")
    private Long supplierId;

    private String note;

    @NotEmpty(message = "Purchase order details cannot be empty")
    private List<PurchaseOrderDetailRequestDto> details;
}