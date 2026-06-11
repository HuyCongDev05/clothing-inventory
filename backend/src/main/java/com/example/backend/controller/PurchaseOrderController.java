package com.example.backend.controller;

import com.example.backend.dto.request.PurchaseOrderRequestDto;
import com.example.backend.dto.response.PageResponseDto;
import com.example.backend.model.PurchaseOrder;
import com.example.backend.service.PurchaseOrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/purchase-orders")
@RequiredArgsConstructor
public class PurchaseOrderController {
    private final PurchaseOrderService purchaseOrderService;

    @GetMapping
    public ResponseEntity<PageResponseDto<PurchaseOrder>> findAll(@RequestParam(defaultValue = "1") int pageNumber) {
        Pageable pageable = PageRequest.of(pageNumber - 1, 24);
        Page<PurchaseOrder> page = purchaseOrderService.findAll(pageable);
        return ResponseEntity.ok(PageResponseDto.from(page));
    }

    @PostMapping
    public ResponseEntity<PurchaseOrder> create(@Valid @RequestBody PurchaseOrderRequestDto purchaseOrderRequestDto) {
        PurchaseOrder createdPurchaseOrder = purchaseOrderService.create(purchaseOrderRequestDto);
        return new ResponseEntity<>(createdPurchaseOrder, HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    public ResponseEntity<PurchaseOrder> findById(@PathVariable Long id) {
        return ResponseEntity.ok(purchaseOrderService.findById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<PurchaseOrder> update(@PathVariable Long id, @Valid @RequestBody PurchaseOrderRequestDto purchaseOrderRequestDto) {
        return ResponseEntity.ok(purchaseOrderService.update(id, purchaseOrderRequestDto));
    }

    @PostMapping("/{id}/confirm")
    public ResponseEntity<PurchaseOrder> confirm(@PathVariable Long id) {
        return ResponseEntity.ok(purchaseOrderService.confirm(id));
    }
}