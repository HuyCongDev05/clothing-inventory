package com.example.backend.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateWarehouseCapacityRequestDto {

    @NotNull(message = "Sức chứa tối đa không được để trống")
    @Min(value = 1, message = "Sức chứa tối đa phải lớn hơn 0")
    private Long maxCapacity;

    @Min(value = 1, message = "Ngưỡng cảnh báo tối thiểu là 1%")
    @Max(value = 100, message = "Ngưỡng cảnh báo tối đa là 100%")
    private Integer warningThreshold;
}
