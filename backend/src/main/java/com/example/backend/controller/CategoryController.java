package com.example.backend.controller;

import com.example.backend.dto.request.CategoryRequestDto;
import com.example.backend.dto.request.CategoryRestoreRequestDto;
import com.example.backend.dto.response.CategoryResponseDto;
import com.example.backend.service.CategoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    @PreAuthorize("hasAuthority('warehouse-staff')")
    @GetMapping
    public ResponseEntity<List<CategoryResponseDto>> getAllCategories() {
        return ResponseEntity.ok(categoryService.getAllCategories());
    }

    @PreAuthorize("hasAuthority('warehouse-staff')")
    @PostMapping
    public ResponseEntity<CategoryResponseDto> createCategory(@Valid @RequestBody CategoryRequestDto request) {
        return ResponseEntity.ok(categoryService.createCategory(request));
    }

    @PreAuthorize("hasAuthority('warehouse-staff')")
    @PutMapping("/{id}")
    public ResponseEntity<CategoryResponseDto> updateCategory(@PathVariable Long id,
                                                              @Valid @RequestBody CategoryRequestDto request) {
        return ResponseEntity.ok(categoryService.updateCategory(id, request));
    }

    @PreAuthorize("hasAuthority('warehouse-staff')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCategory(@PathVariable Long id) {
        categoryService.deleteCategory(id);
        return ResponseEntity.noContent().build();
    }

    @PreAuthorize("hasAuthority('warehouse-staff')")
    @PutMapping("/restore")
    public ResponseEntity<CategoryResponseDto> restoreCategory(@Valid @RequestBody CategoryRestoreRequestDto request) {
        return ResponseEntity.ok(categoryService.restoreCategory(request.getName()));
    }
}
