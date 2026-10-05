package com.example.backend.controller;

import com.example.backend.dto.request.UpdateWarehouseCapacityRequestDto;
import com.example.backend.dto.response.FormatMessageResponseDto;
import com.example.backend.dto.response.WarehouseCapacityDto;
import com.example.backend.service.SystemSettingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;

@RestController
@RequestMapping("/api/v1/settings")
@RequiredArgsConstructor
public class SystemSettingController {

    private final SystemSettingService systemSettingService;

    // Xem thông số sức chứa và tỷ lệ lấp đầy kho hiện tại
    @PreAuthorize("hasAuthority('admin')")
    @GetMapping("/warehouse-capacity")
    public ResponseEntity<FormatMessageResponseDto<WarehouseCapacityDto>> getWarehouseCapacity() {
        FormatMessageResponseDto<WarehouseCapacityDto> response = new FormatMessageResponseDto<>();
        response.setSuccess(true);
        response.setStatusCode(200);
        response.setMessage("Lấy thông tin sức chứa kho thành công");
        response.setData(systemSettingService.getWarehouseCapacityInfo());
        response.setTimestamp(Instant.now().toString());

        return ResponseEntity.ok(response);
    }

    // Cập nhật mức sức chứa tối đa của kho hàng
    @PreAuthorize("hasAuthority('admin')")
    @PutMapping("/warehouse-capacity")
    public ResponseEntity<FormatMessageResponseDto<WarehouseCapacityDto>> updateWarehouseCapacity(
            @Valid @RequestBody UpdateWarehouseCapacityRequestDto request,
            Authentication authentication) {

        String username = authentication != null ? authentication.getName() : "admin";
        WarehouseCapacityDto updated = systemSettingService.updateWarehouseCapacity(request, username);

        FormatMessageResponseDto<WarehouseCapacityDto> response = new FormatMessageResponseDto<>();
        response.setSuccess(true);
        response.setStatusCode(200);
        response.setMessage("Cập nhật sức chứa kho hàng thành công");
        response.setData(updated);
        response.setTimestamp(Instant.now().toString());

        return ResponseEntity.ok(response);
    }
}
