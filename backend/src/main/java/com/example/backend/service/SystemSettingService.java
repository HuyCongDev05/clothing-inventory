package com.example.backend.service;

import com.example.backend.dto.request.UpdateWarehouseCapacityRequestDto;
import com.example.backend.dto.response.WarehouseCapacityDto;
import com.example.backend.exception.ErrorCode;
import com.example.backend.exception.InvalidException;
import com.example.backend.model.SystemSetting;
import com.example.backend.repository.ProductVariantRepository;
import com.example.backend.repository.SystemSettingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class SystemSettingService {

    public static final String KEY_WAREHOUSE_MAX_CAPACITY = "WAREHOUSE_MAX_CAPACITY";
    public static final String KEY_WAREHOUSE_WARNING_THRESHOLD = "WAREHOUSE_WARNING_THRESHOLD";

    public static final long DEFAULT_MAX_CAPACITY = 20000L;
    public static final int DEFAULT_WARNING_THRESHOLD = 85;

    private final SystemSettingRepository systemSettingRepository;
    private final ProductVariantRepository productVariantRepository;

    // Lấy thông tin sức chứa kho và tình trạng lấp đầy thời gian thực
    public WarehouseCapacityDto getWarehouseCapacityInfo() {
        long currentInventory = getCurrentTotalInventory();
        long maxCapacity = getWarehouseMaxCapacity();
        int warningThreshold = getWarehouseWarningThreshold();

        double occupancyRate = 0.0;
        if (maxCapacity > 0) {
            occupancyRate = Math.round(((double) currentInventory / maxCapacity * 100) * 100.0) / 100.0;
        }

        long remainingCapacity = Math.max(0, maxCapacity - currentInventory);
        String status;
        String statusMessage;

        if (currentInventory >= maxCapacity) {
            status = "FULL";
            statusMessage = "Kho đã đầy tải 100%, không thể nhập thêm hàng hóa";
        } else if (occupancyRate >= warningThreshold) {
            status = "WARNING";
            statusMessage = String.format("Cảnh báo: Kho sắp đầy (đã đạt %.1f%% / ngưỡng %d%%)", occupancyRate, warningThreshold);
        } else if (occupancyRate >= 75) {
            status = "MODERATE";
            statusMessage = String.format("Sức chứa ở mức khá cao (%.1f%%)", occupancyRate);
        } else {
            status = "SAFE";
            statusMessage = String.format("Sức chứa kho an toàn (%.1f%%)", occupancyRate);
        }

        return WarehouseCapacityDto.builder()
                .maxCapacity(maxCapacity)
                .currentInventory(currentInventory)
                .remainingCapacity(remainingCapacity)
                .occupancyRate(occupancyRate)
                .warningThreshold(warningThreshold)
                .status(status)
                .statusMessage(statusMessage)
                .build();
    }

    // Cập nhật mức sức chứa tối đa của kho hàng (dành cho Admin)
    @Transactional
    public WarehouseCapacityDto updateWarehouseCapacity(UpdateWarehouseCapacityRequestDto request, String username) {
        saveOrUpdateSetting(KEY_WAREHOUSE_MAX_CAPACITY,
                String.valueOf(request.getMaxCapacity()),
                "Sức chứa lưu trữ tối đa của kho hàng (sản phẩm)",
                username);

        if (request.getWarningThreshold() != null) {
            saveOrUpdateSetting(KEY_WAREHOUSE_WARNING_THRESHOLD,
                    String.valueOf(request.getWarningThreshold()),
                    "Ngưỡng cảnh báo lấp đầy kho (%)",
                    username);
        }

        return getWarehouseCapacityInfo();
    }

    // Kiểm tra tính khả dụng trước khi nhập hàng vào kho
    public void validateCapacityForInbound(long incomingQuantity) {
        long currentInventory = getCurrentTotalInventory();
        long maxCapacity = getWarehouseMaxCapacity();

        if (currentInventory + incomingQuantity > maxCapacity) {
            log.warn("Chặn nhập kho do vượt sức chứa: incoming={}, current={}, max={}", incomingQuantity, currentInventory, maxCapacity);
            throw new InvalidException(ErrorCode.WAREHOUSE_CAPACITY_EXCEEDED);
        }
    }

    public long getWarehouseMaxCapacity() {
        return getSettingLong(KEY_WAREHOUSE_MAX_CAPACITY, DEFAULT_MAX_CAPACITY);
    }

    public int getWarehouseWarningThreshold() {
        return (int) getSettingLong(KEY_WAREHOUSE_WARNING_THRESHOLD, DEFAULT_WARNING_THRESHOLD);
    }

    public long getCurrentTotalInventory() {
        Long sum = productVariantRepository.sumAllQuantityOnHand();
        return sum != null ? sum : 0L;
    }

    private long getSettingLong(String key, long defaultValue) {
        return systemSettingRepository.findBySettingKey(key)
                .map(s -> {
                    try {
                        return Long.parseLong(s.getSettingValue().trim());
                    } catch (Exception e) {
                        return defaultValue;
                    }
                })
                .orElse(defaultValue);
    }

    private void saveOrUpdateSetting(String key, String value, String description, String username) {
        SystemSetting setting = systemSettingRepository.findBySettingKey(key)
                .orElseGet(() -> SystemSetting.builder()
                        .settingKey(key)
                        .description(description)
                        .build());

        setting.setSettingValue(value);
        setting.setDescription(description);
        setting.setUpdatedBy(username);
        systemSettingRepository.save(setting);
    }
}
