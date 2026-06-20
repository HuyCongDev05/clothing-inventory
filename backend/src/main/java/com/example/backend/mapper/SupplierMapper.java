package com.example.backend.mapper;

import com.example.backend.dto.request.SupplierRequestDto;
import com.example.backend.dto.response.SupplierResponseDto;
import com.example.backend.model.Supplier;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring")
public interface SupplierMapper {
    Supplier toEntity(SupplierRequestDto request);

    SupplierResponseDto toResponse(Supplier supplier);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateEntity(SupplierRequestDto request, @MappingTarget Supplier supplier);
}