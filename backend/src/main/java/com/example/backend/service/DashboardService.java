package com.example.backend.service;

import com.example.backend.dto.response.DashboardAnalyticsResponseDto;
import com.example.backend.dto.response.DashboardAnalyticsResponseDto.*;
import com.example.backend.dto.response.DashboardResponseDto;
import com.example.backend.model.enums.PurchaseOrderStatus;
import com.example.backend.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private static final ZoneId ZONE_VN = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final int TOP_VALUE_LIMIT = 5;

    private static final List<String> CATEGORY_COLORS = List.of(
            "#2563EB", "#7C3AED", "#059669", "#D97706",
            "#DC2626", "#0891B2", "#9333EA", "#EA580C",
            "#16A34A", "#CA8A04"
    );

    private record OrderStatusMeta(String label, String color) {}

    private static final Map<PurchaseOrderStatus, OrderStatusMeta> ORDER_STATUS_META = Map.of(
            PurchaseOrderStatus.RECEIVED,  new OrderStatusMeta("Đã nhập kho",  "#059669"),
            PurchaseOrderStatus.PENDING,   new OrderStatusMeta("Chờ duyệt",    "#2563EB"),
            PurchaseOrderStatus.DRAFT,     new OrderStatusMeta("Bản nháp",     "#64748B"),
            PurchaseOrderStatus.CANCELLED, new OrderStatusMeta("Đã hủy",       "#DC2626")
    );

    private static final List<PurchaseOrderStatus> ALL_PO_STATUSES = List.of(
            PurchaseOrderStatus.RECEIVED,
            PurchaseOrderStatus.PENDING,
            PurchaseOrderStatus.DRAFT,
            PurchaseOrderStatus.CANCELLED
    );

    private final PaymentRepository paymentRepository;
    private final ProductRepository productRepository;
    private final SupplierRepository supplierRepository;
    private final ProductVariantRepository productVariantRepository;
    private final CategoryRepository categoryRepository;
    private final InventoryTransactionRepository inventoryTransactionRepository;
    private final PurchaseOrderRepository purchaseOrderRepository;
    private final SystemSettingService systemSettingService;

    public DashboardResponseDto getParameterDashboard() {
        DashboardResponseDto dto = new DashboardResponseDto();
        dto.setTotalAmount(paymentRepository.sumAllAmount());
        dto.setTotalProduct(productRepository.sumAllProduct());
        dto.setTotalSupplier(supplierRepository.sumAllSupplier());
        dto.setTotalInventory(productVariantRepository.sumAllQuantityOnHand());

        com.example.backend.dto.response.WarehouseCapacityDto capacityInfo = systemSettingService.getWarehouseCapacityInfo();
        dto.setWarehouseMaxCapacity(capacityInfo.getMaxCapacity());
        dto.setWarehouseRemainingCapacity(capacityInfo.getRemainingCapacity());
        dto.setWarehouseOccupancyRate(capacityInfo.getOccupancyRate());
        dto.setWarehouseCapacityStatus(capacityInfo.getStatus());
        dto.setWarehouseCapacityMessage(capacityInfo.getStatusMessage());

        return dto;
    }

    public DashboardAnalyticsResponseDto getAnalytics(String timeframe) {
        String normalized = normalizeTimeframe(timeframe);
        int months = resolveMonths(normalized);

        LocalDateTime endDate   = LocalDate.now(ZONE_VN).atTime(23, 59, 59);
        LocalDateTime startDate = endDate.minusMonths(months).withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0);

        return DashboardAnalyticsResponseDto.builder()
                .timeframe(normalized)
                .monthlyMovements(buildMonthlyMovements(startDate, endDate, months))
                .categoryDistribution(buildCategoryDistribution())
                .stockHealthSegments(buildStockHealthSegments())
                .orderStatusDistribution(buildOrderStatusDistribution())
                .topValueProducts(buildTopValueProducts())
                .build();
    }

    private List<MonthlyMovementDto> buildMonthlyMovements(LocalDateTime startDate,
                                                            LocalDateTime endDate,
                                                            int months) {
        List<Object[]> rows = inventoryTransactionRepository.findMonthlyMovements(startDate, endDate);
        Map<String, Object[]> dataByYearMonth = rows.stream()
                .collect(Collectors.toMap(r -> String.valueOf(r[0]), r -> r));

        List<YearMonth> periods = buildPeriods(months);
        long currentTotal = Optional.ofNullable(productVariantRepository.sumAllQuantityOnHand()).orElse(0L);

        // Tính balance lũy kế bằng cách suy ngược từ tồn kho hiện tại
        long[] balances = computeBalances(periods, dataByYearMonth, currentTotal);

        List<MonthlyMovementDto> result = new ArrayList<>();
        for (int i = 0; i < periods.size(); i++) {
            YearMonth ym = periods.get(i);
            String key = String.format("%d%02d", ym.getYear(), ym.getMonthValue());
            Object[] row = dataByYearMonth.get(key);

            result.add(MonthlyMovementDto.builder()
                    .month("Tháng " + String.format("%02d", ym.getMonthValue()))
                    .inbound(row != null ? toLong(row[1]) : 0L)
                    .outbound(row != null ? toLong(row[2]) : 0L)
                    .balance(balances[i])
                    .inboundValue(row != null ? toBigDecimal(row[3]) : BigDecimal.ZERO)
                    .outboundValue(row != null ? toBigDecimal(row[4]) : BigDecimal.ZERO)
                    .build());
        }
        return result;
    }

    private List<YearMonth> buildPeriods(int months) {
        YearMonth current = YearMonth.now(ZONE_VN);
        List<YearMonth> periods = new ArrayList<>();
        for (int i = months - 1; i >= 0; i--) {
            periods.add(current.minusMonths(i));
        }
        return periods;
    }

    private long[] computeBalances(List<YearMonth> periods,
                                   Map<String, Object[]> dataByYearMonth,
                                   long currentTotal) {
        int n = periods.size();
        long[] balances = new long[n];
        balances[n - 1] = currentTotal;

        for (int i = n - 2; i >= 0; i--) {
            YearMonth next = periods.get(i + 1);
            String nextKey = String.format("%d%02d", next.getYear(), next.getMonthValue());
            Object[] nextRow = dataByYearMonth.get(nextKey);
            long net = nextRow != null ? toLong(nextRow[1]) - toLong(nextRow[2]) : 0L;
            balances[i] = Math.max(0, balances[i + 1] - net);
        }
        return balances;
    }

    private List<CategoryDistributionDto> buildCategoryDistribution() {
        List<Object[]> rows = categoryRepository.findCategoryStockDistribution();
        if (rows.isEmpty()) return Collections.emptyList();

        long grandTotal = rows.stream().mapToLong(r -> toLong(r[2])).sum();
        if (grandTotal == 0) return Collections.emptyList();

        List<CategoryDistributionDto> result = new ArrayList<>();
        for (int i = 0; i < rows.size(); i++) {
            Object[] row = rows.get(i);
            long quantity = toLong(row[2]);
            result.add(CategoryDistributionDto.builder()
                    .id(String.valueOf(row[0]))
                    .name(String.valueOf(row[1]))
                    .quantity(quantity)
                    .totalValue(toBigDecimal(row[3]))
                    .percentage((int) Math.round((double) quantity / grandTotal * 100))
                    .color(CATEGORY_COLORS.get(i % CATEGORY_COLORS.size()))
                    .build());
        }
        adjustCategoryPercentages(result);
        return result;
    }

    private List<StockHealthSegmentDto> buildStockHealthSegments() {
        List<Object[]> rows = productVariantRepository.countSkuByStockHealthSegment();
        Map<String, Long> count = rows.stream()
                .collect(Collectors.toMap(r -> String.valueOf(r[0]), r -> toLong(r[1])));

        long low  = count.getOrDefault("low",  0L);
        long safe = count.getOrDefault("safe", 0L);
        long over = count.getOrDefault("over", 0L);
        long total = low + safe + over;

        if (total == 0) return emptyStockHealthSegments();

        int lowPct  = (int) Math.round((double) low  / total * 100);
        int overPct = (int) Math.round((double) over / total * 100);
        int safePct = 100 - lowPct - overPct;

        return List.of(
                StockHealthSegmentDto.builder()
                        .id("safe").label("Tồn kho an toàn")
                        .skuCount(safe).percentage(Math.max(0, safePct))
                        .color("#059669").description("Số lượng từ 20 - 100 sp")
                        .build(),
                StockHealthSegmentDto.builder()
                        .id("low").label("Cảnh báo sắp hết")
                        .skuCount(low).percentage(lowPct)
                        .color("#D97706").description("Dưới mức tối thiểu (< 20 sp)")
                        .build(),
                StockHealthSegmentDto.builder()
                        .id("over").label("Tồn vượt định mức")
                        .skuCount(over).percentage(overPct)
                        .color("#DC2626").description("Vượt quá 100 sp/SKU")
                        .build()
        );
    }

    private List<StockHealthSegmentDto> emptyStockHealthSegments() {
        return List.of(
                StockHealthSegmentDto.builder().id("safe").label("Tồn kho an toàn")
                        .skuCount(0).percentage(0).color("#059669").description("Số lượng từ 20 - 100 sp").build(),
                StockHealthSegmentDto.builder().id("low").label("Cảnh báo sắp hết")
                        .skuCount(0).percentage(0).color("#D97706").description("Dưới mức tối thiểu (< 20 sp)").build(),
                StockHealthSegmentDto.builder().id("over").label("Tồn vượt định mức")
                        .skuCount(0).percentage(0).color("#DC2626").description("Vượt quá 100 sp/SKU").build()
        );
    }

    private List<OrderStatusDistributionDto> buildOrderStatusDistribution() {
        List<Object[]> rows = purchaseOrderRepository.countAndSumByStatuses(ALL_PO_STATUSES);
        Map<PurchaseOrderStatus, Object[]> dataByStatus = rows.stream()
                .collect(Collectors.toMap(r -> (PurchaseOrderStatus) r[0], r -> r));

        long totalOrders = dataByStatus.values().stream().mapToLong(r -> toLong(r[1])).sum();

        List<OrderStatusDistributionDto> result = new ArrayList<>();
        for (PurchaseOrderStatus status : ALL_PO_STATUSES) {
            Object[] row = dataByStatus.get(status);
            long count     = row != null ? toLong(row[1])       : 0L;
            BigDecimal amt = row != null ? toBigDecimal(row[2]) : BigDecimal.ZERO;
            int pct = totalOrders > 0 ? (int) Math.round((double) count / totalOrders * 100) : 0;

            OrderStatusMeta meta = ORDER_STATUS_META.get(status);
            result.add(OrderStatusDistributionDto.builder()
                    .status(status.name()).label(meta.label())
                    .orderCount(count).totalAmount(amt)
                    .percentage(pct).color(meta.color())
                    .build());
        }
        adjustOrderPercentages(result, totalOrders);
        return result;
    }

    private List<TopValueProductDto> buildTopValueProducts() {
        return productVariantRepository.findTopValueVariants(TOP_VALUE_LIMIT).stream()
                .map(row -> TopValueProductDto.builder()
                        .id(String.valueOf(row[0]))
                        .productName(String.valueOf(row[1]))
                        .sku(String.valueOf(row[2]))
                        .category(String.valueOf(row[3]))
                        .quantity(toInt(row[4]))
                        .unitPrice(toBigDecimal(row[5]))
                        .totalValue(toBigDecimal(row[6]))
                        .build())
                .toList();
    }
    
    private String normalizeTimeframe(String tf) {
        if (tf == null) return "6months";
        return switch (tf.toLowerCase().trim()) {
            case "3months", "3month" -> "3months";
            case "year", "12months"  -> "year";
            default                  -> "6months";
        };
    }

    private int resolveMonths(String timeframe) {
        return switch (timeframe) {
            case "3months" -> 3;
            case "year"    -> 12;
            default        -> 6;
        };
    }

    private long toLong(Object obj) {
        if (obj == null) return 0L;
        if (obj instanceof Number num) return num.longValue();
        try { return Long.parseLong(obj.toString()); } catch (NumberFormatException e) { return 0L; }
    }

    private int toInt(Object obj) { return (int) toLong(obj); }

    private BigDecimal toBigDecimal(Object obj) {
        if (obj == null) return BigDecimal.ZERO;
        if (obj instanceof BigDecimal bd) return bd;
        if (obj instanceof Number num) return BigDecimal.valueOf(num.doubleValue()).setScale(2, RoundingMode.HALF_UP);
        try { return new BigDecimal(obj.toString()).setScale(2, RoundingMode.HALF_UP); }
        catch (Exception e) { return BigDecimal.ZERO; }
    }

    // Điều chỉnh tổng % = 100 (phần dư do làm tròn gán vào phần tử đầu)
    private void adjustCategoryPercentages(List<CategoryDistributionDto> list) {
        int sum = list.stream().mapToInt(CategoryDistributionDto::getPercentage).sum();
        if (!list.isEmpty() && sum != 100) {
            CategoryDistributionDto first = list.get(0);
            first.setPercentage(first.getPercentage() + (100 - sum));
        }
    }

    private void adjustOrderPercentages(List<OrderStatusDistributionDto> list, long total) {
        if (total == 0) return;
        int sum = list.stream().mapToInt(OrderStatusDistributionDto::getPercentage).sum();
        if (sum != 100) {
            list.stream()
                    .filter(d -> d.getOrderCount() > 0)
                    .max(Comparator.comparingLong(OrderStatusDistributionDto::getOrderCount))
                    .ifPresent(d -> d.setPercentage(d.getPercentage() + (100 - sum)));
        }
    }
}
