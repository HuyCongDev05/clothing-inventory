package com.example.backend.dto.request;

import com.example.backend.model.enums.PurchaseOrderStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PurchaseOrderStatusUpdateRequestDto {

    @NotNull(message = "Status cannot be null")
    private PurchaseOrderStatus status;
}