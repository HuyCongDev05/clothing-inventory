package com.example.backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardAnalyticsResponseDto {

    private String timeframe;
    private List<MonthlyMovementDto> monthlyMovements;
    private List<CategoryDistributionDto> categoryDistribution;
    private List<StockHealthSegmentDto> stockHealthSegments;
    private List<OrderStatusDistributionDto> orderStatusDistribution;
    private List<TopValueProductDto> topValueProducts;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MonthlyMovementDto {
        private String month;
        private long inbound;
        private long outbound;
        private long balance;
        private BigDecimal inboundValue;
        private BigDecimal outboundValue;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CategoryDistributionDto {
        private String id;
        private String name;
        private long quantity;
        private BigDecimal totalValue;
        private int percentage;
        private String color;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StockHealthSegmentDto {
        private String id;
        private String label;
        private long skuCount;
        private int percentage;
        private String color;
        private String description;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrderStatusDistributionDto {
        private String status;
        private String label;
        private long orderCount;
        private BigDecimal totalAmount;
        private int percentage;
        private String color;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TopValueProductDto {
        private String id;
        private String productName;
        private String sku;
        private String category;
        private int quantity;
        private BigDecimal unitPrice;
        private BigDecimal totalValue;
    }
}
