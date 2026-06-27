package com.example.backend.controller;

import com.example.backend.dto.request.ProductCreateRequestDto;
import com.example.backend.dto.response.PageResponseDto;
import com.example.backend.dto.response.ProductResponseDto;
import com.example.backend.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    @GetMapping
    public ResponseEntity<PageResponseDto<ProductResponseDto>> getAllProducts(
            @RequestParam(name = "page", defaultValue = "1") int pageNumber,
            @RequestParam(name = "keyword", required = false) String keyword) {
        Pageable pageable = PageRequest.of(pageNumber - 1, 10);
        return ResponseEntity.ok(productService.getAllProducts(keyword, pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProductResponseDto> getProductById(@PathVariable Long id) {
        return ResponseEntity.ok(productService.getProductById(id));
    }

    @PostMapping
    public ResponseEntity<ProductResponseDto> createProduct(@Valid @RequestBody ProductCreateRequestDto request) {
        ProductResponseDto response = productService.createProduct(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }
}