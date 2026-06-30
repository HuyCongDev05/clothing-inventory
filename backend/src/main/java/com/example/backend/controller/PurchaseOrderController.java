package com.example.backend.controller;

import com.example.backend.dto.request.PurchaseOrderRequestDto;
import com.example.backend.dto.request.PurchaseOrderStatusUpdateRequestDto;
import com.example.backend.dto.response.PageResponseDto;
import com.example.backend.dto.response.PurchaseOrderResponseDto;
import com.example.backend.service.PurchaseOrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Set;

@RestController
@RequestMapping("/api/v1/purchase-orders")
@RequiredArgsConstructor
public class PurchaseOrderController {

    private final PurchaseOrderService purchaseOrderService;

    private static final Set<String> ALLOWED_SORT_FIELDS =
            Set.of("totalAmount", "orderDate", "totalQuantity");

    private static final Set<String> ALLOWED_RECEIPT_SORT_FIELDS =
            Set.of("totalAmount", "receivedDate", "totalQuantity");

    @GetMapping
    public ResponseEntity<PageResponseDto<PurchaseOrderResponseDto>> getAllPurchaseOrders(
            @RequestParam(name = "page", defaultValue = "1") int pageNumber,
            @RequestParam(name = "keyword", required = false) String keyword,
            @RequestParam(name = "sortBy", defaultValue = "orderDate") String sortBy,
            @RequestParam(name = "sortDir", defaultValue = "desc") String sortDir) {
        String resolvedSortBy = ALLOWED_SORT_FIELDS.contains(sortBy) ? sortBy : "orderDate";
        Sort sort = "asc".equalsIgnoreCase(sortDir)
                ? Sort.by(resolvedSortBy).ascending()
                : Sort.by(resolvedSortBy).descending();
        Pageable pageable = PageRequest.of(pageNumber - 1, 10, sort);
        return ResponseEntity.ok(purchaseOrderService.getAllPurchaseOrders(keyword, pageable));
    }

    /** Lấy danh sách phiếu nhập kho — chỉ các đơn có status = RECEIVED */
    @GetMapping("/received")
    public ResponseEntity<PageResponseDto<PurchaseOrderResponseDto>> getReceivedPurchaseOrders(
            @RequestParam(name = "page", defaultValue = "1") int pageNumber,
            @RequestParam(name = "keyword", required = false) String keyword,
            @RequestParam(name = "sortBy", defaultValue = "receivedDate") String sortBy,
            @RequestParam(name = "sortDir", defaultValue = "desc") String sortDir) {
        String resolvedSortBy = ALLOWED_RECEIPT_SORT_FIELDS.contains(sortBy) ? sortBy : "receivedDate";
        Sort sort = "asc".equalsIgnoreCase(sortDir)
                ? Sort.by(resolvedSortBy).ascending()
                : Sort.by(resolvedSortBy).descending();
        Pageable pageable = PageRequest.of(pageNumber - 1, 10, sort);
        return ResponseEntity.ok(purchaseOrderService.getReceivedPurchaseOrders(keyword, pageable));
    }


    @GetMapping("/{id}")
    public ResponseEntity<PurchaseOrderResponseDto> getPurchaseOrderById(@PathVariable Long id) {
        return ResponseEntity.ok(purchaseOrderService.getPurchaseOrderById(id));
    }

    @PostMapping
    public ResponseEntity<PurchaseOrderResponseDto> createPurchaseOrder(
            @Valid @RequestBody PurchaseOrderRequestDto request) {
        return ResponseEntity.ok(purchaseOrderService.createPurchaseOrder(request));
    }

    /** Sửa đơn đặt hàng — chỉ cho phép khi đơn ở trạng thái DRAFT */
    @PutMapping("/{id}")
    public ResponseEntity<PurchaseOrderResponseDto> updatePurchaseOrder(
            @PathVariable Long id,
            @Valid @RequestBody PurchaseOrderRequestDto request) {
        return ResponseEntity.ok(purchaseOrderService.updatePurchaseOrder(id, request));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<PurchaseOrderResponseDto> updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody PurchaseOrderStatusUpdateRequestDto request) {
        return ResponseEntity.ok(purchaseOrderService.updateStatus(id, request));
    }
}