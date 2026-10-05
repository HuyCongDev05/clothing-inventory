package com.example.backend.service;

import com.example.backend.dto.request.PaymentMethodRequestDto;
import com.example.backend.dto.response.PaymentMethodResponseDto;
import com.example.backend.exception.ErrorCode;
import com.example.backend.exception.InvalidException;
import com.example.backend.mapper.PaymentMethodMapper;
import com.example.backend.model.PaymentMethod;
import com.example.backend.model.enums.Status;
import com.example.backend.repository.PaymentMethodRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PaymentMethodService Unit Tests")
class PaymentMethodServiceTest {

    @Mock private PaymentMethodRepository paymentMethodRepository;
    @Mock private PaymentMethodMapper paymentMethodMapper;

    @InjectMocks
    private PaymentMethodService paymentMethodService;

    // ─── getAllPaymentMethods() ────────────────────────────────────────────────

    @Test
    @DisplayName("getAllPaymentMethods() - trả về danh sách map qua mapper")
    void getAllPaymentMethods_returnsMappedList() {
        PaymentMethod pm1 = new PaymentMethod();
        pm1.setCode("CASH");
        PaymentMethod pm2 = new PaymentMethod();
        pm2.setCode("BANK");

        PaymentMethodResponseDto dto1 = new PaymentMethodResponseDto();
        PaymentMethodResponseDto dto2 = new PaymentMethodResponseDto();

        when(paymentMethodRepository.findAll()).thenReturn(List.of(pm1, pm2));
        when(paymentMethodMapper.toResponse(pm1)).thenReturn(dto1);
        when(paymentMethodMapper.toResponse(pm2)).thenReturn(dto2);

        List<PaymentMethodResponseDto> result = paymentMethodService.getAllPaymentMethods();

        assertThat(result).hasSize(2);
        verify(paymentMethodMapper, times(2)).toResponse(any(PaymentMethod.class));
    }

    @Test
    @DisplayName("getAllPaymentMethods() - danh sách rỗng → trả về []")
    void getAllPaymentMethods_empty_returnsEmptyList() {
        when(paymentMethodRepository.findAll()).thenReturn(List.of());
        assertThat(paymentMethodService.getAllPaymentMethods()).isEmpty();
    }

    // ─── createPaymentMethod() ────────────────────────────────────────────────

    @Test
    @DisplayName("createPaymentMethod() - code đã tồn tại → throw CONFLICT_PAYMENT_METHOD_CODE")
    void createPaymentMethod_codeConflict_throwsException() {
        PaymentMethodRequestDto req = new PaymentMethodRequestDto();
        req.setCode("CASH");
        req.setName("Tiền mặt");

        when(paymentMethodRepository.existsByCode("CASH")).thenReturn(true);

        assertThatThrownBy(() -> paymentMethodService.createPaymentMethod(req))
                .isInstanceOf(InvalidException.class)
                .extracting(e -> ((InvalidException) e).getErrorCode())
                .isEqualTo(ErrorCode.CONFLICT_PAYMENT_METHOD_CODE);
    }

    @Test
    @DisplayName("createPaymentMethod() - thành công: set ACTIVE, lưu và trả về DTO")
    void createPaymentMethod_success_setsActiveAndReturns() {
        PaymentMethodRequestDto req = new PaymentMethodRequestDto();
        req.setCode("BANK");
        req.setName("Chuyển khoản");

        PaymentMethod entity = new PaymentMethod();
        entity.setCode("BANK");

        PaymentMethodResponseDto dto = new PaymentMethodResponseDto();

        when(paymentMethodRepository.existsByCode("BANK")).thenReturn(false);
        when(paymentMethodMapper.toEntity(req)).thenReturn(entity);
        when(paymentMethodRepository.save(entity)).thenReturn(entity);
        when(paymentMethodMapper.toResponse(entity)).thenReturn(dto);

        PaymentMethodResponseDto result = paymentMethodService.createPaymentMethod(req);

        assertThat(result).isNotNull();
        assertThat(entity.getStatus()).isEqualTo(Status.ACTIVE);
        verify(paymentMethodRepository).save(entity);
    }
}
