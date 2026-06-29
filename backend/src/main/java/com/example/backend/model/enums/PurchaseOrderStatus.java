package com.example.backend.model.enums;

import lombok.Getter;

@Getter
public enum PurchaseOrderStatus {
    DRAFT("DRAFT"),          // Nháp — vừa tạo, chưa duyệt
    PENDING("PENDING"),      // Chờ nhập hàng — đã duyệt
    RECEIVED("RECEIVED"),    // Đã nhận hàng — tồn kho đã cập nhật
    CANCELLED("CANCELLED");  // Đã huỷ

    private final String value;

    PurchaseOrderStatus(String value) {
        this.value = value;
    }
}