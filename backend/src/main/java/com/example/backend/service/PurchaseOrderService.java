package com.example.backend.service;

import com.example.backend.dto.request.PurchaseOrderRequestDto;
import com.example.backend.exception.ErrorCode;
import com.example.backend.exception.InvalidException;
import com.example.backend.model.*;
import com.example.backend.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PurchaseOrderService {
    private final PurchaseOrderRepository purchaseOrderRepository;
    private final PurchaseOrderDetailRepository purchaseOrderDetailRepository;
    private final SupplierRepository supplierRepository;
    private final ProductVariantRepository productVariantRepository;
    private final UserRepository userRepository;
    private final InventoryRepository inventoryRepository;

    public Page<PurchaseOrder> findAll(Pageable pageable) {
        return purchaseOrderRepository.findAll(pageable);
    }

    @Transactional
    public PurchaseOrder create(PurchaseOrderRequestDto requestDto) {
        Supplier supplier = supplierRepository.findById(requestDto.getSupplierId())
                .orElseThrow(() -> new InvalidException(ErrorCode.SUPPLIER_NOT_FOUND));

        User user = userRepository.findById(1L)
                .orElseThrow(() -> new InvalidException(ErrorCode.USER_NOT_FOUND));

        PurchaseOrder purchaseOrder = new PurchaseOrder();
        purchaseOrder.setPoCode("PO-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        purchaseOrder.setSupplier(supplier);
        purchaseOrder.setCreatedBy(user);
        purchaseOrder.setOrderDate(LocalDateTime.now());
        purchaseOrder.setStatus("PENDING");
        purchaseOrder.setNote(requestDto.getNote());

        PurchaseOrder savedPurchaseOrder = purchaseOrderRepository.save(purchaseOrder);

        List<PurchaseOrderDetail> details = requestDto.getDetails().stream().map(detailDto -> {
            ProductVariant variant = productVariantRepository.findById(detailDto.getVariantId())
                    .orElseThrow(() -> new InvalidException(ErrorCode.VARIANT_NOT_FOUND));

            PurchaseOrderDetail detail = new PurchaseOrderDetail();
            detail.setPurchaseOrder(savedPurchaseOrder);
            detail.setVariant(variant);
            detail.setQuantity(detailDto.getQuantity());
            detail.setExpectedPrice(detailDto.getExpectedPrice());
            return detail;
        }).toList();

        purchaseOrderDetailRepository.saveAll(details);

        return savedPurchaseOrder;
    }

    public PurchaseOrder findById(Long id) {
        return purchaseOrderRepository.findById(id)
                .orElseThrow(() -> new InvalidException(ErrorCode.PURCHASE_ORDER_NOT_FOUND));
    }

    @Transactional
    public PurchaseOrder update(Long id, PurchaseOrderRequestDto requestDto) {
        PurchaseOrder purchaseOrder = findById(id);

        if (!"PENDING".equals(purchaseOrder.getStatus())) {
            throw new InvalidException(ErrorCode.PURCHASE_ORDER_NOT_PENDING);
        }

        Supplier supplier = supplierRepository.findById(requestDto.getSupplierId())
                .orElseThrow(() -> new InvalidException(ErrorCode.SUPPLIER_NOT_FOUND));

        purchaseOrder.setSupplier(supplier);
        purchaseOrder.setNote(requestDto.getNote());

        List<PurchaseOrderDetail> existingDetails = purchaseOrderDetailRepository.findAll().stream()
                .filter(d -> d.getPurchaseOrder().getId().equals(id))
                .toList();
        purchaseOrderDetailRepository.deleteAll(existingDetails);

        List<PurchaseOrderDetail> newDetails = requestDto.getDetails().stream().map(detailDto -> {
            ProductVariant variant = productVariantRepository.findById(detailDto.getVariantId())
                    .orElseThrow(() -> new InvalidException(ErrorCode.VARIANT_NOT_FOUND));

            PurchaseOrderDetail detail = new PurchaseOrderDetail();
            detail.setPurchaseOrder(purchaseOrder);
            detail.setVariant(variant);
            detail.setQuantity(detailDto.getQuantity());
            detail.setExpectedPrice(detailDto.getExpectedPrice());
            return detail;
        }).toList();

        purchaseOrderDetailRepository.saveAll(newDetails);

        return purchaseOrderRepository.save(purchaseOrder);
    }

    @Transactional
    public PurchaseOrder confirm(Long id) {
        PurchaseOrder purchaseOrder = findById(id);

        if (!"PENDING".equals(purchaseOrder.getStatus())) {
            throw new InvalidException(ErrorCode.PURCHASE_ORDER_NOT_PENDING);
        }

        purchaseOrder.setStatus("CONFIRMED");

        List<PurchaseOrderDetail> details = purchaseOrderDetailRepository.findAll().stream()
                .filter(d -> d.getPurchaseOrder().getId().equals(id))
                .toList();

        for (PurchaseOrderDetail detail : details) {
            ProductVariant variant = detail.getVariant();
            Inventory inventory = inventoryRepository.findByVariant(variant)
                    .orElseGet(() -> {
                        Inventory newInventory = new Inventory();
                        newInventory.setVariant(variant);
                        newInventory.setQuantityOnHand(0);
                        return newInventory;
                    });

            inventory.setQuantityOnHand(inventory.getQuantityOnHand() + detail.getQuantity());
            inventoryRepository.save(inventory);
        }

        return purchaseOrderRepository.save(purchaseOrder);
    }
}