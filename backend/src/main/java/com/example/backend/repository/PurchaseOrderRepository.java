package com.example.backend.repository;

import com.example.backend.model.PurchaseOrder;
import com.example.backend.model.enums.PurchaseOrderStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PurchaseOrderRepository extends JpaRepository<PurchaseOrder, Long>, JpaSpecificationExecutor<PurchaseOrder> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT po FROM PurchaseOrder po WHERE po.id = :id")
    Optional<PurchaseOrder> findByIdForUpdate(@Param("id") Long id);

    // Đếm số đơn và tổng tiền theo từng trạng thái PO
    @Query("""
            SELECT po.status          AS status,
                   COUNT(po)          AS orderCount,
                   COALESCE(SUM(po.totalAmount), 0) AS totalAmount
            FROM PurchaseOrder po
            WHERE po.status IN :statuses
            GROUP BY po.status
            """)
    List<Object[]> countAndSumByStatuses(@Param("statuses") List<PurchaseOrderStatus> statuses);
}
