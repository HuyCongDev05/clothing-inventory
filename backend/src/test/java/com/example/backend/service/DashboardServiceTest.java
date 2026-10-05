package com.example.backend.service;

import com.example.backend.dto.response.DashboardAnalyticsResponseDto;
import com.example.backend.dto.response.DashboardResponseDto;
import com.example.backend.dto.response.WarehouseCapacityDto;
import com.example.backend.model.enums.PurchaseOrderStatus;
import com.example.backend.repository.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("DashboardService Unit Tests")
class DashboardServiceTest {

    @Mock private PaymentRepository paymentRepository;
    @Mock private ProductRepository productRepository;
    @Mock private SupplierRepository supplierRepository;
    @Mock private ProductVariantRepository productVariantRepository;
    @Mock private CategoryRepository categoryRepository;
    @Mock private InventoryTransactionRepository inventoryTransactionRepository;
    @Mock private PurchaseOrderRepository purchaseOrderRepository;
    @Mock private SystemSettingService systemSettingService;

    @InjectMocks
    private DashboardService dashboardService;

    // ─── getParameterDashboard() ─────────────────────────────────────────────

    @Test
    @DisplayName("getParameterDashboard() - trả về đúng các chỉ số tổng hợp")
    void getParameterDashboard_returnsAggregatedMetrics() {
        when(paymentRepository.sumAllAmount()).thenReturn(BigDecimal.valueOf(15000000));
        when(productRepository.sumAllProduct()).thenReturn(120L);
        when(supplierRepository.sumAllSupplier()).thenReturn(15L);
        when(productVariantRepository.sumAllQuantityOnHand()).thenReturn(2500L);

        WarehouseCapacityDto capacityDto = WarehouseCapacityDto.builder()
                .maxCapacity(20000L)
                .currentInventory(2500L)
                .remainingCapacity(17500L)
                .occupancyRate(12.5)
                .status("SAFE")
                .statusMessage("Kho hàng an toàn")
                .build();
        when(systemSettingService.getWarehouseCapacityInfo()).thenReturn(capacityDto);

        DashboardResponseDto result = dashboardService.getParameterDashboard();

        assertThat(result).isNotNull();
        assertThat(result.getTotalAmount()).isEqualTo(BigDecimal.valueOf(15000000));
        assertThat(result.getTotalProduct()).isEqualTo(120L);
        assertThat(result.getTotalSupplier()).isEqualTo(15L);
        assertThat(result.getTotalInventory()).isEqualTo(2500L);
        assertThat(result.getWarehouseMaxCapacity()).isEqualTo(20000L);
        assertThat(result.getWarehouseCapacityStatus()).isEqualTo("SAFE");
    }

    // ─── getAnalytics() với các timeframe khác nhau ───────────────────────────

    @Test
    @DisplayName("getAnalytics() - timeframe 3months: tính toán đúng các dữ liệu phân tích")
    void getAnalytics_timeframe3Months_success() {
        mockRepositoriesForAnalytics();

        DashboardAnalyticsResponseDto result = dashboardService.getAnalytics("3months");

        assertThat(result).isNotNull();
        assertThat(result.getTimeframe()).isEqualTo("3months");
        assertThat(result.getMonthlyMovements()).hasSize(3);
        assertThat(result.getCategoryDistribution()).isNotEmpty();
        assertThat(result.getStockHealthSegments()).isNotEmpty();
        assertThat(result.getOrderStatusDistribution()).isNotEmpty();
        assertThat(result.getTopValueProducts()).isNotEmpty();
    }

    @Test
    @DisplayName("getAnalytics() - timeframe year (12months): tính toán đúng")
    void getAnalytics_timeframeYear_success() {
        mockRepositoriesForAnalytics();

        DashboardAnalyticsResponseDto result = dashboardService.getAnalytics("year");

        assertThat(result).isNotNull();
        assertThat(result.getTimeframe()).isEqualTo("year");
        assertThat(result.getMonthlyMovements()).hasSize(12);
    }

    @Test
    @DisplayName("getAnalytics() - timeframe null hoặc mặc định: chuẩn hóa thành 6months")
    void getAnalytics_timeframeNullOrUnknown_defaultsTo6Months() {
        mockRepositoriesForAnalytics();

        DashboardAnalyticsResponseDto result1 = dashboardService.getAnalytics(null);
        assertThat(result1.getTimeframe()).isEqualTo("6months");
        assertThat(result1.getMonthlyMovements()).hasSize(6);

        DashboardAnalyticsResponseDto result2 = dashboardService.getAnalytics("unknown");
        assertThat(result2.getTimeframe()).isEqualTo("6months");
    }

    // ─── Edge cases cho Category Distribution ─────────────────────────────────

    @Test
    @DisplayName("getAnalytics() - category distribution rỗng hoặc tổng số lượng = 0")
    void getAnalytics_emptyCategoryDistribution_returnsEmptyList() {
        // Case 1: rows empty
        when(categoryRepository.findCategoryStockDistribution()).thenReturn(Collections.emptyList());
        DashboardAnalyticsResponseDto result1 = dashboardService.getAnalytics("3months");
        assertThat(result1.getCategoryDistribution()).isEmpty();

        // Case 2: grandTotal == 0
        Object[] zeroRow = new Object[]{"1", "Áo", 0L, BigDecimal.ZERO};
        when(categoryRepository.findCategoryStockDistribution()).thenReturn(List.<Object[]>of(zeroRow));
        DashboardAnalyticsResponseDto result2 = dashboardService.getAnalytics("3months");
        assertThat(result2.getCategoryDistribution()).isEmpty();
    }

    // ─── Edge cases cho Stock Health Segments ─────────────────────────────────

    @Test
    @DisplayName("getAnalytics() - stock health tổng số lượng = 0 → trả về emptyStockHealthSegments")
    void getAnalytics_emptyStockHealth_returnsDefaultSegments() {
        when(productVariantRepository.countSkuByStockHealthSegment()).thenReturn(Collections.emptyList());

        DashboardAnalyticsResponseDto result = dashboardService.getAnalytics("3months");
        assertThat(result.getStockHealthSegments()).hasSize(3);
        assertThat(result.getStockHealthSegments().get(0).getSkuCount()).isEqualTo(0);
        assertThat(result.getStockHealthSegments().get(0).getPercentage()).isEqualTo(0);
    }

    // ─── Edge cases cho Order Status Distribution ─────────────────────────────

    @Test
    @DisplayName("getAnalytics() - purchase orders rỗng (totalOrders = 0)")
    void getAnalytics_emptyPurchaseOrders_handled() {
        when(purchaseOrderRepository.countAndSumByStatuses(anyList())).thenReturn(Collections.emptyList());

        DashboardAnalyticsResponseDto result = dashboardService.getAnalytics("3months");
        assertThat(result.getOrderStatusDistribution()).hasSize(4);
        assertThat(result.getOrderStatusDistribution().get(0).getOrderCount()).isEqualTo(0L);
    }

    // ─── Helper method mock chung ─────────────────────────────────────────────

    private void mockRepositoriesForAnalytics() {
        when(productVariantRepository.sumAllQuantityOnHand()).thenReturn(1000L);

        // Monthly movements row: [YearMonth (e.g. 202609), Inbound, Outbound, InboundValue, OutboundValue]
        Object[] movementRow = new Object[]{"202609", 100L, 40L, BigDecimal.valueOf(5000000), BigDecimal.valueOf(2000000)};
        when(inventoryTransactionRepository.findMonthlyMovements(any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(List.<Object[]>of(movementRow));

        // Category distribution: [id, name, quantity, totalValue]
        Object[] cat1 = new Object[]{"1", "Áo thun", 300L, BigDecimal.valueOf(10000000)};
        Object[] cat2 = new Object[]{"2", "Quần jean", 700L, BigDecimal.valueOf(25000000)};
        when(categoryRepository.findCategoryStockDistribution()).thenReturn(List.<Object[]>of(cat1, cat2));

        // Stock health: [segment, count]
        Object[] segLow = new Object[]{"low", 15L};
        Object[] segSafe = new Object[]{"safe", 80L};
        Object[] segOver = new Object[]{"over", 5L};
        when(productVariantRepository.countSkuByStockHealthSegment()).thenReturn(List.<Object[]>of(segLow, segSafe, segOver));

        // Order status: [status, count, totalAmount]
        Object[] orderReceived = new Object[]{PurchaseOrderStatus.RECEIVED, 10L, BigDecimal.valueOf(20000000)};
        Object[] orderPending = new Object[]{PurchaseOrderStatus.PENDING, 5L, BigDecimal.valueOf(8000000)};
        when(purchaseOrderRepository.countAndSumByStatuses(anyList()))
                .thenReturn(List.<Object[]>of(orderReceived, orderPending));

        // Top value variants: [id, productName, sku, category, quantity, unitPrice, totalValue]
        Object[] topVariant = new Object[]{"10", "Áo Polo", "SP-001", "Áo", 50, BigDecimal.valueOf(150000), BigDecimal.valueOf(7500000)};
        when(productVariantRepository.findTopValueVariants(5)).thenReturn(List.<Object[]>of(topVariant));
    }
}
