package com.example.backend.controller;

import com.example.backend.dto.response.DashboardAnalyticsResponseDto;
import com.example.backend.dto.response.DashboardResponseDto;
import com.example.backend.dto.response.FormatMessageResponseDto;
import com.example.backend.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

@RestController
@RequestMapping("/api/v1/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @PreAuthorize("hasAuthority('admin')")
    @GetMapping
    public ResponseEntity<DashboardResponseDto> getDashboardData() {
        return ResponseEntity.ok(dashboardService.getParameterDashboard());
    }

    // timeframe: "3months" | "6months" | "year"
    @PreAuthorize("hasAnyAuthority('admin', 'coordinator')")
    @GetMapping("/analytics")
    public ResponseEntity<FormatMessageResponseDto<DashboardAnalyticsResponseDto>> getAnalytics(
            @RequestParam(defaultValue = "6months") String timeframe) {

        FormatMessageResponseDto<DashboardAnalyticsResponseDto> response = new FormatMessageResponseDto<>();
        response.setSuccess(true);
        response.setStatusCode(200);
        response.setMessage("Lấy dữ liệu thống kê kho thành công");
        response.setData(dashboardService.getAnalytics(timeframe));
        response.setTimestamp(Instant.now().toString());

        return ResponseEntity.ok(response);
    }
}
