package com.example.backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatProductDto {
    private Long id;
    private String name;
    private String code;
    private String category;
    private Long totalStock;
    private String url;       // URL ảnh sản phẩm (theo yêu cầu user)
    private String imageUrl;  // URL ảnh sản phẩm
}
