package com.example.backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WarehouseCapacityDto {
    private Long maxCapacity;
    private Long currentInventory;
    private Long remainingCapacity;
    private Double occupancyRate;
    private Integer warningThreshold;
    private String status;
    private String statusMessage;
}
