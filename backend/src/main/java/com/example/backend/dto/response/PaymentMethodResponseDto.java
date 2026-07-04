package com.example.backend.dto.response;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PaymentMethodResponseDto {
    private Long id;
    private String code;
    private String name;
    private String status;
}
