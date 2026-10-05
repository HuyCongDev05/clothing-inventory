package com.example.backend.service;

import com.example.backend.dto.request.PaymentRequestDto;
import com.example.backend.dto.response.PageResponseDto;
import com.example.backend.dto.response.PaymentResponseDto;
import com.example.backend.exception.ErrorCode;
import com.example.backend.exception.InvalidException;
import com.example.backend.mapper.PaymentMapper;
import com.example.backend.model.*;
import com.example.backend.model.enums.PurchaseOrderPaymentStatus;
import com.example.backend.model.enums.PurchaseOrderStatus;
import com.example.backend.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;
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

@ExtendWith(MockitoExtension.class)
@DisplayName("PaymentService Unit Tests")
class PaymentServiceTest {

    @Mock private PaymentRepository paymentRepository;
    @Mock private PaymentMethodRepository paymentMethodRepository;
    @Mock private PurchaseOrderRepository purchaseOrderRepository;
    @Mock private UserRepository userRepository;
    @Mock private PaymentMapper paymentMapper;

    @InjectMocks
    private PaymentService paymentService;

    private PurchaseOrder purchaseOrder;
    private PaymentMethod paymentMethod;
    private User currentUser;

    @BeforeEach
    void setUp() {
        currentUser = new User();
        currentUser.setId(1L);
        currentUser.setUuid("user-uuid-001");
        currentUser.setUsername("admin");

        purchaseOrder = PurchaseOrder.builder()
                .id(1L)
                .code("PO-260923-0001")
                .totalAmount(BigDecimal.valueOf(1_000_000))
                .status(PurchaseOrderStatus.RECEIVED)
                .paymentStatus(PurchaseOrderPaymentStatus.UNPAID)
                .orderDate(LocalDateTime.now())
                .build();

        paymentMethod = new PaymentMethod();
        paymentMethod.setId(1L);
    }

    // ─── createPayment() ─────────────────────────────────────────────────────

    @Test
    @DisplayName("createPayment() - purchase order không tồn tại → throw PURCHASE_ORDER_NOT_FOUND")
    void createPayment_orderNotFound_throwsException() {
        when(purchaseOrderRepository.findByIdForUpdate(99L)).thenReturn(Optional.empty());

        PaymentRequestDto req = buildPaymentRequest(99L, 1L, BigDecimal.valueOf(500_000));

        assertThatThrownBy(() -> paymentService.createPayment(req))
                .isInstanceOf(InvalidException.class)
                .extracting(e -> ((InvalidException) e).getErrorCode())
                .isEqualTo(ErrorCode.PURCHASE_ORDER_NOT_FOUND);
    }

    @Test
    @DisplayName("createPayment() - payment method không tồn tại → throw PAYMENT_METHOD_NOT_FOUND")
    void createPayment_paymentMethodNotFound_throwsException() {
        when(purchaseOrderRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(purchaseOrder));
        when(paymentMethodRepository.findById(99L)).thenReturn(Optional.empty());

        PaymentRequestDto req = buildPaymentRequest(1L, 99L, BigDecimal.valueOf(500_000));

        assertThatThrownBy(() -> paymentService.createPayment(req))
                .isInstanceOf(InvalidException.class)
                .extracting(e -> ((InvalidException) e).getErrorCode())
                .isEqualTo(ErrorCode.PAYMENT_METHOD_NOT_FOUND);
    }

    @Test
    @DisplayName("createPayment() - amount vượt remaining → throw PAYMENT_AMOUNT_EXCEEDS_REMAINING")
    void createPayment_amountExceedsRemaining_throwsException() {
        when(purchaseOrderRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(purchaseOrder));
        when(paymentMethodRepository.findById(1L)).thenReturn(Optional.of(paymentMethod));
        // Đã thanh toán 400.000, còn lại 600.000
        when(paymentRepository.sumAmountByPurchaseOrderId(1L))
                .thenReturn(BigDecimal.valueOf(400_000));

        // Request thanh toán 700.000 → vượt quá 600.000 remaining
        PaymentRequestDto req = buildPaymentRequest(1L, 1L, BigDecimal.valueOf(700_000));

        assertThatThrownBy(() -> paymentService.createPayment(req))
                .isInstanceOf(InvalidException.class)
                .extracting(e -> ((InvalidException) e).getErrorCode())
                .isEqualTo(ErrorCode.PAYMENT_AMOUNT_EXCEEDS_REMAINING);
    }

    @Test
    @DisplayName("createPayment() - thanh toán đủ → paymentStatus = PAID")
    void createPayment_fullPayment_setsPaidStatus() {
        setupSecurityContext();
        when(userRepository.findByUuid("user-uuid-001")).thenReturn(Optional.of(currentUser));
        when(purchaseOrderRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(purchaseOrder));
        when(paymentMethodRepository.findById(1L)).thenReturn(Optional.of(paymentMethod));
        // Chưa trả gì cả → remaining = 1.000.000
        when(paymentRepository.sumAmountByPurchaseOrderId(1L)).thenReturn(BigDecimal.ZERO);

        Payment savedPayment = buildPayment(BigDecimal.valueOf(1_000_000));
        when(paymentRepository.save(any(Payment.class))).thenReturn(savedPayment);

        PaymentResponseDto responseDto = new PaymentResponseDto();
        responseDto.setAmount(BigDecimal.valueOf(1_000_000));
        when(paymentMapper.toResponse(savedPayment)).thenReturn(responseDto);

        // Thanh toán đúng 1.000.000 → remaining = 0 → PAID
        PaymentRequestDto req = buildPaymentRequest(1L, 1L, BigDecimal.valueOf(1_000_000));
        PaymentResponseDto result = paymentService.createPayment(req);

        assertThat(result).isNotNull();
        assertThat(purchaseOrder.getPaymentStatus()).isEqualTo(PurchaseOrderPaymentStatus.PAID);
    }

    @Test
    @DisplayName("createPayment() - thanh toán một phần → paymentStatus = PARTIALLY_PAID")
    void createPayment_partialPayment_setsPartiallyPaidStatus() {
        setupSecurityContext();
        when(userRepository.findByUuid("user-uuid-001")).thenReturn(Optional.of(currentUser));
        when(purchaseOrderRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(purchaseOrder));
        when(paymentMethodRepository.findById(1L)).thenReturn(Optional.of(paymentMethod));
        when(paymentRepository.sumAmountByPurchaseOrderId(1L)).thenReturn(BigDecimal.ZERO);

        Payment savedPayment = buildPayment(BigDecimal.valueOf(400_000));
        when(paymentRepository.save(any(Payment.class))).thenReturn(savedPayment);

        PaymentResponseDto responseDto = new PaymentResponseDto();
        when(paymentMapper.toResponse(savedPayment)).thenReturn(responseDto);

        // Thanh toán 400.000 / 1.000.000 → PARTIALLY_PAID
        PaymentRequestDto req = buildPaymentRequest(1L, 1L, BigDecimal.valueOf(400_000));
        paymentService.createPayment(req);

        assertThat(purchaseOrder.getPaymentStatus()).isEqualTo(PurchaseOrderPaymentStatus.PARTIALLY_PAID);
    }

    @Test
    @DisplayName("createPayment() - amount bằng đúng remaining → PAID")
    void createPayment_exactRemainingAmount_setsPaid() {
        setupSecurityContext();
        when(userRepository.findByUuid("user-uuid-001")).thenReturn(Optional.of(currentUser));
        when(purchaseOrderRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(purchaseOrder));
        when(paymentMethodRepository.findById(1L)).thenReturn(Optional.of(paymentMethod));
        // Đã trả 400.000, còn 600.000
        when(paymentRepository.sumAmountByPurchaseOrderId(1L)).thenReturn(BigDecimal.valueOf(400_000));

        Payment savedPayment = buildPayment(BigDecimal.valueOf(600_000));
        when(paymentRepository.save(any(Payment.class))).thenReturn(savedPayment);

        PaymentResponseDto responseDto = new PaymentResponseDto();
        when(paymentMapper.toResponse(savedPayment)).thenReturn(responseDto);

        // Trả đúng phần còn lại 600.000 → PAID
        PaymentRequestDto req = buildPaymentRequest(1L, 1L, BigDecimal.valueOf(600_000));
        paymentService.createPayment(req);

        assertThat(purchaseOrder.getPaymentStatus()).isEqualTo(PurchaseOrderPaymentStatus.PAID);
    }

    // ─── getPaymentHistoryByPurchaseOrderId() ────────────────────────────────

    @Test
    @DisplayName("getPaymentHistory() - purchase order không tồn tại → throw PURCHASE_ORDER_NOT_FOUND")
    void getPaymentHistory_orderNotFound_throwsException() {
        when(purchaseOrderRepository.findById(99L)).thenReturn(Optional.empty());
        Pageable pageable = PageRequest.of(0, 10);

        assertThatThrownBy(() -> paymentService.getPaymentHistoryByPurchaseOrderId(99L, pageable))
                .isInstanceOf(InvalidException.class)
                .extracting(e -> ((InvalidException) e).getErrorCode())
                .isEqualTo(ErrorCode.PURCHASE_ORDER_NOT_FOUND);
    }

    @Test
    @DisplayName("getPaymentHistory() - thành công: trả về page DTO")
    void getPaymentHistory_success_returnsPage() {
        Pageable pageable = PageRequest.of(0, 10);
        when(purchaseOrderRepository.findById(1L)).thenReturn(Optional.of(purchaseOrder));
        when(paymentRepository.sumAmountByPurchaseOrderId(1L)).thenReturn(BigDecimal.valueOf(400_000));

        Payment payment = buildPayment(BigDecimal.valueOf(400_000));
        Page<Payment> paymentPage = new PageImpl<>(List.of(payment), pageable, 1);
        when(paymentRepository.findByPurchaseOrderIdOrderByPaymentDateDesc(1L, pageable))
                .thenReturn(paymentPage);

        PaymentResponseDto responseDto = new PaymentResponseDto();
        responseDto.setAmount(BigDecimal.valueOf(400_000));
        when(paymentMapper.toResponse(payment)).thenReturn(responseDto);

        PageResponseDto<PaymentResponseDto> result =
                paymentService.getPaymentHistoryByPurchaseOrderId(1L, pageable);

        assertThat(result).isNotNull();
        assertThat(result.items()).hasSize(1);
    }

    // ─── Helper methods ───────────────────────────────────────────────────────

    private void setupSecurityContext() {
        Authentication auth = mock(Authentication.class);
        when(auth.getName()).thenReturn("user-uuid-001");
        SecurityContext secCtx = mock(SecurityContext.class);
        when(secCtx.getAuthentication()).thenReturn(auth);
        SecurityContextHolder.setContext(secCtx);
    }

    private PaymentRequestDto buildPaymentRequest(Long orderId, Long methodId, BigDecimal amount) {
        PaymentRequestDto req = new PaymentRequestDto();
        req.setPurchaseOrderId(orderId);
        req.setPaymentMethodId(methodId);
        req.setAmount(amount);
        req.setPaymentDate(LocalDateTime.now());
        return req;
    }

    private Payment buildPayment(BigDecimal amount) {
        return Payment.builder()
                .id(1L)
                .purchaseOrder(purchaseOrder)
                .paymentMethod(paymentMethod)
                .amount(amount)
                .paymentDate(LocalDateTime.now())
                .createdBy(currentUser)
                .build();
    }
}
