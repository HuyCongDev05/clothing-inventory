package com.example.backend.exception;

import lombok.Getter;

@Getter
public enum ErrorCode {
    TOKEN_SUBJECT_MISMATCH(401, "Invalid token signature"),
    UNAUTHORIZED_ACCESS(401, "Unauthorized access"),
    FORBIDDEN_ACCESS(403, "Forbidden access"),
    SUPPLIER_NOT_FOUND(404, "Supplier not found"),
    USER_NOT_FOUND(404, "User not found"),
    VARIANT_NOT_FOUND(404, "Variant not found"),
    PURCHASE_ORDER_NOT_FOUND(404, "Purchase order not found"),
    PURCHASE_ORDER_NOT_PENDING(400, "Only pending purchase orders can be modified");

    private final int status;
    private final String message;

    ErrorCode(int status, String message) {
        this.status = status;
        this.message = message;
    }
}