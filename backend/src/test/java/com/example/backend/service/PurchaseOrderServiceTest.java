package com.example.backend.service;

import com.example.backend.dto.request.PurchaseOrderDetailRequestDto;
import com.example.backend.dto.request.PurchaseOrderRequestDto;
import com.example.backend.dto.request.PurchaseOrderStatusUpdateRequestDto;
import com.example.backend.dto.response.PurchaseOrderResponseDto;
import com.example.backend.exception.ErrorCode;
import com.example.backend.exception.InvalidException;
import com.example.backend.mapper.PurchaseOrderDetailMapper;
import com.example.backend.mapper.PurchaseOrderMapper;
import com.example.backend.model.*;
import com.example.backend.model.enums.PurchaseOrderPaymentStatus;
import com.example.backend.model.enums.PurchaseOrderStatus;
import com.example.backend.model.enums.Status;
import com.example.backend.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("PurchaseOrderService Unit Tests")
class PurchaseOrderServiceTest {

    @Mock private PurchaseOrderRepository purchaseOrderRepository;
    @Mock private PurchaseOrderDetailRepository purchaseOrderDetailRepository;
    @Mock private InventoryTransactionRepository inventoryTransactionRepository;
    @Mock private SupplierRepository supplierRepository;
    @Mock private ProductVariantRepository productVariantRepository;
    @Mock private UserRepository userRepository;
    @Mock private PurchaseOrderMapper purchaseOrderMapper;
    @Mock private PurchaseOrderDetailMapper purchaseOrderDetailMapper;
    @Mock private CacheService cacheService;
    @Mock private SystemSettingService systemSettingService;

    @InjectMocks
    private PurchaseOrderService purchaseOrderService;

    private Supplier supplier;
    private User currentUser;
    private PurchaseOrder draftOrder;
    private PurchaseOrder pendingOrder;
    private PurchaseOrder receivedOrder;

    @BeforeEach
    void setUp() {
        supplier = new Supplier();
        supplier.setId(1L);
        supplier.setCode("NCC-001");
        supplier.setName("Nhà Cung Cấp A");

        currentUser = new User();
        currentUser.setId(1L);
        currentUser.setUuid("user-uuid-001");
        currentUser.setUsername("admin");

        draftOrder = PurchaseOrder.builder()
                .id(1L).code("PO-260923-0001")
                .supplier(supplier).createdBy(currentUser)
                .status(PurchaseOrderStatus.DRAFT)
                .paymentStatus(PurchaseOrderPaymentStatus.UNPAID)
                .totalAmount(BigDecimal.valueOf(5000000))
                .orderDate(LocalDateTime.now())
                .build();

        pendingOrder = PurchaseOrder.builder()
                .id(2L).code("PO-260923-0002")
                .supplier(supplier).createdBy(currentUser)
                .status(PurchaseOrderStatus.PENDING)
                .paymentStatus(PurchaseOrderPaymentStatus.UNPAID)
                .totalAmount(BigDecimal.valueOf(3000000))
                .orderDate(LocalDateTime.now())
                .build();

        receivedOrder = PurchaseOrder.builder()
                .id(3L).code("PO-260923-0003")
                .supplier(supplier).createdBy(currentUser)
                .status(PurchaseOrderStatus.RECEIVED)
                .paymentStatus(PurchaseOrderPaymentStatus.PAID)
                .totalAmount(BigDecimal.valueOf(2000000))
                .orderDate(LocalDateTime.now())
                .build();
    }

    // ─── getPurchaseOrderById() ───────────────────────────────────────────────

    @Test
    @DisplayName("getPurchaseOrderById() - không tồn tại → throw PURCHASE_ORDER_NOT_FOUND")
    void getPurchaseOrderById_notFound_throwsException() {
        when(purchaseOrderRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> purchaseOrderService.getPurchaseOrderById(999L))
                .isInstanceOf(InvalidException.class)
                .extracting(e -> ((InvalidException) e).getErrorCode())
                .isEqualTo(ErrorCode.PURCHASE_ORDER_NOT_FOUND);
    }

    @Test
    @DisplayName("getPurchaseOrderById() - thành công: trả về DTO")
    void getPurchaseOrderById_success_returnsDto() {
        PurchaseOrderResponseDto responseDto = new PurchaseOrderResponseDto();
        responseDto.setCode("PO-260923-0001");

        when(purchaseOrderRepository.findById(1L)).thenReturn(Optional.of(draftOrder));
        when(purchaseOrderMapper.toResponse(draftOrder)).thenReturn(responseDto);
        when(purchaseOrderDetailRepository.findByPurchaseOrderId(1L)).thenReturn(List.of());

        PurchaseOrderResponseDto result = purchaseOrderService.getPurchaseOrderById(1L);
        assertThat(result.getCode()).isEqualTo("PO-260923-0001");
    }

    // ─── createPurchaseOrder() ───────────────────────────────────────────────

    @Test
    @DisplayName("createPurchaseOrder() - supplier không tồn tại → throw SUPPLIER_NOT_FOUND")
    void createPurchaseOrder_supplierNotFound_throwsException() {
        when(supplierRepository.findById(99L)).thenReturn(Optional.empty());

        PurchaseOrderRequestDto req = buildPurchaseOrderRequest(99L, 10L, 5);

        assertThatThrownBy(() -> purchaseOrderService.createPurchaseOrder(req))
                .isInstanceOf(InvalidException.class)
                .extracting(e -> ((InvalidException) e).getErrorCode())
                .isEqualTo(ErrorCode.SUPPLIER_NOT_FOUND);
    }

    @Test
    @DisplayName("createPurchaseOrder() - variant không tồn tại → throw VARIANT_NOT_FOUND")
    void createPurchaseOrder_variantNotFound_throwsException() {
        setupSecurityContext();
        when(userRepository.findByUuid("user-uuid-001")).thenReturn(Optional.of(currentUser));
        when(cacheService.generatePurchaseOrderCode()).thenReturn("PO-260923-0001");
        when(supplierRepository.findById(1L)).thenReturn(Optional.of(supplier));
        when(purchaseOrderRepository.save(any(PurchaseOrder.class))).thenReturn(draftOrder);
        when(productVariantRepository.findById(999L)).thenReturn(Optional.empty());

        PurchaseOrderRequestDto req = buildPurchaseOrderRequest(1L, 999L, 10);

        assertThatThrownBy(() -> purchaseOrderService.createPurchaseOrder(req))
                .isInstanceOf(InvalidException.class)
                .extracting(e -> ((InvalidException) e).getErrorCode())
                .isEqualTo(ErrorCode.VARIANT_NOT_FOUND);
    }

    @Test
    @DisplayName("createPurchaseOrder() - thành công: lưu order, tính totalAmount")
    void createPurchaseOrder_success_savesOrder() {
        ProductVariant variant = new ProductVariant();
        variant.setId(10L);

        setupSecurityContext();
        when(userRepository.findByUuid("user-uuid-001")).thenReturn(Optional.of(currentUser));
        when(cacheService.generatePurchaseOrderCode()).thenReturn("PO-260923-0001");
        when(supplierRepository.findById(1L)).thenReturn(Optional.of(supplier));
        when(purchaseOrderRepository.save(any(PurchaseOrder.class))).thenReturn(draftOrder);
        when(productVariantRepository.findById(10L)).thenReturn(Optional.of(variant));
        when(purchaseOrderDetailRepository.saveAll(any())).thenReturn(List.of());
        when(purchaseOrderDetailRepository.findByPurchaseOrderId(anyLong())).thenReturn(List.of());

        PurchaseOrderResponseDto responseDto = new PurchaseOrderResponseDto();
        responseDto.setCode("PO-260923-0001");
        when(purchaseOrderMapper.toResponse(any(PurchaseOrder.class))).thenReturn(responseDto);

        PurchaseOrderRequestDto req = buildPurchaseOrderRequest(1L, 10L, 5);
        PurchaseOrderResponseDto result = purchaseOrderService.createPurchaseOrder(req);

        assertThat(result).isNotNull();
        assertThat(result.getCode()).isEqualTo("PO-260923-0001");
        verify(purchaseOrderRepository, times(2)).save(any(PurchaseOrder.class));
    }

    // ─── updateStatus() ──────────────────────────────────────────────────────

    @Test
    @DisplayName("updateStatus() - RECEIVED → PENDING (transition không hợp lệ) → throw exception")
    void updateStatus_invalidTransition_throwsException() {
        when(purchaseOrderRepository.findById(3L)).thenReturn(Optional.of(receivedOrder));

        PurchaseOrderStatusUpdateRequestDto req = new PurchaseOrderStatusUpdateRequestDto();
        req.setStatus(PurchaseOrderStatus.PENDING);

        assertThatThrownBy(() -> purchaseOrderService.updateStatus(3L, req))
                .isInstanceOf(InvalidException.class)
                .extracting(e -> ((InvalidException) e).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_PURCHASE_ORDER_STATUS_TRANSITION);
    }

    @Test
    @DisplayName("updateStatus() - DRAFT → PENDING: thành công")
    void updateStatus_draftToPending_success() {
        when(purchaseOrderRepository.findById(1L)).thenReturn(Optional.of(draftOrder));

        PurchaseOrderResponseDto responseDto = new PurchaseOrderResponseDto();
        responseDto.setCode("PO-260923-0001");
        when(purchaseOrderRepository.save(any())).thenReturn(draftOrder);
        when(purchaseOrderMapper.toResponse(any())).thenReturn(responseDto);
        when(purchaseOrderDetailRepository.findByPurchaseOrderId(1L)).thenReturn(List.of());

        PurchaseOrderStatusUpdateRequestDto req = new PurchaseOrderStatusUpdateRequestDto();
        req.setStatus(PurchaseOrderStatus.PENDING);

        PurchaseOrderResponseDto result = purchaseOrderService.updateStatus(1L, req);
        assertThat(result).isNotNull();
        assertThat(draftOrder.getStatus()).isEqualTo(PurchaseOrderStatus.PENDING);
    }

    @Test
    @DisplayName("updateStatus() - DRAFT → CANCELLED: thành công")
    void updateStatus_draftToCancelled_success() {
        when(purchaseOrderRepository.findById(1L)).thenReturn(Optional.of(draftOrder));

        PurchaseOrderResponseDto responseDto = new PurchaseOrderResponseDto();
        when(purchaseOrderRepository.save(any())).thenReturn(draftOrder);
        when(purchaseOrderMapper.toResponse(any())).thenReturn(responseDto);
        when(purchaseOrderDetailRepository.findByPurchaseOrderId(1L)).thenReturn(List.of());

        PurchaseOrderStatusUpdateRequestDto req = new PurchaseOrderStatusUpdateRequestDto();
        req.setStatus(PurchaseOrderStatus.CANCELLED);

        purchaseOrderService.updateStatus(1L, req);
        assertThat(draftOrder.getStatus()).isEqualTo(PurchaseOrderStatus.CANCELLED);
    }

    @Test
    @DisplayName("updateStatus() - PENDING → RECEIVED nhưng vượt capacity → throw WAREHOUSE_CAPACITY_EXCEEDED")
    void updateStatus_pendingToReceived_capacityExceeded_throwsException() {
        setupSecurityContext();
        when(userRepository.findByUuid("user-uuid-001")).thenReturn(Optional.of(currentUser));
        when(purchaseOrderRepository.findById(2L)).thenReturn(Optional.of(pendingOrder));

        PurchaseOrderDetail detail = buildDetail(pendingOrder, 100);
        when(purchaseOrderDetailRepository.findByPurchaseOrderId(2L)).thenReturn(List.of(detail));

        doThrow(new InvalidException(ErrorCode.WAREHOUSE_CAPACITY_EXCEEDED))
                .when(systemSettingService).validateCapacityForInbound(100L);

        PurchaseOrderStatusUpdateRequestDto req = new PurchaseOrderStatusUpdateRequestDto();
        req.setStatus(PurchaseOrderStatus.RECEIVED);

        assertThatThrownBy(() -> purchaseOrderService.updateStatus(2L, req))
                .isInstanceOf(InvalidException.class)
                .extracting(e -> ((InvalidException) e).getErrorCode())
                .isEqualTo(ErrorCode.WAREHOUSE_CAPACITY_EXCEEDED);
    }

    @Test
    @DisplayName("updateStatus() - PENDING → RECEIVED thành công: nhập kho, lưu transaction")
    void updateStatus_pendingToReceived_success() {
        setupSecurityContext();
        when(userRepository.findByUuid("user-uuid-001")).thenReturn(Optional.of(currentUser));
        when(purchaseOrderRepository.findById(2L)).thenReturn(Optional.of(pendingOrder));

        ProductVariant variant = new ProductVariant();
        variant.setId(10L);
        variant.setQuantityOnHand(20);
        variant.setStatus(Status.ACTIVE);

        PurchaseOrderDetail detail = buildDetail(pendingOrder, 50);
        detail.setVariant(variant);

        when(purchaseOrderDetailRepository.findByPurchaseOrderId(2L)).thenReturn(List.of(detail));
        doNothing().when(systemSettingService).validateCapacityForInbound(50L);
        when(productVariantRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(variant));
        when(inventoryTransactionRepository.saveAll(any())).thenReturn(List.of());

        PurchaseOrderResponseDto responseDto = new PurchaseOrderResponseDto();
        when(purchaseOrderRepository.save(any())).thenReturn(pendingOrder);
        when(purchaseOrderMapper.toResponse(any())).thenReturn(responseDto);
        // second call for buildResponseWithDetails
        when(purchaseOrderDetailRepository.findByPurchaseOrderId(2L)).thenReturn(List.of(detail));

        PurchaseOrderStatusUpdateRequestDto req = new PurchaseOrderStatusUpdateRequestDto();
        req.setStatus(PurchaseOrderStatus.RECEIVED);

        PurchaseOrderResponseDto result = purchaseOrderService.updateStatus(2L, req);
        assertThat(result).isNotNull();
        assertThat(variant.getQuantityOnHand()).isEqualTo(70);
        verify(inventoryTransactionRepository).saveAll(any());
    }

    // ─── updatePurchaseOrder() ───────────────────────────────────────────────

    @Test
    @DisplayName("updatePurchaseOrder() - order đã RECEIVED → throw PURCHASE_ORDER_CANNOT_BE_MODIFIED")
    void updatePurchaseOrder_received_throwsException() {
        when(purchaseOrderRepository.findById(3L)).thenReturn(Optional.of(receivedOrder));

        PurchaseOrderRequestDto req = buildPurchaseOrderRequest(1L, 10L, 5);

        assertThatThrownBy(() -> purchaseOrderService.updatePurchaseOrder(3L, req))
                .isInstanceOf(InvalidException.class)
                .extracting(e -> ((InvalidException) e).getErrorCode())
                .isEqualTo(ErrorCode.PURCHASE_ORDER_CANNOT_BE_MODIFIED);
    }

    @Test
    @DisplayName("updatePurchaseOrder() - supplier không tồn tại → throw SUPPLIER_NOT_FOUND")
    void updatePurchaseOrder_supplierNotFound_throwsException() {
        when(purchaseOrderRepository.findById(1L)).thenReturn(Optional.of(draftOrder));
        when(supplierRepository.findById(99L)).thenReturn(Optional.empty());

        PurchaseOrderRequestDto req = buildPurchaseOrderRequest(99L, 10L, 5);

        assertThatThrownBy(() -> purchaseOrderService.updatePurchaseOrder(1L, req))
                .isInstanceOf(InvalidException.class)
                .extracting(e -> ((InvalidException) e).getErrorCode())
                .isEqualTo(ErrorCode.SUPPLIER_NOT_FOUND);
    }

    @Test
    @DisplayName("updatePurchaseOrder() - order không tồn tại → throw PURCHASE_ORDER_NOT_FOUND")
    void updatePurchaseOrder_notFound_throwsException() {
        when(purchaseOrderRepository.findById(999L)).thenReturn(Optional.empty());

        PurchaseOrderRequestDto req = buildPurchaseOrderRequest(1L, 10L, 5);

        assertThatThrownBy(() -> purchaseOrderService.updatePurchaseOrder(999L, req))
                .isInstanceOf(InvalidException.class)
                .extracting(e -> ((InvalidException) e).getErrorCode())
                .isEqualTo(ErrorCode.PURCHASE_ORDER_NOT_FOUND);
    }

    @Test
    @DisplayName("updatePurchaseOrder() - thành công: xóa detail cũ, lưu detail mới, cập nhật totalAmount")
    void updatePurchaseOrder_success_updatesDetailsAndTotal() {
        when(purchaseOrderRepository.findById(1L)).thenReturn(Optional.of(draftOrder));
        when(supplierRepository.findById(1L)).thenReturn(Optional.of(supplier));

        ProductVariant variant = new ProductVariant();
        variant.setId(10L);
        when(productVariantRepository.findById(10L)).thenReturn(Optional.of(variant));

        PurchaseOrderResponseDto responseDto = new PurchaseOrderResponseDto();
        when(purchaseOrderMapper.toResponse(any())).thenReturn(responseDto);
        when(purchaseOrderDetailRepository.findByPurchaseOrderId(1L)).thenReturn(List.of());
        when(purchaseOrderRepository.save(any())).thenReturn(draftOrder);

        PurchaseOrderRequestDto req = buildPurchaseOrderRequest(1L, 10L, 3);
        req.setNote("Updated Note");

        PurchaseOrderResponseDto result = purchaseOrderService.updatePurchaseOrder(1L, req);

        assertThat(result).isNotNull();
        assertThat(draftOrder.getNote()).isEqualTo("Updated Note");
        verify(purchaseOrderDetailRepository).deleteByPurchaseOrderId(1L);
        verify(purchaseOrderDetailRepository).saveAll(any());
        verify(purchaseOrderRepository).save(draftOrder);
    }

    // ─── Additional updateStatus transitions & edge cases ─────────────────────

    @Test
    @DisplayName("updateStatus() - PENDING → CANCELLED: thành công")
    void updateStatus_pendingToCancelled_success() {
        when(purchaseOrderRepository.findById(2L)).thenReturn(Optional.of(pendingOrder));

        PurchaseOrderResponseDto responseDto = new PurchaseOrderResponseDto();
        when(purchaseOrderRepository.save(any())).thenReturn(pendingOrder);
        when(purchaseOrderMapper.toResponse(any())).thenReturn(responseDto);
        when(purchaseOrderDetailRepository.findByPurchaseOrderId(2L)).thenReturn(List.of());

        PurchaseOrderStatusUpdateRequestDto req = new PurchaseOrderStatusUpdateRequestDto();
        req.setStatus(PurchaseOrderStatus.CANCELLED);

        PurchaseOrderResponseDto result = purchaseOrderService.updateStatus(2L, req);
        assertThat(result).isNotNull();
        assertThat(pendingOrder.getStatus()).isEqualTo(PurchaseOrderStatus.CANCELLED);
    }

    @Test
    @DisplayName("updateStatus() - PENDING → RECEIVED nhưng variant không tìm thấy → throw VARIANT_NOT_FOUND")
    void updateStatus_pendingToReceived_variantNotFound_throwsException() {
        setupSecurityContext();
        when(userRepository.findByUuid("user-uuid-001")).thenReturn(Optional.of(currentUser));
        when(purchaseOrderRepository.findById(2L)).thenReturn(Optional.of(pendingOrder));

        ProductVariant variant = new ProductVariant();
        variant.setId(99L);
        PurchaseOrderDetail detail = buildDetail(pendingOrder, 10);
        detail.setVariant(variant);

        when(purchaseOrderDetailRepository.findByPurchaseOrderId(2L)).thenReturn(List.of(detail));
        doNothing().when(systemSettingService).validateCapacityForInbound(anyLong());
        when(productVariantRepository.findByIdForUpdate(99L)).thenReturn(Optional.empty());

        PurchaseOrderStatusUpdateRequestDto req = new PurchaseOrderStatusUpdateRequestDto();
        req.setStatus(PurchaseOrderStatus.RECEIVED);

        assertThatThrownBy(() -> purchaseOrderService.updateStatus(2L, req))
                .isInstanceOf(InvalidException.class)
                .extracting(e -> ((InvalidException) e).getErrorCode())
                .isEqualTo(ErrorCode.VARIANT_NOT_FOUND);
    }

    @Test
    @DisplayName("createPurchaseOrder() - getCurrentUser không tìm thấy trong DB → throw ACCOUNT_NOT_FOUND")
    void createPurchaseOrder_currentUserNotFound_throwsException() {
        when(supplierRepository.findById(1L)).thenReturn(Optional.of(supplier));
        setupSecurityContext();
        when(userRepository.findByUuid("user-uuid-001")).thenReturn(Optional.empty());

        PurchaseOrderRequestDto req = buildPurchaseOrderRequest(1L, 10L, 5);

        assertThatThrownBy(() -> purchaseOrderService.createPurchaseOrder(req))
                .isInstanceOf(InvalidException.class)
                .extracting(e -> ((InvalidException) e).getErrorCode())
                .isEqualTo(ErrorCode.ACCOUNT_NOT_FOUND);
    }

    // ─── getAllPurchaseOrders() & Specifications ──────────────────────────────

    @Test
    @DisplayName("getAllPurchaseOrders() - trả về danh sách phân trang")
    void getAllPurchaseOrders_returnsPagedOrders() {
        org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(0, 10);
        org.springframework.data.domain.Page<PurchaseOrder> page = new org.springframework.data.domain.PageImpl<>(List.of(draftOrder), pageable, 1);

        when(purchaseOrderRepository.findAll(any(org.springframework.data.jpa.domain.Specification.class), eq(pageable)))
                .thenReturn(page);
        PurchaseOrderResponseDto responseDto = new PurchaseOrderResponseDto();
        when(purchaseOrderMapper.toResponse(draftOrder)).thenReturn(responseDto);
        when(purchaseOrderDetailRepository.findByPurchaseOrderId(anyLong())).thenReturn(List.of());

        var result = purchaseOrderService.getAllPurchaseOrders("PO", PurchaseOrderStatus.DRAFT,
                LocalDateTime.now().minusDays(1), LocalDateTime.now(), 1L, pageable);

        assertThat(result.items()).hasSize(1);
    }

    @Test
    @DisplayName("getAllPurchaseOrders() - verify specification lambda toPredicate execution")
    @SuppressWarnings("unchecked")
    void getAllPurchaseOrders_specificationPredicate_executed() {
        org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(0, 10);
        when(purchaseOrderRepository.findAll(any(org.springframework.data.jpa.domain.Specification.class), eq(pageable)))
                .thenReturn(new org.springframework.data.domain.PageImpl<>(List.of()));

        LocalDateTime now = LocalDateTime.now();
        purchaseOrderService.getAllPurchaseOrders("test", PurchaseOrderStatus.DRAFT, now, now, 1L, pageable);

        org.mockito.ArgumentCaptor<org.springframework.data.jpa.domain.Specification<PurchaseOrder>> captor =
                org.mockito.ArgumentCaptor.forClass(org.springframework.data.jpa.domain.Specification.class);
        verify(purchaseOrderRepository, atLeastOnce()).findAll(captor.capture(), eq(pageable));
        org.springframework.data.jpa.domain.Specification<PurchaseOrder> spec = captor.getValue();

        jakarta.persistence.criteria.Root<PurchaseOrder> root = mock(jakarta.persistence.criteria.Root.class);
        jakarta.persistence.criteria.CriteriaQuery<?> query = mock(jakarta.persistence.criteria.CriteriaQuery.class);
        jakarta.persistence.criteria.CriteriaBuilder cb = mock(jakarta.persistence.criteria.CriteriaBuilder.class);

        jakarta.persistence.criteria.Path pathStatus = mock(jakarta.persistence.criteria.Path.class);
        jakarta.persistence.criteria.Path pathCode = mock(jakarta.persistence.criteria.Path.class);
        jakarta.persistence.criteria.Path pathSupplier = mock(jakarta.persistence.criteria.Path.class);
        jakarta.persistence.criteria.Path pathSupplierName = mock(jakarta.persistence.criteria.Path.class);
        jakarta.persistence.criteria.Path pathSupplierId = mock(jakarta.persistence.criteria.Path.class);
        jakarta.persistence.criteria.Path pathOrderDate = mock(jakarta.persistence.criteria.Path.class);

        jakarta.persistence.criteria.Predicate predNotCancelled = mock(jakarta.persistence.criteria.Predicate.class);
        jakarta.persistence.criteria.Predicate predStatusEqual = mock(jakarta.persistence.criteria.Predicate.class);
        jakarta.persistence.criteria.Predicate predGte = mock(jakarta.persistence.criteria.Predicate.class);
        jakarta.persistence.criteria.Predicate predLte = mock(jakarta.persistence.criteria.Predicate.class);
        jakarta.persistence.criteria.Predicate predSupplierEqual = mock(jakarta.persistence.criteria.Predicate.class);
        jakarta.persistence.criteria.Predicate predOr = mock(jakarta.persistence.criteria.Predicate.class);
        jakarta.persistence.criteria.Predicate predAnd = mock(jakarta.persistence.criteria.Predicate.class);

        when(root.get("status")).thenReturn(pathStatus);
        when(root.get("code")).thenReturn(pathCode);
        when(root.get("supplier")).thenReturn(pathSupplier);
        when(pathSupplier.get("name")).thenReturn(pathSupplierName);
        when(pathSupplier.get("id")).thenReturn(pathSupplierId);
        when(root.get("orderDate")).thenReturn(pathOrderDate);

        when(cb.notEqual(pathStatus, PurchaseOrderStatus.CANCELLED)).thenReturn(predNotCancelled);
        when(cb.equal(pathStatus, PurchaseOrderStatus.DRAFT)).thenReturn(predStatusEqual);
        when(cb.greaterThanOrEqualTo(pathOrderDate, now)).thenReturn(predGte);
        when(cb.lessThanOrEqualTo(pathOrderDate, now)).thenReturn(predLte);
        when(cb.equal(pathSupplierId, 1L)).thenReturn(predSupplierEqual);

        jakarta.persistence.criteria.Expression expLower = mock(jakarta.persistence.criteria.Expression.class);
        when(cb.lower(any())).thenReturn(expLower);
        jakarta.persistence.criteria.Predicate predLike = mock(jakarta.persistence.criteria.Predicate.class);
        when(cb.like(any(), anyString())).thenReturn(predLike);
        when(cb.or(any(jakarta.persistence.criteria.Predicate[].class))).thenReturn(predOr);
        when(cb.and(any(jakarta.persistence.criteria.Predicate[].class))).thenReturn(predAnd);

        jakarta.persistence.criteria.Predicate result = spec.toPredicate(root, query, cb);
        assertThat(result).isNotNull();

        // Also test null filters
        purchaseOrderService.getAllPurchaseOrders(null, null, null, null, null, pageable);
        captor = org.mockito.ArgumentCaptor.forClass(org.springframework.data.jpa.domain.Specification.class);
        verify(purchaseOrderRepository, atLeastOnce()).findAll(captor.capture(), eq(pageable));
        org.springframework.data.jpa.domain.Specification<PurchaseOrder> specNull = captor.getValue();
        jakarta.persistence.criteria.Predicate resultNull = specNull.toPredicate(root, query, cb);
        assertThat(resultNull).isNotNull();
    }

    // ─── getReceivedPurchaseOrders() & Specifications ─────────────────────────

    @Test
    @DisplayName("getReceivedPurchaseOrders() - trả về danh sách phân trang")
    void getReceivedPurchaseOrders_returnsPagedOrders() {
        org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(0, 10);
        org.springframework.data.domain.Page<PurchaseOrder> page = new org.springframework.data.domain.PageImpl<>(List.of(receivedOrder), pageable, 1);

        when(purchaseOrderRepository.findAll(any(org.springframework.data.jpa.domain.Specification.class), eq(pageable)))
                .thenReturn(page);
        PurchaseOrderResponseDto responseDto = new PurchaseOrderResponseDto();
        when(purchaseOrderMapper.toResponse(receivedOrder)).thenReturn(responseDto);
        when(purchaseOrderDetailRepository.findByPurchaseOrderId(anyLong())).thenReturn(List.of());

        var result = purchaseOrderService.getReceivedPurchaseOrders("PO", PurchaseOrderStatus.RECEIVED,
                null, null, null, pageable);

        assertThat(result.items()).hasSize(1);
    }

    @Test
    @DisplayName("getReceivedPurchaseOrders() - verify specification lambda toPredicate execution")
    @SuppressWarnings("unchecked")
    void getReceivedPurchaseOrders_specificationPredicate_executed() {
        org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(0, 10);
        when(purchaseOrderRepository.findAll(any(org.springframework.data.jpa.domain.Specification.class), eq(pageable)))
                .thenReturn(new org.springframework.data.domain.PageImpl<>(List.of()));

        LocalDateTime now = LocalDateTime.now();
        purchaseOrderService.getReceivedPurchaseOrders("test", null, now, now, 1L, pageable);

        org.mockito.ArgumentCaptor<org.springframework.data.jpa.domain.Specification<PurchaseOrder>> captor =
                org.mockito.ArgumentCaptor.forClass(org.springframework.data.jpa.domain.Specification.class);
        verify(purchaseOrderRepository, atLeastOnce()).findAll(captor.capture(), eq(pageable));
        org.springframework.data.jpa.domain.Specification<PurchaseOrder> spec = captor.getValue();

        jakarta.persistence.criteria.Root<PurchaseOrder> root = mock(jakarta.persistence.criteria.Root.class);
        jakarta.persistence.criteria.CriteriaQuery<?> query = mock(jakarta.persistence.criteria.CriteriaQuery.class);
        jakarta.persistence.criteria.CriteriaBuilder cb = mock(jakarta.persistence.criteria.CriteriaBuilder.class);

        jakarta.persistence.criteria.Path pathStatus = mock(jakarta.persistence.criteria.Path.class);
        jakarta.persistence.criteria.Path pathCode = mock(jakarta.persistence.criteria.Path.class);
        jakarta.persistence.criteria.Path pathSupplier = mock(jakarta.persistence.criteria.Path.class);
        jakarta.persistence.criteria.Path pathSupplierName = mock(jakarta.persistence.criteria.Path.class);
        jakarta.persistence.criteria.Path pathSupplierId = mock(jakarta.persistence.criteria.Path.class);
        jakarta.persistence.criteria.Path pathReceivedDate = mock(jakarta.persistence.criteria.Path.class);

        jakarta.persistence.criteria.Predicate predStatusEqual = mock(jakarta.persistence.criteria.Predicate.class);
        jakarta.persistence.criteria.Predicate predGte = mock(jakarta.persistence.criteria.Predicate.class);
        jakarta.persistence.criteria.Predicate predLte = mock(jakarta.persistence.criteria.Predicate.class);
        jakarta.persistence.criteria.Predicate predSupplierEqual = mock(jakarta.persistence.criteria.Predicate.class);
        jakarta.persistence.criteria.Predicate predOr = mock(jakarta.persistence.criteria.Predicate.class);
        jakarta.persistence.criteria.Predicate predAnd = mock(jakarta.persistence.criteria.Predicate.class);

        when(root.get("status")).thenReturn(pathStatus);
        when(root.get("code")).thenReturn(pathCode);
        when(root.get("supplier")).thenReturn(pathSupplier);
        when(pathSupplier.get("name")).thenReturn(pathSupplierName);
        when(pathSupplier.get("id")).thenReturn(pathSupplierId);
        when(root.get("receivedDate")).thenReturn(pathReceivedDate);

        when(cb.equal(pathStatus, PurchaseOrderStatus.RECEIVED)).thenReturn(predStatusEqual);
        when(cb.greaterThanOrEqualTo(pathReceivedDate, now)).thenReturn(predGte);
        when(cb.lessThanOrEqualTo(pathReceivedDate, now)).thenReturn(predLte);
        when(cb.equal(pathSupplierId, 1L)).thenReturn(predSupplierEqual);

        jakarta.persistence.criteria.Expression expLower = mock(jakarta.persistence.criteria.Expression.class);
        when(cb.lower(any())).thenReturn(expLower);
        jakarta.persistence.criteria.Predicate predLike = mock(jakarta.persistence.criteria.Predicate.class);
        when(cb.like(any(), anyString())).thenReturn(predLike);
        when(cb.or(any(jakarta.persistence.criteria.Predicate[].class))).thenReturn(predOr);
        when(cb.and(any(jakarta.persistence.criteria.Predicate[].class))).thenReturn(predAnd);

        jakarta.persistence.criteria.Predicate result = spec.toPredicate(root, query, cb);
        assertThat(result).isNotNull();

        // Also test null filters and non-null status
        purchaseOrderService.getReceivedPurchaseOrders(null, PurchaseOrderStatus.PENDING, null, null, null, pageable);
        captor = org.mockito.ArgumentCaptor.forClass(org.springframework.data.jpa.domain.Specification.class);
        verify(purchaseOrderRepository, atLeastOnce()).findAll(captor.capture(), eq(pageable));
        org.springframework.data.jpa.domain.Specification<PurchaseOrder> specNull = captor.getValue();
        jakarta.persistence.criteria.Predicate resultNull = specNull.toPredicate(root, query, cb);
        assertThat(resultNull).isNotNull();
    }

    // ─── Helper methods ───────────────────────────────────────────────────────

    private void setupSecurityContext() {
        Authentication auth = mock(Authentication.class);
        when(auth.getName()).thenReturn("user-uuid-001");
        SecurityContext secCtx = mock(SecurityContext.class);
        when(secCtx.getAuthentication()).thenReturn(auth);
        SecurityContextHolder.setContext(secCtx);
    }

    private PurchaseOrderRequestDto buildPurchaseOrderRequest(Long supplierId, Long variantId, int qty) {
        PurchaseOrderDetailRequestDto detail = new PurchaseOrderDetailRequestDto();
        detail.setVariantId(variantId);
        detail.setQuantity(qty);
        detail.setUnitPrice(BigDecimal.valueOf(100000));

        PurchaseOrderRequestDto req = new PurchaseOrderRequestDto();
        req.setSupplierId(supplierId);
        req.setOrderDate(LocalDateTime.now());
        req.setDetails(List.of(detail));
        return req;
    }

    private PurchaseOrderDetail buildDetail(PurchaseOrder order, int quantity) {
        ProductVariant variant = new ProductVariant();
        variant.setId(10L);
        variant.setQuantityOnHand(10);
        variant.setStatus(Status.ACTIVE);

        return PurchaseOrderDetail.builder()
                .id(1L)
                .purchaseOrder(order)
                .variant(variant)
                .quantity(quantity)
                .unitPrice(BigDecimal.valueOf(100000))
                .build();
    }
}
