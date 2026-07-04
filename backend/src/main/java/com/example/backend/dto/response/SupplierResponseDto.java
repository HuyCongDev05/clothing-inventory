package com.example.backend.dto.response;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class SupplierResponseDto {
    private Long id;
    private String code;
    private String name;
    private String contactPerson;
    private String phone;
    private String email;
    private String address;
    private String taxCode;
    private String note;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

}

