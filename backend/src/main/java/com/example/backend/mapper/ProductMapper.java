package com.example.backend.mapper;

import com.example.backend.dto.request.ProductCreateRequestDto;
import com.example.backend.dto.response.ProductResponseDto;
import com.example.backend.model.Product;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = {ProductVariantMapper.class})
public interface ProductMapper {

    @Mapping(target = "variants", source = "product.variants")
    ProductResponseDto toResponse(Product product);

    Product toEntity(ProductCreateRequestDto request);
}