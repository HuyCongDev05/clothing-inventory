package com.example.backend.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CacheService Unit Tests")
class CacheServiceTest {

    @Mock private StringRedisTemplate stringRedisTemplate;
    @Mock private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private CacheService cacheService;

    @BeforeEach
    void setUp() {
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    // ─── saveRefreshToken() ────────────────────────────────────────────────────

    @Test
    @DisplayName("saveRefreshToken() - gọi redis set với key đúng, TTL 7 ngày")
    void saveRefreshToken_callsRedisSet() {
        cacheService.saveRefreshToken("uuid-001", "refresh-tok");

        verify(valueOperations).set("refresh_token:uuid-001", "refresh-tok", 7, TimeUnit.DAYS);
    }

    // ─── deleteRefreshToken() ─────────────────────────────────────────────────

    @Test
    @DisplayName("deleteRefreshToken() - token không tồn tại trong redis → trả về false")
    void deleteRefreshToken_tokenNull_returnsFalse() {
        when(valueOperations.get("refresh_token:uuid-001")).thenReturn(null);

        boolean result = cacheService.deleteRefreshToken("uuid-001", "refresh-tok");

        assertThat(result).isFalse();
        verify(stringRedisTemplate, never()).delete(anyString());
    }

    @Test
    @DisplayName("deleteRefreshToken() - token không khớp → trả về false")
    void deleteRefreshToken_tokenMismatch_returnsFalse() {
        when(valueOperations.get("refresh_token:uuid-001")).thenReturn("other-token");

        boolean result = cacheService.deleteRefreshToken("uuid-001", "refresh-tok");

        assertThat(result).isFalse();
        verify(stringRedisTemplate, never()).delete(anyString());
    }

    @Test
    @DisplayName("deleteRefreshToken() - token khớp → xóa key, trả về true")
    void deleteRefreshToken_tokenMatch_deletesAndReturnsTrue() {
        when(valueOperations.get("refresh_token:uuid-001")).thenReturn("refresh-tok");

        boolean result = cacheService.deleteRefreshToken("uuid-001", "refresh-tok");

        assertThat(result).isTrue();
        verify(stringRedisTemplate).delete("refresh_token:uuid-001");
    }

    // ─── generatePurchaseOrderCode() ─────────────────────────────────────────

    @Test
    @DisplayName("generatePurchaseOrderCode() - sequence=1 (key mới) → set TTL 2 ngày")
    void generatePurchaseOrderCode_firstSeq_setsTTL() {
        when(valueOperations.increment(anyString())).thenReturn(1L);

        String code = cacheService.generatePurchaseOrderCode();

        assertThat(code).matches("PO-\\d{6}-0001");
        verify(stringRedisTemplate).expire(anyString(), eq(2L), eq(TimeUnit.DAYS));
    }

    @Test
    @DisplayName("generatePurchaseOrderCode() - sequence>1 (key cũ) → không set TTL lại")
    void generatePurchaseOrderCode_subsequentSeq_doesNotSetTTL() {
        when(valueOperations.increment(anyString())).thenReturn(5L);

        String code = cacheService.generatePurchaseOrderCode();

        assertThat(code).matches("PO-\\d{6}-0005");
        verify(stringRedisTemplate, never()).expire(anyString(), anyLong(), any(TimeUnit.class));
    }

    @Test
    @DisplayName("generatePurchaseOrderCode() - format đúng PO-YYMMDD-XXXX")
    void generatePurchaseOrderCode_formatCorrect() {
        when(valueOperations.increment(anyString())).thenReturn(42L);

        String code = cacheService.generatePurchaseOrderCode();

        assertThat(code).startsWith("PO-");
        assertThat(code).matches("PO-\\d{6}-0042");
    }
}
