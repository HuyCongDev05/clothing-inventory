package com.example.backend.service;

import com.example.backend.dto.request.UpdateWarehouseCapacityRequestDto;
import com.example.backend.dto.response.WarehouseCapacityDto;
import com.example.backend.exception.ErrorCode;
import com.example.backend.exception.InvalidException;
import com.example.backend.model.SystemSetting;
import com.example.backend.repository.ProductVariantRepository;
import com.example.backend.repository.SystemSettingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("SystemSettingService Unit Tests")
class SystemSettingServiceTest {

    @Mock private SystemSettingRepository systemSettingRepository;
    @Mock private ProductVariantRepository productVariantRepository;

    @InjectMocks
    private SystemSettingService systemSettingService;

    // ─── getCurrentTotalInventory() ───────────────────────────────────────────

    @Test
    @DisplayName("getCurrentTotalInventory() - sumAll returns null → trả về 0")
    void getCurrentTotalInventory_nullSum_returnsZero() {
        when(productVariantRepository.sumAllQuantityOnHand()).thenReturn(null);
        assertThat(systemSettingService.getCurrentTotalInventory()).isEqualTo(0L);
    }

    @Test
    @DisplayName("getCurrentTotalInventory() - sumAll returns value → trả về đúng")
    void getCurrentTotalInventory_withValue_returnsValue() {
        when(productVariantRepository.sumAllQuantityOnHand()).thenReturn(1500L);
        assertThat(systemSettingService.getCurrentTotalInventory()).isEqualTo(1500L);
    }

    // ─── getWarehouseMaxCapacity() ────────────────────────────────────────────

    @Test
    @DisplayName("getWarehouseMaxCapacity() - không có setting → trả về default 20000")
    void getWarehouseMaxCapacity_noSetting_returnsDefault() {
        when(systemSettingRepository.findBySettingKey(SystemSettingService.KEY_WAREHOUSE_MAX_CAPACITY))
                .thenReturn(Optional.empty());
        assertThat(systemSettingService.getWarehouseMaxCapacity())
                .isEqualTo(SystemSettingService.DEFAULT_MAX_CAPACITY);
    }

    @Test
    @DisplayName("getWarehouseMaxCapacity() - có setting → trả về giá trị từ DB")
    void getWarehouseMaxCapacity_withSetting_returnsDbValue() {
        SystemSetting setting = new SystemSetting();
        setting.setSettingKey(SystemSettingService.KEY_WAREHOUSE_MAX_CAPACITY);
        setting.setSettingValue("30000");
        when(systemSettingRepository.findBySettingKey(SystemSettingService.KEY_WAREHOUSE_MAX_CAPACITY))
                .thenReturn(Optional.of(setting));
        assertThat(systemSettingService.getWarehouseMaxCapacity()).isEqualTo(30000L);
    }

    @Test
    @DisplayName("getWarehouseMaxCapacity() - giá trị không parse được → trả về default")
    void getWarehouseMaxCapacity_invalidValue_returnsDefault() {
        SystemSetting setting = new SystemSetting();
        setting.setSettingValue("not-a-number");
        when(systemSettingRepository.findBySettingKey(SystemSettingService.KEY_WAREHOUSE_MAX_CAPACITY))
                .thenReturn(Optional.of(setting));
        assertThat(systemSettingService.getWarehouseMaxCapacity())
                .isEqualTo(SystemSettingService.DEFAULT_MAX_CAPACITY);
    }

    // ─── getWarehouseWarningThreshold() ──────────────────────────────────────

    @Test
    @DisplayName("getWarehouseWarningThreshold() - không có setting → trả về default 85")
    void getWarehouseWarningThreshold_noSetting_returnsDefault() {
        when(systemSettingRepository.findBySettingKey(SystemSettingService.KEY_WAREHOUSE_WARNING_THRESHOLD))
                .thenReturn(Optional.empty());
        assertThat(systemSettingService.getWarehouseWarningThreshold())
                .isEqualTo(SystemSettingService.DEFAULT_WARNING_THRESHOLD);
    }

    // ─── validateCapacityForInbound() ────────────────────────────────────────

    @Test
    @DisplayName("validateCapacityForInbound() - vượt sức chứa → throw WAREHOUSE_CAPACITY_EXCEEDED")
    void validateCapacityForInbound_exceeds_throwsException() {
        when(productVariantRepository.sumAllQuantityOnHand()).thenReturn(19900L);
        when(systemSettingRepository.findBySettingKey(SystemSettingService.KEY_WAREHOUSE_MAX_CAPACITY))
                .thenReturn(Optional.empty()); // default 20000

        // 19900 + 200 = 20100 > 20000
        assertThatThrownBy(() -> systemSettingService.validateCapacityForInbound(200L))
                .isInstanceOf(InvalidException.class)
                .extracting(e -> ((InvalidException) e).getErrorCode())
                .isEqualTo(ErrorCode.WAREHOUSE_CAPACITY_EXCEEDED);
    }

    @Test
    @DisplayName("validateCapacityForInbound() - trong giới hạn → không throw")
    void validateCapacityForInbound_withinLimit_doesNotThrow() {
        when(productVariantRepository.sumAllQuantityOnHand()).thenReturn(1000L);
        when(systemSettingRepository.findBySettingKey(SystemSettingService.KEY_WAREHOUSE_MAX_CAPACITY))
                .thenReturn(Optional.empty()); // default 20000

        // 1000 + 100 = 1100 < 20000
        systemSettingService.validateCapacityForInbound(100L);
        // no exception → pass
    }

    @Test
    @DisplayName("validateCapacityForInbound() - đúng bằng max → không throw")
    void validateCapacityForInbound_exactMax_doesNotThrow() {
        SystemSetting setting = new SystemSetting();
        setting.setSettingValue("1000");
        when(productVariantRepository.sumAllQuantityOnHand()).thenReturn(900L);
        when(systemSettingRepository.findBySettingKey(SystemSettingService.KEY_WAREHOUSE_MAX_CAPACITY))
                .thenReturn(Optional.of(setting));

        // 900 + 100 = 1000 == max → không throw
        systemSettingService.validateCapacityForInbound(100L);
    }

    // ─── getWarehouseCapacityInfo() ───────────────────────────────────────────

    @Test
    @DisplayName("getWarehouseCapacityInfo() - kho đầy (>=max) → status FULL")
    void getWarehouseCapacityInfo_full_returnsFull() {
        when(productVariantRepository.sumAllQuantityOnHand()).thenReturn(20000L);
        when(systemSettingRepository.findBySettingKey(SystemSettingService.KEY_WAREHOUSE_MAX_CAPACITY))
                .thenReturn(Optional.empty()); // max = 20000
        when(systemSettingRepository.findBySettingKey(SystemSettingService.KEY_WAREHOUSE_WARNING_THRESHOLD))
                .thenReturn(Optional.empty()); // threshold = 85

        WarehouseCapacityDto result = systemSettingService.getWarehouseCapacityInfo();

        assertThat(result.getStatus()).isEqualTo("FULL");
        assertThat(result.getRemainingCapacity()).isEqualTo(0L);
    }

    @Test
    @DisplayName("getWarehouseCapacityInfo() - >= threshold (>=85%) → status WARNING")
    void getWarehouseCapacityInfo_warning_returnsWarning() {
        when(productVariantRepository.sumAllQuantityOnHand()).thenReturn(17100L); // 85.5%
        when(systemSettingRepository.findBySettingKey(SystemSettingService.KEY_WAREHOUSE_MAX_CAPACITY))
                .thenReturn(Optional.empty()); // max = 20000
        when(systemSettingRepository.findBySettingKey(SystemSettingService.KEY_WAREHOUSE_WARNING_THRESHOLD))
                .thenReturn(Optional.empty()); // threshold = 85

        WarehouseCapacityDto result = systemSettingService.getWarehouseCapacityInfo();

        assertThat(result.getStatus()).isEqualTo("WARNING");
    }

    @Test
    @DisplayName("getWarehouseCapacityInfo() - >= 75% và < threshold → status MODERATE")
    void getWarehouseCapacityInfo_moderate_returnsModerate() {
        when(productVariantRepository.sumAllQuantityOnHand()).thenReturn(15500L); // 77.5%
        when(systemSettingRepository.findBySettingKey(SystemSettingService.KEY_WAREHOUSE_MAX_CAPACITY))
                .thenReturn(Optional.empty()); // max = 20000
        when(systemSettingRepository.findBySettingKey(SystemSettingService.KEY_WAREHOUSE_WARNING_THRESHOLD))
                .thenReturn(Optional.empty()); // threshold = 85

        WarehouseCapacityDto result = systemSettingService.getWarehouseCapacityInfo();

        assertThat(result.getStatus()).isEqualTo("MODERATE");
    }

    @Test
    @DisplayName("getWarehouseCapacityInfo() - < 75% → status SAFE")
    void getWarehouseCapacityInfo_safe_returnsSafe() {
        when(productVariantRepository.sumAllQuantityOnHand()).thenReturn(5000L); // 25%
        when(systemSettingRepository.findBySettingKey(SystemSettingService.KEY_WAREHOUSE_MAX_CAPACITY))
                .thenReturn(Optional.empty()); // max = 20000
        when(systemSettingRepository.findBySettingKey(SystemSettingService.KEY_WAREHOUSE_WARNING_THRESHOLD))
                .thenReturn(Optional.empty()); // threshold = 85

        WarehouseCapacityDto result = systemSettingService.getWarehouseCapacityInfo();

        assertThat(result.getStatus()).isEqualTo("SAFE");
        assertThat(result.getCurrentInventory()).isEqualTo(5000L);
        assertThat(result.getMaxCapacity()).isEqualTo(20000L);
        assertThat(result.getRemainingCapacity()).isEqualTo(15000L);
    }

    @Test
    @DisplayName("getWarehouseCapacityInfo() - maxCapacity=0 → occupancyRate=0.0")
    void getWarehouseCapacityInfo_zeroMaxCapacity_occupancyZero() {
        SystemSetting setting = new SystemSetting();
        setting.setSettingValue("0");
        when(productVariantRepository.sumAllQuantityOnHand()).thenReturn(0L);
        when(systemSettingRepository.findBySettingKey(SystemSettingService.KEY_WAREHOUSE_MAX_CAPACITY))
                .thenReturn(Optional.of(setting));
        when(systemSettingRepository.findBySettingKey(SystemSettingService.KEY_WAREHOUSE_WARNING_THRESHOLD))
                .thenReturn(Optional.empty());

        WarehouseCapacityDto result = systemSettingService.getWarehouseCapacityInfo();
        assertThat(result.getOccupancyRate()).isEqualTo(0.0);
    }

    // ─── updateWarehouseCapacity() ────────────────────────────────────────────

    @Test
    @DisplayName("updateWarehouseCapacity() - cập nhật cả maxCapacity và warningThreshold")
    void updateWarehouseCapacity_bothFields_savesBoth() {
        when(systemSettingRepository.findBySettingKey(anyString())).thenReturn(Optional.empty());
        when(productVariantRepository.sumAllQuantityOnHand()).thenReturn(1000L);
        when(systemSettingRepository.save(any(SystemSetting.class))).thenAnswer(inv -> inv.getArgument(0));

        UpdateWarehouseCapacityRequestDto req = new UpdateWarehouseCapacityRequestDto();
        req.setMaxCapacity(25000L);
        req.setWarningThreshold(90);

        systemSettingService.updateWarehouseCapacity(req, "admin");

        // save được gọi 2 lần (maxCapacity + warningThreshold)
        verify(systemSettingRepository, times(2)).save(any(SystemSetting.class));
    }

    @Test
    @DisplayName("updateWarehouseCapacity() - warningThreshold null → chỉ save maxCapacity")
    void updateWarehouseCapacity_onlyMaxCapacity_savesOnce() {
        when(systemSettingRepository.findBySettingKey(anyString())).thenReturn(Optional.empty());
        when(productVariantRepository.sumAllQuantityOnHand()).thenReturn(1000L);
        when(systemSettingRepository.save(any(SystemSetting.class))).thenAnswer(inv -> inv.getArgument(0));

        UpdateWarehouseCapacityRequestDto req = new UpdateWarehouseCapacityRequestDto();
        req.setMaxCapacity(15000L);
        // warningThreshold = null

        systemSettingService.updateWarehouseCapacity(req, "admin");

        verify(systemSettingRepository, times(1)).save(any(SystemSetting.class));
    }

    @Test
    @DisplayName("updateWarehouseCapacity() - existing setting → update giá trị cũ")
    void updateWarehouseCapacity_existingSetting_updatesValue() {
        SystemSetting existingSetting = new SystemSetting();
        existingSetting.setSettingKey(SystemSettingService.KEY_WAREHOUSE_MAX_CAPACITY);
        existingSetting.setSettingValue("20000");

        when(systemSettingRepository.findBySettingKey(SystemSettingService.KEY_WAREHOUSE_MAX_CAPACITY))
                .thenReturn(Optional.of(existingSetting));
        when(systemSettingRepository.save(any(SystemSetting.class))).thenAnswer(inv -> inv.getArgument(0));
        when(productVariantRepository.sumAllQuantityOnHand()).thenReturn(500L);
        when(systemSettingRepository.findBySettingKey(SystemSettingService.KEY_WAREHOUSE_WARNING_THRESHOLD))
                .thenReturn(Optional.empty());

        UpdateWarehouseCapacityRequestDto req = new UpdateWarehouseCapacityRequestDto();
        req.setMaxCapacity(50000L);

        systemSettingService.updateWarehouseCapacity(req, "manager");

        assertThat(existingSetting.getSettingValue()).isEqualTo("50000");
        assertThat(existingSetting.getUpdatedBy()).isEqualTo("manager");
    }
}
