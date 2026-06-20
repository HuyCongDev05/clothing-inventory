package com.example.backend.service;

import com.example.backend.dto.request.SupplierRequestDto;
import com.example.backend.dto.response.PageResponseDto;
import com.example.backend.dto.response.SupplierResponseDto;
import com.example.backend.exception.ErrorCode;
import com.example.backend.exception.InvalidException;
import com.example.backend.mapper.SupplierMapper;
import com.example.backend.model.Supplier;
import com.example.backend.repository.SupplierRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SupplierService {

    private final SupplierRepository supplierRepository;
    private final SupplierMapper supplierMapper;

    public PageResponseDto<SupplierResponseDto> getAllSuppliers(Pageable pageable) {
        Page<Supplier> supplierPage = supplierRepository.findAll(pageable);
        return PageResponseDto.from(supplierPage.map(supplierMapper::toResponse));
    }

    public SupplierResponseDto getSupplierByCode(String code) {
        Supplier supplier = supplierRepository.findByCode(code)
                .orElseThrow(() -> new InvalidException(ErrorCode.SUPPLIER_NOT_FOUND));
        return supplierMapper.toResponse(supplier);
    }

    @Transactional
    public SupplierResponseDto createSupplier(SupplierRequestDto request) {
        if (supplierRepository.existsByCode(request.getCode())) {
            throw new InvalidException(ErrorCode.CONFLICT_SUPPLIER_CODE);
        }
        Supplier supplier = supplierMapper.toEntity(request);
        supplier.setStatus("ACTIVE");
        Supplier savedSupplier = supplierRepository.save(supplier);
        return supplierMapper.toResponse(savedSupplier);
    }

    @Transactional
    public SupplierResponseDto updateSupplier(String code, SupplierRequestDto request) {
        Supplier existingSupplier = supplierRepository.findByCode(code)
                .orElseThrow(() -> new InvalidException(ErrorCode.SUPPLIER_NOT_FOUND));

        if (request.getCode() != null && !request.getCode().equals(code) && supplierRepository.existsByCode(request.getCode())) {
            throw new InvalidException(ErrorCode.CONFLICT_SUPPLIER_CODE);
        }

        supplierMapper.updateEntity(request, existingSupplier);
        Supplier updatedSupplier = supplierRepository.save(existingSupplier);
        return supplierMapper.toResponse(updatedSupplier);
    }

    @Transactional
    public void deleteSupplier(String code) {
        Supplier supplier = supplierRepository.findByCode(code)
                .orElseThrow(() -> new InvalidException(ErrorCode.SUPPLIER_NOT_FOUND));
        supplierRepository.delete(supplier);
    }
}