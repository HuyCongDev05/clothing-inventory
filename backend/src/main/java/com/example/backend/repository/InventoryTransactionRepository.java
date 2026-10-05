package com.example.backend.repository;

import com.example.backend.model.InventoryTransaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface InventoryTransactionRepository extends JpaRepository<InventoryTransaction, Long>, JpaSpecificationExecutor<InventoryTransaction> {

    Page<InventoryTransaction> findByVariantIdOrderByCreatedAtDesc(Long variantId, Pageable pageable);

    // Thống kê nhập/xuất kho theo tháng; hỗ trợ cả type cũ (IN/OUT) và mới (INBOUND/OUTBOUND)
    @Query(value = """
            SELECT
                DATE_FORMAT(it.created_at, '%Y%m')    AS yearMonth,
                SUM(CASE WHEN it.transaction_type IN ('IN','INBOUND')
                         THEN it.quantity ELSE 0 END)  AS inboundQty,
                SUM(CASE WHEN it.transaction_type IN ('OUT','OUTBOUND')
                         THEN it.quantity ELSE 0 END)  AS outboundQty,
                SUM(CASE WHEN it.transaction_type IN ('IN','INBOUND')
                         THEN it.quantity * COALESCE(pod.unit_price, pv.purchase_price, 0)
                         ELSE 0 END)                   AS inboundValue,
                SUM(CASE WHEN it.transaction_type IN ('OUT','OUTBOUND')
                         THEN it.quantity * COALESCE(pv.sale_price, pv.purchase_price, 0)
                         ELSE 0 END)                   AS outboundValue
            FROM inventory_transactions it
            INNER JOIN product_variants pv ON it.variant_id = pv.id
            LEFT JOIN purchase_order_details pod ON it.purchase_order_detail_id = pod.id
            WHERE it.created_at >= :startDate
              AND it.created_at <= :endDate
            GROUP BY DATE_FORMAT(it.created_at, '%Y%m')
            ORDER BY yearMonth ASC
            """, nativeQuery = true)
    List<Object[]> findMonthlyMovements(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );
}