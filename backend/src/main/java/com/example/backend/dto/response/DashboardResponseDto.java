package com.example.backend.dto.response;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class DashboardResponseDto {
    private BigDecimal totalAmount;
    private Long totalProduct;
    private Long totalSupplier;
    private Long totalInventory;
    private Long warehouseMaxCapacity;
    private Long warehouseRemainingCapacity;
    private Double warehouseOccupancyRate;
    private String warehouseCapacityStatus;
    private String warehouseCapacityMessage;
}
