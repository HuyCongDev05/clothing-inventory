package com.example.backend.controller;

import com.example.backend.dto.request.SupplierRequestDto;
import com.example.backend.dto.response.PageResponseDto;
import com.example.backend.dto.response.SupplierResponseDto;
import com.example.backend.model.enums.Status;
import com.example.backend.service.SupplierService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Set;

@RestController
@RequestMapping("/api/v1/suppliers")
@RequiredArgsConstructor
@Validated
public class SupplierController {

    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of("name", "email", "phone", "createdAt", "status");

    private final SupplierService supplierService;

    @GetMapping
    public ResponseEntity<PageResponseDto<SupplierResponseDto>> getAllSuppliers(
            @RequestParam(defaultValue = "1") @Min(value = 1, message = "Page number must be greater than or equal to 1") int page,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Status status,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDirection) {

        Pageable pageable = PageRequest.of(page - 1, 10, buildSort(sortBy, sortDirection));
        return ResponseEntity.ok(supplierService.getAllSuppliers(keyword, status, pageable));
    }

    @PreAuthorize("hasAuthority('store-keeper')")
    @PostMapping
    public ResponseEntity<SupplierResponseDto> createSupplier(@Valid @RequestBody SupplierRequestDto request) {
        return ResponseEntity.ok(supplierService.createSupplier(request));
    }

    @PreAuthorize("hasAuthority('store-keeper')")
    @PutMapping("/{code}")
    public ResponseEntity<SupplierResponseDto> updateSupplier(@PathVariable String code, @Valid @RequestBody SupplierRequestDto request) {
        return ResponseEntity.ok(supplierService.updateSupplier(code, request));
    }

    @PreAuthorize("hasAuthority('store-keeper')")
    @PatchMapping("/{code}")
    public ResponseEntity<SupplierResponseDto> patchSupplier(@PathVariable String code, @RequestBody SupplierRequestDto request) {
        return ResponseEntity.ok(supplierService.updateSupplier(code, request));
    }

    @PreAuthorize("hasAuthority('store-keeper')")
    @DeleteMapping("/{code}")
    public ResponseEntity<Void> deleteSupplier(@PathVariable String code) {
        supplierService.deleteSupplier(code);
        return ResponseEntity.noContent().build();
    }

    private Sort buildSort(String sortBy, String sortDir) {
        String safeSortBy = ALLOWED_SORT_FIELDS.contains(sortBy) ? sortBy : "createdAt";
        Sort.Direction direction = "asc".equalsIgnoreCase(sortDir) ? Sort.Direction.ASC : Sort.Direction.DESC;
        return Sort.by(direction, safeSortBy);
    }
}